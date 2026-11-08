package br.ufms.jogai.data.firebase;

import br.ufms.jogai.data.model.shared.EntityDocument;
import br.ufms.jogai.data.model.shared.EntityDocumentMapper;
import br.ufms.jogai.data.model.shared.Unique;
import br.ufms.jogai.domain.shared.Entidade;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Transaction;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Firestore repository whose persistence representation is a typed document record.
 */
public abstract class FirestorePojoRepository<
        E extends Entidade<ID>,
        ID extends Serializable,
        D extends EntityDocument<E>
        > extends AbstractFirestoreRepository<E, ID, D> {

    public static final int MAX_PAGE_SIZE = AbstractFirestoreRepository.MAX_PAGE_SIZE;

    private final Class<D> documentType;
    private final EntityDocumentMapper<E, D> documentMapper;

    protected FirestorePojoRepository(CollectionReference collection, Class<D> documentType,
                                      EntityDocumentMapper<E, D> documentMapper) {
        this(collection, documentType, uniqueFields(documentType), documentMapper);
    }

    protected FirestorePojoRepository(CollectionReference collection, Class<D> documentType,
                                      Set<String> uniqueFields, EntityDocumentMapper<E, D> documentMapper) {
        this(collection, Objects.requireNonNull(collection, "Collection must not be null").getPath(),
                documentType, uniqueFields, documentMapper);
    }

    protected FirestorePojoRepository(CollectionReference collection, String scope, Class<D> documentType,
                                      Set<String> uniqueFields, EntityDocumentMapper<E, D> documentMapper) {
        super(collection, scope, union(uniqueFields(documentType), uniqueFields));
        this.documentType = Objects.requireNonNull(documentType, "Document type must not be null");
        if (!documentType.isRecord()) {
            throw new IllegalArgumentException("Firestore V2 document type must be a record");
        }
        this.documentMapper = Objects.requireNonNull(documentMapper, "Document mapper must not be null");
    }

    private static Set<String> uniqueFields(Class<?> documentType) {
        Objects.requireNonNull(documentType, "Document type must not be null");
        Set<String> fields = new HashSet<>();
        if (documentType.isRecord()) {
            for (RecordComponent component : documentType.getRecordComponents()) {
                if (component.isAnnotationPresent(Unique.class)) fields.add(component.getName());
            }
        }
        for (Class<?> type = documentType; type != null && type != Object.class; type = type.getSuperclass()) {
            for (var field : type.getDeclaredFields()) {
                if (!java.lang.reflect.Modifier.isStatic(field.getModifiers()) && field.isAnnotationPresent(Unique.class)) {
                    fields.add(field.getName());
                }
            }
            for (var method : type.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Unique.class) && method.getParameterCount() == 0) {
                    String name = method.getName();
                    if (name.startsWith("get") && name.length() > 3)
                        name = Character.toLowerCase(name.charAt(3)) + name.substring(4);
                    fields.add(name);
                }
            }
        }
        return Set.copyOf(fields);
    }

    private static Set<String> union(Set<String> annotated, Set<String> configured) {
        Set<String> result = new HashSet<>(annotated);
        result.addAll(Objects.requireNonNull(configured, "Unique fields must not be null"));
        return Set.copyOf(result);
    }

    @Override
    D captureData(E entity) {
        D document = Objects.requireNonNull(documentMapper.toDocument(entity), "Document mapper must not return null");
        return documentType.cast(FirestoreDataSnapshot.copyValue(document));
    }

    @Override
    Map<String, String> uniqueValuesFromData(D document) {
        if (uniqueFields().isEmpty()) return Map.of();
        Map<String, String> values = HashMap.newHashMap(uniqueFields().size());
        Map<String, RecordComponent> components = new HashMap<>();
        for (RecordComponent component : documentType.getRecordComponents()) {
            components.put(component.getName(), component);
        }
        for (String field : uniqueFields()) {
            RecordComponent component = components.get(field);
            if (component == null) {
                throw new IllegalArgumentException("Unique field " + field + " is not a document component");
            }
            try {
                var accessor = component.getAccessor();
                if (!accessor.canAccess(document) && !accessor.trySetAccessible()) {
                    throw new IllegalAccessException("Record component accessor is not accessible");
                }
                Object value = accessor.invoke(document);
                if (!(value instanceof String string)) {
                    throw new IllegalArgumentException("Unique field " + field + " must be a required String");
                }
                values.put(field, string);
            } catch (IllegalAccessException | InvocationTargetException error) {
                throw new IllegalStateException("Could not read document field " + field, error);
            }
        }
        return values;
    }

    @Override
    Map<String, String> uniqueValuesFromSnapshot(DocumentSnapshot snapshot) {
        if (uniqueFields().isEmpty()) return Map.of();
        D document = Objects.requireNonNull(snapshot.toObject(documentType), "Firestore returned an empty document");
        return uniqueValuesFromData(document);
    }

    @Override
    ApiFuture<?> createDocument(DocumentReference reference, D document) {
        return reference.create(document);
    }

    @Override
    ApiFuture<?> setDocument(DocumentReference reference, D document) {
        return reference.set(document);
    }

    @Override
    void createInTransaction(Transaction transaction, DocumentReference reference, Object document) {
        transaction.create(reference, documentType.cast(document));
    }

    @Override
    void setInTransaction(Transaction transaction, DocumentReference reference, Object document) {
        transaction.set(reference, documentType.cast(document));
    }

    @Override
    protected final E documentToEntity(DocumentSnapshot document) {
        D dto = Objects.requireNonNull(document.toObject(documentType), "Firestore returned an empty document");
        return Objects.requireNonNull(dto.toEntity(), "Document mapper must not return null");
    }
}
