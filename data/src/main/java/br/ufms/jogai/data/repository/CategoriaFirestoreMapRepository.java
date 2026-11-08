package br.ufms.jogai.data.repository;

import br.ufms.jogai.data.firebase.FirestoreMapRepository;
import br.ufms.jogai.data.firebase.config.FirebaseConfig;
import br.ufms.jogai.domain.jogo.CategoriaJogo;
import br.ufms.jogai.domain.jogo.CategoriaRepository;
import br.ufms.jogai.domain.shared.Page;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.DocumentSnapshot;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class CategoriaFirestoreMapRepository extends FirestoreMapRepository<CategoriaJogo, String>
        implements CategoriaRepository {

    public CategoriaFirestoreMapRepository() {
        this(FirebaseConfig.getFirestore());
    }

    public CategoriaFirestoreMapRepository(Firestore firestore) {
        super(Objects.requireNonNull(firestore).collection("categoriasJogo"), Set.of("nome"));
    }

    @Override
    protected Map<String, Object> entityToMap(CategoriaJogo entity) {
        return Map.of(
                "nome", entity.getNome(),
                "descricao", entity.getDescricao()
        );
    }

    @Override
    protected CategoriaJogo documentToEntity(DocumentSnapshot document) {
        return CategoriaJogo.reconstitute(
                document.getId(),
                document.getString("nome"),
                document.getString("descricao")
        );
    }

    @Override
    public CompletableFuture<Optional<CategoriaJogo>> buscarPorNome(String nome) {
        return queryOne(collection.whereEqualTo("nome", nome));
    }

    @Override
    public CompletableFuture<Page<CategoriaJogo, String>> listarPorNome(String nome, int pageSize, String afterTag) {
        return queryPage(collection.whereEqualTo("nome", nome), pageSize, afterTag);
    }
}
