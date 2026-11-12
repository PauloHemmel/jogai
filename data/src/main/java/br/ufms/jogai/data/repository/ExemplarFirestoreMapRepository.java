package br.ufms.jogai.data.repository;

import br.ufms.jogai.data.firebase.FirestoreMapRepository;
import br.ufms.jogai.data.firebase.config.FirebaseConfig;
import br.ufms.jogai.domain.jogo.CategoriaJogo;
import br.ufms.jogai.domain.jogo.EstadoConservacao;
import br.ufms.jogai.domain.jogo.Exemplar;
import br.ufms.jogai.domain.jogo.ExemplarRepository;
import br.ufms.jogai.domain.jogo.JogoInfo;
import br.ufms.jogai.domain.shared.Page;
import br.ufms.jogai.domain.usuario.UsuarioInfo;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Cada exemplar mantém os dados do jogo e do proprietário necessários à sua reconstrução. */
public class ExemplarFirestoreMapRepository extends FirestoreMapRepository<Exemplar, UUID>
        implements ExemplarRepository {

    public ExemplarFirestoreMapRepository() {
        this(FirebaseConfig.getFirestore());
    }

    public ExemplarFirestoreMapRepository(Firestore firestore) {
        // Vários exemplares podem pertencer ao mesmo jogo e ao mesmo proprietário.
        super(Objects.requireNonNull(firestore).collection("exemplares"));
    }

    @Override
    protected Map<String, Object> entityToMap(Exemplar exemplar) {
        JogoInfo jogo = exemplar.getJogoInfo();
        UsuarioInfo proprietario = exemplar.getProprietarioInfo();
        var dados = new HashMap<String, Object>();
        dados.put("jogo", Map.of(
                "id", jogo.id().toString(),
                "nome", jogo.nome(),
                "editora", jogo.editora(),
                "categorias", CategoriasFirestore.toMaps(jogo.categorias())));
        dados.put("proprietario", Map.of(
                "id", proprietario.id().toString(),
                "nome", proprietario.nome(),
                "email", proprietario.email(),
                "telefone", proprietario.telefone(),
                "dataNascimento", toTimestamp(proprietario.dataNascimento()),
                "notaAvaliacaoComodante", proprietario.notaAvaliacaoComodante(),
                "notaAvaliacaoComodatario", proprietario.notaAvaliacaoComodatario()));
        dados.put("estadoConservacao", exemplar.getEstadoConservacao().name());
        dados.put("observacao", exemplar.getObservacao());
        dados.put("cadastradoEm", toTimestamp(exemplar.getDataCadastro()));
        return dados;
    }

    @Override
    protected Exemplar documentToEntity(DocumentSnapshot documento) {
        var jogo = new JogoInfo(
                idToUUID(documento.getString("jogo.id")),
                documento.getString("jogo.nome"),
                documento.getString("jogo.editora"),
                CategoriasFirestore.fromMaps(documento.get("jogo.categorias")));
        var proprietario = new UsuarioInfo(
                idToUUID(documento.getString("proprietario.id")),
                documento.getString("proprietario.nome"),
                documento.getString("proprietario.email"),
                documento.getString("proprietario.telefone"),
                toLocalDate(documento.getTimestamp("proprietario.dataNascimento")),
                Objects.requireNonNull(documento.getDouble("proprietario.notaAvaliacaoComodante")),
                Objects.requireNonNull(documento.getDouble("proprietario.notaAvaliacaoComodatario")));
        return Exemplar.reconstitute(
                idToUUID(documento.getId()), jogo, proprietario,
                EstadoConservacao.valueOf(documento.getString("estadoConservacao")),
                documento.getString("observacao"), toInstant(documento.getTimestamp("cadastradoEm")));
    }

    @Override
    public CompletableFuture<Page<Exemplar, UUID>> listarPorJogo(UUID jogoId, int pageSize, UUID afterId) {
        return queryPage(collection.whereEqualTo("jogo.id", Objects.requireNonNull(jogoId).toString()),
                pageSize, afterId);
    }

    @Override
    public CompletableFuture<Page<Exemplar, UUID>> listarPorProprietario(UUID proprietarioId, int pageSize, UUID afterId) {
        return queryPage(collection.whereEqualTo("proprietario.id", Objects.requireNonNull(proprietarioId).toString()),
                pageSize, afterId);
    }

    @Override
    public CompletableFuture<Page<Exemplar, UUID>> listarPorEstadoConservacao(
            EstadoConservacao estado, int pageSize, UUID afterId) {
        return queryPage(collection.whereEqualTo("estadoConservacao", Objects.requireNonNull(estado).name()),
                pageSize, afterId);
    }
}
