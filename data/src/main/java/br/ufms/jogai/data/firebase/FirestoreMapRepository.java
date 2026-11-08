package br.ufms.jogai.data.firebase;

import br.ufms.jogai.domain.shared.Entidade;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Transaction;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * Firestore repository whose persistence representation is a field map.
 */
public abstract class FirestoreMapRepository<E extends Entidade<ID>, ID extends Serializable>
        extends AbstractFirestoreRepository<E, ID, Map<String, Object>> {

    public static final int MAX_PAGE_SIZE = AbstractFirestoreRepository.MAX_PAGE_SIZE;

    protected FirestoreMapRepository(CollectionReference collection) {
        super(collection);
    }

    protected FirestoreMapRepository(CollectionReference collection, Set<String> uniqueFields) {
        super(collection, uniqueFields);
    }

    protected FirestoreMapRepository(CollectionReference collection, String scope, Set<String> uniqueFields) {
        super(collection, scope, uniqueFields);
    }

    @Override
    Map<String, Object> captureData(E entity) {
        return FirestoreDataSnapshot.copy(entityToMap(entity));
    }

    @Override
    Map<String, String> uniqueValuesFromData(Map<String, Object> data) {
        return extractUniqueValues(data);
    }

    @Override
    Map<String, String> uniqueValuesFromSnapshot(DocumentSnapshot snapshot) {
        return extractUniqueValues(snapshot.getData());
    }

    @Override
    ApiFuture<?> createDocument(DocumentReference reference, Map<String, Object> data) {
        return reference.create(data);
    }

    @Override
    ApiFuture<?> setDocument(DocumentReference reference, Map<String, Object> data) {
        return reference.set(data);
    }

    @Override
    void createInTransaction(Transaction transaction, DocumentReference reference, Object data) {
        transaction.create(reference, castData(data));
    }

    @Override
    void setInTransaction(Transaction transaction, DocumentReference reference, Object data) {
        transaction.set(reference, castData(data));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castData(Object data) {
        return (Map<String, Object>) data;
    }

    protected abstract Map<String, Object> entityToMap(E entity);
}
