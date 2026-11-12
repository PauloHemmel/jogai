package br.ufms.jogai.data.repository;

import br.ufms.jogai.data.firebase.FirestoreMapRepository;
import br.ufms.jogai.data.firebase.config.FirebaseConfig;
import br.ufms.jogai.domain.jogo.CategoriaJogo;
import br.ufms.jogai.domain.jogo.CategoriaRepository;
import br.ufms.jogai.domain.shared.Page;
import br.ufms.jogai.domain.util.Validar;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Persiste categorias com nome único e descrição opcional, preservando as tags do domínio. */
public class CategoriaFirestoreMapRepository extends FirestoreMapRepository<CategoriaJogo, String>
        implements CategoriaRepository {

    /** Usa o cliente compartilhado de Firestore configurado pela aplicação. */
    public CategoriaFirestoreMapRepository() {
        this(FirebaseConfig.getFirestore());
    }

    /** Recebe o cliente da aplicação ou do emulador, sem assumir seu encerramento. */
    public CategoriaFirestoreMapRepository(Firestore firestore) {
        super(Objects.requireNonNull(firestore).collection("categoriasJogo"), Set.of("nome"));
    }

    @Override
    protected Map<String, Object> entityToMap(CategoriaJogo categoria) {
        // A ausência de descrição tem significado no domínio e deve continuar sendo nula.
        var dados = new HashMap<String, Object>();
        dados.put("nome", categoria.getNome());
        dados.put("descricao", categoria.getDescricao());
        return dados;
    }

    @Override
    protected CategoriaJogo documentToEntity(DocumentSnapshot documento) {
        return CategoriaJogo.reconstitute(
                documento.getId(),
                documento.getString("nome"),
                documento.getString("descricao")
        );
    }

    @Override
    public CompletableFuture<Optional<CategoriaJogo>> buscarPorNome(String nome) {
        return queryOne(collection.whereEqualTo("nome", Validar.nomeCategoriaJogo(nome)));
    }

    @Override
    public CompletableFuture<Page<CategoriaJogo, String>> listarPorNome(String nome, int tamanhoPagina, String aposTag) {
        return queryPage(collection.whereEqualTo("nome", Validar.nomeCategoriaJogo(nome)), tamanhoPagina, aposTag);
    }
}
