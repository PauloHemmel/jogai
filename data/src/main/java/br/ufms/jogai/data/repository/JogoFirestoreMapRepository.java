package br.ufms.jogai.data.repository;

import br.ufms.jogai.data.firebase.FirestoreMapRepository;
import br.ufms.jogai.data.firebase.config.FirebaseConfig;
import br.ufms.jogai.domain.jogo.CategoriaJogo;
import br.ufms.jogai.domain.jogo.DuracaoPartida;
import br.ufms.jogai.domain.jogo.Jogo;
import br.ufms.jogai.domain.jogo.JogoRepository;
import br.ufms.jogai.domain.shared.Page;
import br.ufms.jogai.domain.util.Validar;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Persiste jogos com nome único e categorias incorporadas ao documento. */
public class JogoFirestoreMapRepository extends FirestoreMapRepository<Jogo, UUID> implements JogoRepository {

    public JogoFirestoreMapRepository() {
        this(FirebaseConfig.getFirestore());
    }

    public JogoFirestoreMapRepository(Firestore firestore) {
        super(Objects.requireNonNull(firestore).collection("jogos"), Set.of("nome"));
    }

    @Override
    protected Map<String, Object> entityToMap(Jogo jogo) {
        var dados = new HashMap<String, Object>();
        dados.put("nome", jogo.getNome());
        dados.put("editora", jogo.getEditora());
        dados.put("anoLancamento", jogo.getAnoLancamento() == null ? null : jogo.getAnoLancamento().getValue());
        dados.put("minimoJogadores", jogo.getMinimoJogadores());
        dados.put("maximoJogadores", jogo.getMaximoJogadores());
        dados.put("idadeMinima", jogo.getIdadeMinimaRecomendada());
        dados.put("idadeMaxima", jogo.getIdadeMaximaRecomendada());
        dados.put("duracaoMinima", jogo.getTempoMinimoEstimado() == null ? null
                : jogo.getTempoMinimoEstimado().toString());
        dados.put("duracaoMaxima", jogo.getTempoMaximoEstimado() == null ? null
                : jogo.getTempoMaximoEstimado().toString());
        dados.put("categorias", CategoriasFirestore.toMaps(jogo.getCategorias()));
        dados.put("categoriaTags", jogo.getCategorias().stream().map(CategoriaJogo::getTag).sorted().toList());
        dados.put("descricao", jogo.getDescricao());
        dados.put("ativo", jogo.isAtivo());
        return dados;
    }

    @Override
    protected Jogo documentToEntity(DocumentSnapshot documento) {
        return Jogo.reconstitute(idToUUID(documento.getId()),
                        Objects.requireNonNull(documento.getBoolean("ativo")))
                .nomeEditoraLancamento(documento.getString("nome"), documento.getString("editora"),
                        inteiro(documento.getLong("anoLancamento")))
                .numeroJogadores(Math.toIntExact(Objects.requireNonNull(documento.getLong("minimoJogadores"))),
                        inteiro(documento.getLong("maximoJogadores")))
                .faixaEtaria(inteiro(documento.getLong("idadeMinima")), inteiro(documento.getLong("idadeMaxima")))
                .duracaoPartida(new DuracaoPartida(duracao(documento.getString("duracaoMinima")),
                        duracao(documento.getString("duracaoMaxima"))))
                .categorias(CategoriasFirestore.fromMaps(documento.get("categorias")).toArray(CategoriaJogo[]::new))
                .descricao(documento.getString("descricao"))
                .build();
    }

    private static Integer inteiro(Long value) {
        return value == null ? null : Math.toIntExact(value);
    }

    private static Duration duracao(String value) {
        return value == null ? null : Duration.parse(value);
    }

    @Override
    public CompletableFuture<Optional<Jogo>> buscarPorNome(String nome) {
        return queryOne(collection.whereEqualTo("nome", Validar.nomeJogo(nome)));
    }

    @Override
    public CompletableFuture<Page<Jogo, UUID>> listarPorCategoria(CategoriaJogo categoria, int pageSize, UUID afterId) {
        Objects.requireNonNull(categoria, "A categoria é obrigatória");
        return queryPage(collection.whereArrayContains("categoriaTags", categoria.getTag()), pageSize, afterId);
    }

    @Override
    public CompletableFuture<Page<Jogo, UUID>> listarPorEditora(String editora, int pageSize, UUID afterId) {
        return queryPage(collection.whereEqualTo("editora", Validar.nomeEditora(editora)), pageSize, afterId);
    }

    @Override
    public CompletableFuture<Page<Jogo, UUID>> listarAtivos(int pageSize, UUID afterId) {
        return queryPage(collection.whereEqualTo("ativo", true), pageSize, afterId);
    }
}
