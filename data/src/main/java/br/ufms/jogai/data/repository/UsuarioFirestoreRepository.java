package br.ufms.jogai.data.repository;

import br.ufms.jogai.data.firebase.FirestoreRepository;
import br.ufms.jogai.data.firebase.config.FirebaseConfig;
import br.ufms.jogai.domain.usuario.*;
import br.ufms.jogai.domain.shared.Page;
import br.ufms.jogai.domain.util.Validar;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.Timestamp;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class UsuarioFirestoreRepository extends FirestoreRepository<Usuario, UUID> implements UsuarioRepository {

    public UsuarioFirestoreRepository() {
        this(FirebaseConfig.getFirestore());
    }

    public UsuarioFirestoreRepository(Firestore firestore) {
        super(Objects.requireNonNull(firestore).collection("usuarios"), Set.of("email", "telefone"));
    }

    /**
     * Cadastra todos os usuários atomicamente, incluindo suas reservas de email e telefone.
     */
    public CompletableFuture<Collection<Usuario>> cadastrarTodos(Collection<Usuario> usuarios) {
        Objects.requireNonNull(usuarios);
        return writeTransaction(writes -> usuarios.stream().map(usuario -> writes.create(this, usuario)).toList());
    }

    @Override
    protected RuntimeException duplicateValueException(String campo) {
        return new UsuarioDuplicadoException(campo);
    }

    @Override
    protected Map<String, Object> entityToMap(Usuario entity) {
        return Map.of(
                "nome", entity.getNome(),
                "usuario", entity.getUsuario(),
                "email", entity.getEmail(),
                "telefone", entity.getTelefone(),
                "nascimento", toTimestamp(entity.getDataNascimento()),
                "avaliacao", Map.of(
                        "qtdAvaliacoesComodante", entity.getQuantidadeAvaliacoesComodante(),
                        "mediaAvaliacaoComodante", entity.getNotaAvaliacaoComodante(),
                        "qtdAvaliacoesComodatario", entity.getQuantidadeAvaliacoesComodatario(),
                        "mediaAvaliacaoComodatario", entity.getNotaAvaliacaoComodante()
                ),
                "status", entity.getStatus().name(),
                "cadastradoEm", toTimestamp(entity.getDataCadastro())

        );
    }

    @Override
    protected Usuario mapToEntity(String id, Map<String, Object> dados) {
        @SuppressWarnings("unchecked")
        Map<String, Object> avaliacaoMap = (Map<String, Object>) dados.get("avaliacao");

        AvaliacaoMediaUsuario avaliacao = AvaliacaoMediaUsuario.reconstitute(
                ((Number) avaliacaoMap.get("qtdAvaliacoesComodante")).longValue(),
                ((Number) avaliacaoMap.get("mediaAvaliacaoComodante")).doubleValue(),
                ((Number) avaliacaoMap.get("qtdAvaliacoesComodatario")).longValue(),
                ((Number) avaliacaoMap.get("mediaAvaliacaoComodatario")).doubleValue()
        );

        return Usuario.reconstitute(
                UUID.fromString(id),
                (String) dados.get("nome"),
                (String) dados.get("usuario"),
                (String) dados.get("email"),
                (String) dados.get("telefone"),
                toLocalDate((Timestamp) dados.get("nascimento")),
                avaliacao,
                StatusUsuario.valueOf((String) dados.get("status")),
                toInstant((Timestamp) dados.get("cadastradoEm"))
        );
    }

    @Override
    public CompletableFuture<Page<Usuario, UUID>> listarPorNome(String nome, int pageSize, UUID afterId) {
        Objects.requireNonNull(nome);

        return queryPage(collection
                .whereGreaterThanOrEqualTo("nome", nome)
                .whereLessThanOrEqualTo("nome", nome + "\uf8ff")
                .orderBy("nome"), pageSize, afterId);
    }

    @Override
    public CompletableFuture<Optional<Usuario>> buscarPorUsuario(String usuario) {
        Objects.requireNonNull(usuario);

        return queryOne(collection.whereEqualTo("usuario", usuario));
    }

    @Override
    public CompletableFuture<Optional<Usuario>> buscarPorEmail(String email) {
        Objects.requireNonNull(email);

        return queryOne(collection.whereEqualTo("email", email));
    }

    @Override
    public CompletableFuture<Optional<Usuario>> buscarPorTelefone(String telefone) {
        Objects.requireNonNull(telefone);

        return queryOne(collection.whereEqualTo("telefone", telefone));
    }

    @Override
    public CompletableFuture<Page<Usuario, UUID>> listarPorStatus(StatusUsuario status, int pageSize, UUID afterId) {
        Objects.requireNonNull(status);

        return queryPage(collection.whereEqualTo("status", status.name()), pageSize, afterId);
    }
}
