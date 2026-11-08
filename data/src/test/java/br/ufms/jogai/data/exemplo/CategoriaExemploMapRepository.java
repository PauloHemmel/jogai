package br.ufms.jogai.data.exemplo;

import br.ufms.jogai.data.firebase.FirestoreMapRepository;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentSnapshot;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Exemplo compilado e exercitado pelos testes de integração.
 */
public final class CategoriaExemploMapRepository extends FirestoreMapRepository<CategoriaExemplo, UUID> {

    public CategoriaExemploMapRepository(Firestore firestore) {
        super(firestore.collection("categorias_exemplo"), Set.of("codigo"));
    }

    /**
     * Configura uma coleção ou subcoleção, com código único dentro do caminho completo.
     */
    public CategoriaExemploMapRepository(CollectionReference collection) {
        super(collection, Set.of("codigo"));
    }

    @Override
    protected Map<String, Object> entityToMap(CategoriaExemplo categoria) {
        return Map.of(
                "nome", categoria.getNome(),
                "codigo", categoria.getCodigo()
        );
    }

    @Override
    protected CategoriaExemplo documentToEntity(DocumentSnapshot document) {
        return new CategoriaExemplo(
                idToUUID(document.getId()),
                document.getString("nome"),
                document.getString("codigo")
        );
    }
}
