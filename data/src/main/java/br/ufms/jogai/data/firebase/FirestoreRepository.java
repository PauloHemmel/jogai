package br.ufms.jogai.data.firebase;

import br.ufms.jogai.domain.shared.Entidade;
import br.ufms.jogai.domain.shared.Repository;
import br.ufms.jogai.domain.shared.Page;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.api.gax.rpc.AlreadyExistsException;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FieldPath;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.concurrent.ForkJoinPool;

/**
 * Generic Firestore persistence with optional uniqueness constraints on required String fields.
 *
 * <p>Subclasses declare a collection, optionally declare a unique scope and field names, and
 * implement {@link #entityToMap(Entidade)} and {@link #documentToEntity(DocumentSnapshot)}. The domain owns
 * validation and normalization; mappings must preserve canonical values and entity IDs.
 * Document mappings reconstruct entities from snapshots. Writes return the same entity instance
 * supplied to the repository. Entity data is captured synchronously before writes begin, so later
 * mutations do not change the payload submitted to Firestore.
 *
 * <p>Without unique fields, writes use Firestore's single-document operations. With unique fields,
 * the entity and its reservations are written in one transaction. A value already owned by the
 * same entity is allowed; another owner's value rejects the entire write. Updates release changed
 * previous values, and deletion releases the deleted entity's reservations.
 *
 * <p>Collections start empty and all writes must use the same reservation protocol and canonical
 * field mappings. Writes perform no initialization, collection scan, or legacy-data migration.
 * Previously stored unique values are read directly from the persisted fields on update/delete.
 * All writers must follow this reservation protocol; direct privileged writes can bypass it.
 *
 * <p>Database failures complete returned futures exceptionally. Null arguments, mapping failures,
 * and local SDK validation may throw synchronously. This repository does not own or close the
 * supplied {@link Firestore} client. Existing storage names are preserved for compatibility.
 *
 * @param <E>  the domain entity type
 * @param <ID> the entity identifier type; its String representation must be a single document ID,
 *             never a relative path containing slashes
 */
public abstract class FirestoreRepository<E extends Entidade<ID>, ID extends Serializable>
        implements Repository<E, ID> {

    /**
     * Maximum number of entities returned by a page.
     */
    public static final int MAX_PAGE_SIZE = 200;

    /**
     * Shared Firestore client owned by the application.
     */
    protected final Firestore db;

    /**
     * Collection containing the entity documents.
     */
    protected final CollectionReference collection;

    private final String scope;
    private final Set<String> uniqueFields;

    /**
     * Creates a repository without unique-field constraints.
     *
     * @param collection the non-null entity collection
     */
    protected FirestoreRepository(CollectionReference collection) {
        this(collection, Set.of());
    }

    /**
     * Creates a repository whose unique values are scoped to the full collection path.
     * This also isolates identically named subcollections under different parent documents.
     *
     * @param collection   the root collection or subcollection managed by this repository
     * @param uniqueFields required canonical String fields, unique within this collection
     */
    protected FirestoreRepository(CollectionReference collection, Set<String> uniqueFields) {
        this(collection, Objects.requireNonNull(collection, "Collection must not be null").getPath(), uniqueFields);
    }

    /**
     * Creates a repository with optional unique-field constraints.
     *
     * <p>Each field is unique independently, rather than as a composite key. The scope must be
     * stable and exclusive to this entity collection. An empty field set disables reservations.
     * For example: {@code super(db.collection("accounts"), "account", Set.of("email"))}.
     *
     * @param collection   the non-null entity collection
     * @param scope        the non-blank uniqueness namespace
     * @param uniqueFields required String fields, using the names produced by {@code entityToMap}
     * @throws IllegalArgumentException if the scope or a reservation key component is blank
     * @throws NullPointerException     if an argument or a field name is null
     */
    protected FirestoreRepository(CollectionReference collection, String scope, Set<String> uniqueFields) {
        this.collection = Objects.requireNonNull(collection, "Collection must not be null");
        this.db = collection.getFirestore();
        this.scope = Objects.requireNonNull(scope, "Scope must not be null");
        if (scope.isBlank()) {
            throw new IllegalArgumentException("Scope must not be blank");
        }
        this.uniqueFields = Set.copyOf(Objects.requireNonNull(uniqueFields, "Unique fields must not be null"));
        for (String field : this.uniqueFields) {
            if (field.isBlank()) throw new IllegalArgumentException("Unique field names must not be blank");
        }
    }

    /**
     * Creates a new entity, rejecting an existing ID or another entity's unique values.
     *
     * @param entity the validated entity to create
     * @return a future containing the same entity instance supplied to this method
     * @see #duplicateValueException(String)
     */
    public final CompletableFuture<E> create(E entity) {
        return persist(entity, true);
    }

    /**
     * Creates or completely replaces the document identified by the entity's ID.
     *
     * <p>Unique values owned by the same entity remain valid. On conflict, neither the document
     * nor its reservations changes. Fields absent from the mapping are removed from the document.
     *
     * @param entity the validated entity to save
     * @return a future containing the same entity instance supplied to this method
     */
    @Override
    public final CompletableFuture<E> save(E entity) {
        return persist(entity, false);
    }

    private CompletableFuture<E> persist(E entity, boolean createOnly) {
        var captured = capture(entity);
        if (uniqueFields.isEmpty()) {
            var write = createOnly ? captured.reference().create(captured.data()) : captured.reference().set(captured.data());
            var persisted = toCompletableFuture(write);
            if (createOnly) {
                persisted = persisted.exceptionallyCompose(error -> CompletableFuture.failedFuture(
                        error instanceof AlreadyExistsException ? duplicateValueException("id") : error));
            }
            return persisted.thenApply(result -> captured.entity());
        }
        return writeTransaction(writes -> writes.addCaptured(this, captured, createOnly));
    }

    /**
     * Plans and commits multiple repository writes in one Firestore transaction.
     *
     * <p>The planner runs once, synchronously, to capture data; database retries do not rerun it.
     * Use its create/save/delete methods instead of invoking asynchronous repository writes inside
     * the planner. All participating repositories must share this Firestore client. Each document
     * may be changed only once per plan. No database reads or writes happen during planning.
     *
     * <p>Execution reads required documents, prepares shared unique reservations, then applies
     * every document and reservation together. Changed values may be transferred or swapped between
     * participating entities if the final state is unique. Any conflict or commit failure rejects
     * the whole plan. Empty plans complete without database access. Collections and subcollections
     * can participate together; construct a repository for each full collection path.
     * Deleting a parent does not implicitly delete its descendants: plan each child deletion.
     *
     * @param planner callback that prepares writes and returns the result to publish after commit
     * @param <R>     result type
     * @return a future completed only after all writes commit, or exceptionally on failure
     */
    protected final <R> CompletableFuture<R> writeTransaction(Function<RepositoryWriteTransaction, R> planner) {
        Objects.requireNonNull(planner, "Transaction planner must not be null");
        var writes = new RepositoryWriteTransaction(db);
        R result;
        try {
            result = planner.apply(writes);
            writes.seal();
        } catch (RuntimeException | Error failure) {
            writes.discard();
            throw failure;
        }
        if (writes.isEmpty()) return CompletableFuture.completedFuture(result);
        return toCompletableFuture(db.runTransaction(transaction -> {
            writes.execute(transaction);
            return result;
        }));
    }

    record CapturedEntity<E>(DocumentReference reference, Map<String, Object> data,
                             Map<String, String> uniqueValues, E entity) {
    }

    final CapturedEntity<E> capture(E entity) {
        Objects.requireNonNull(entity, "Entity must not be null");
        var reference = documentReference(entity.getId());
        var data = captureEntityData(entity);
        return new CapturedEntity<>(reference, data, extractUniqueValues(data), entity);
    }

    final String uniqueScope() {
        return scope;
    }

    final Set<String> uniqueFields() {
        return uniqueFields;
    }

    /**
     * Deletes a document and releases its owned unique values, atomically when constraints exist.
     *
     * <p>Deleting a missing document succeeds without effect. Previous unique values are read
     * directly from the persisted canonical fields, without reconstructing the entity.
     *
     * @param id the non-null document identifier
     * @return a future completed when deletion succeeds
     */
    @Override
    public final CompletableFuture<Void> delete(ID id) {
        if (uniqueFields.isEmpty()) {
            return toCompletableFuture(documentReference(id).delete()).thenApply(result -> null);
        }
        return writeTransaction(writes -> {
            writes.delete(this, id);
            return null;
        });
    }

    private Map<String, Object> captureEntityData(E entity) {
        Map<String, Object> data = Objects.requireNonNull(entityToMap(entity), "Entity data must not be null");
        return copyFirestoreMap(data, new IdentityHashMap<>());
    }

    private static Object copyFirestoreValue(Object value, IdentityHashMap<Object, Boolean> active) {
        switch (value) {
            case null -> {
                return null;
            }
            case Map<?, ?> map -> {
                return copyFirestoreMap(map, active);
            }
            case List<?> list -> {
                enterContainer(value, active);
                try {
                    List<Object> copy = new ArrayList<>(list.size());
                    for (Object nested : list) copy.add(copyFirestoreValue(nested, active));
                    return Collections.unmodifiableList(copy);
                } finally {
                    active.remove(value);
                }
            }
            case byte[] bytes -> {
                return bytes.clone();
            }
            case Date date -> {
                return new Date(date.getTime());
            }
            default -> {
            }
        }
        return value;
    }

    private static Map<String, Object> copyFirestoreMap(Map<?, ?> source,
                                                         IdentityHashMap<Object, Boolean> active) {
        enterContainer(source, active);
        try {
            Map<String, Object> copy = new HashMap<>(source.size());
            source.forEach((key, value) -> {
                if (!(key instanceof String field)) {
                    throw new IllegalArgumentException("Firestore map keys must be strings");
                }
                copy.put(field, copyFirestoreValue(value, active));
            });
            return Collections.unmodifiableMap(copy);
        } finally {
            active.remove(source);
        }
    }

    private static void enterContainer(Object value, IdentityHashMap<Object, Boolean> active) {
        if (active.put(value, Boolean.TRUE) != null) {
            throw new IllegalArgumentException("Entity data must not contain cyclic maps or lists");
        }
    }

    final Map<String, String> extractUniqueValues(Map<String, Object> data) {
        if (uniqueFields.isEmpty()) return Map.of();
        Map<String, String> values = HashMap.newHashMap(uniqueFields.size());
        uniqueFields.forEach(field -> {
            if (!(data.get(field) instanceof String value)) {
                throw new IllegalArgumentException("Unique field " + field + " must be a required String");
            }
            values.put(field, value);
        });
        // This map stays private to the persistence flow; no second copy is needed.
        return values;
    }

    /**
     * Loads an entity from a document reference.
     *
     * @param reference the non-null document reference
     * @return the reconstructed entity, or an empty Optional if the document is missing
     */
    protected final CompletableFuture<Optional<E>> get(DocumentReference reference) {
        Objects.requireNonNull(reference, "Document reference must not be null");
        return toCompletableFuture(reference.get())
                .thenApply(document -> document.exists() ?
                        Optional.of(mapDocumentToEntity(document)) :
                        Optional.empty());
    }

    /**
     * Loads an entity by ID.
     *
     * @param id the non-null document identifier
     * @return the entity, or an empty Optional if no document exists
     */
    @Override
    public CompletableFuture<Optional<E>> get(ID id) {
        return get(documentReference(id));
    }

    /**
     * Loads every document directly contained in this repository's collection.
     *
     * <p>This method has no page-size limit and materializes the entire result in memory.
     * Read volume and memory usage grow with collection size. Subcollections are not traversed.
     * Use {@link #getPage(int)} when a bounded result is preferable.
     *
     * @return all reconstructed entities, or an empty collection if no documents exist
     */
    @Override
    public final CompletableFuture<Collection<E>> getAll() {
        return toCompletableFuture(collection.get()).thenApply(this::documentsToEntities);
    }

    /**
     * Loads a bounded page in document-ID order. The cursor remains usable if its document is deleted.
     *
     * @param pageSize maximum entities returned, from 1 to MAX_PAGE_SIZE
     * @param afterId  exclusive document-ID cursor, or null for the first page
     * @return entities and an optional cursor, based on one extra look-ahead document
     */
    @Override
    public final CompletableFuture<Page<E, ID>> getPage(int pageSize, ID afterId) {
        validateLimit(pageSize);
        Query query = collection.orderBy(FieldPath.documentId());
        if (afterId != null) query = query.startAfter(documentReference(afterId).getId());
        return readPage(query, pageSize);
    }

    /**
     * Executes one bounded page of a filtered or ordered query.
     *
     * @param query    query filters and ordering; do not supply a conflicting start cursor
     * @param pageSize maximum returned entities, from 1 to MAX_PAGE_SIZE
     * @param afterId  ID of the last returned entity from the same query, or null
     * @return the bounded page; a missing cursor document fails instead of restarting pagination
     * <p>Ordered queries use a snapshot cursor, requiring one extra document read after the first
     * page. Pagination is not a frozen database snapshot; concurrent edits can change later pages.
     */
    protected final CompletableFuture<Page<E, ID>> queryPage(Query query, int pageSize, ID afterId) {
        Objects.requireNonNull(query, "Query must not be null");
        validateLimit(pageSize);
        if (afterId == null) return readPage(query, pageSize);
        return toCompletableFuture(documentReference(afterId).get()).thenCompose(cursor -> {
            if (!cursor.exists()) throw new IllegalArgumentException("Page cursor document no longer exists");
            return readPage(query.startAfter(cursor), pageSize);
        });
    }

    private CompletableFuture<Page<E, ID>> readPage(Query query, int pageSize) {
        return toCompletableFuture(query.limit(pageSize + 1).get()).thenApply(snapshot -> {
            var documents = snapshot.getDocuments();
            int count = Math.min(pageSize, documents.size());
            var items = new ArrayList<E>(count);
            for (int i = 0; i < count; i++) items.add(mapDocumentToEntity(documents.get(i)));
            Optional<ID> next = documents.size() > pageSize
                    ? Optional.of(items.getLast().getId()) : Optional.empty();
            return new Page<>(items, next);
        });
    }

    private static void validateLimit(int limit) {
        if (limit < 1 || limit > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Limit must be between 1 and " + MAX_PAGE_SIZE);
        }
    }

    /**
     * Returns at most one query result. This method does not establish uniqueness.
     *
     * @param query the non-null query; supply ordering when the first result must be predictable
     * @return the first matching entity, or an empty Optional
     */
    protected final CompletableFuture<Optional<E>> queryOne(Query query) {
        Objects.requireNonNull(query, "Query must not be null");
        return toCompletableFuture(query.limit(1).get())
                .thenApply(snapshot -> {
                    var documents = snapshot.getDocuments();
                    return documents.isEmpty() ? Optional.empty() : Optional.of(mapDocumentToEntity(documents.getFirst()));
                });
    }

    /**
     * Converts a query snapshot using the subclass's entity mapping.
     *
     * @param snapshot the query result
     * @return the reconstructed entities in query order
     */
    private Collection<E> documentsToEntities(QuerySnapshot snapshot) {
        return snapshot.getDocuments().stream().map(this::mapDocumentToEntity).toList();
    }

    private E mapDocumentToEntity(DocumentSnapshot document) {
        E entity = Objects.requireNonNull(documentToEntity(document), "Document mapper must not return null");
        if (!document.getId().equals(idToStr(entity.getId()))) {
            throw new IllegalArgumentException("Document mapper must preserve the document ID");
        }
        return entity;
    }

    /**
     * Parses a UUID document ID.
     *
     * @param id the UUID string
     * @return the parsed UUID
     * @throws IllegalArgumentException if the string is not a valid UUID
     */
    protected static UUID idToUUID(String id) {
        return UUID.fromString(id);
    }

    /**
     * Converts an ID into a document name; subclasses may override for custom identifier types.
     *
     * @param id the non-null entity identifier
     * @return the document ID, which must be stable and unique for the identifier
     */
    protected String idToStr(ID id) {
        return String.valueOf(Objects.requireNonNull(id, "ID must not be null"));
    }

    // IDs identify direct children only. Relative paths would escape the configured collection
    // and make distinct documents with the same leaf ID appear to own the same reservation.
    final DocumentReference documentReference(ID id) {
        String value = Objects.requireNonNull(idToStr(id), "Mapped ID must not be null");
        if (value.isEmpty() || value.indexOf('/') >= 0) {
            throw new IllegalArgumentException("ID must be a single document name, not a relative path");
        }
        return collection.document(value);
    }

    /**
     * Converts an Instant without losing nanosecond precision.
     *
     * @param instant the non-null instant
     * @return the equivalent Firestore timestamp before any server-side precision conversion
     */
    protected static Timestamp toTimestamp(Instant instant) {
        Objects.requireNonNull(instant, "Instant must not be null");
        return Timestamp.ofTimeSecondsAndNanos(instant.getEpochSecond(), instant.getNano());
    }

    /**
     * Stores a date as midnight UTC, independent of the machine's default time zone.
     *
     * @param date the date to convert
     * @return a timestamp representing the start of that date in UTC
     */
    protected static Timestamp toTimestamp(LocalDate date) {
        return toTimestamp(date.atStartOfDay(ZoneOffset.UTC).toInstant());
    }

    /**
     * Converts a timestamp to an Instant without losing its nanosecond component.
     *
     * @param timestamp the non-null Firestore timestamp
     * @return the equivalent instant
     */
    protected static Instant toInstant(Timestamp timestamp) {
        Objects.requireNonNull(timestamp, "Timestamp must not be null");
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }

    /**
     * Extracts the calendar date in UTC.
     *
     * @param timestamp the timestamp to convert
     * @return its UTC date
     */
    protected static LocalDate toLocalDate(Timestamp timestamp) {
        return toInstant(timestamp).atZone(ZoneOffset.UTC).toLocalDate();
    }

    /**
     * Adapts an ApiFuture, preserving its result or failure using the common ForkJoinPool.
     * Cancellation of the returned future is not propagated to the source future.
     *
     * @param apiFuture the source asynchronous operation
     * @param <T>       the result type
     * @return the adapted future
     */
    protected <T> CompletableFuture<T> toCompletableFuture(ApiFuture<T> apiFuture) {
        CompletableFuture<T> future = new CompletableFuture<>();
        ApiFutures.addCallback(apiFuture, new ApiFutureCallback<>() {
            @Override
            public void onSuccess(T result) {
                future.complete(result);
            }

            @Override
            public void onFailure(Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        }, ForkJoinPool.commonPool());
        return future;
    }

    /**
     * Supplies the exception used for duplicate IDs and unique-field conflicts.
     *
     * @param field the conflicting persisted field name, or {@code "id"} for an existing ID
     * @return the exception to propagate; defaults to DuplicateUniqueValueException
     * <p><strong>Implementation contract:</strong> Override to translate infrastructure conflicts into an application's domain exception.
     */
    protected RuntimeException duplicateValueException(String field) {
        return new DuplicateUniqueValueException(field);
    }

    /**
     * Captures a persistable representation of a valid domain entity.
     *
     * @param entity the source entity
     * @return a non-null map containing the canonical unique values; non-unique fields may be null
     * <p><strong>Implementation contract:</strong> Do not normalize values here. Preserve domain values and copy mutable nested
     * structures so subsequent changes to the original entity cannot alter the captured write.
     */
    protected abstract Map<String, Object> entityToMap(E entity);

    /**
     * Reconstructs an existing document through the domain mapper.
     *
     * @param document an existing document snapshot
     * @return the domain entity represented by the snapshot
     */
    protected abstract E documentToEntity(DocumentSnapshot document);
}
