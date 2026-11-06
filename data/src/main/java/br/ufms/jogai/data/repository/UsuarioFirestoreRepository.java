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
                "email", entity.getEmail(),
                "telefone", entity.getTelefone(),
                "nascimento", toTimestamp(entity.getDataNascimento()),
                "avaliacaoComodante", Map.of(
                        "quantidade", entity.getQtdAvaliacoesComodante(),
                        "media", entity.getAvaliacaoMediaComodante()
                ),
                "avaliacaoComodatario", Map.of(
                        "quantidade", entity.getQtdAvaliacoesComodatario(),
                        "media", entity.getAvaliacaoMediaComodatario()
                ),
                "cadastradoEm", toTimestamp(entity.getDataCadastro()),
                "status", entity.getStatus().name()
        );
    }

    @Override
    protected Usuario mapToEntity(String id, Map<String, Object> dados) {
        var avaliacaoComodante = obterMapa(dados, "avaliacaoComodante");
        var avaliacaoComodatario = obterMapa(dados, "avaliacaoComodatario");

        var avaliacaoMedia = AvaliacaoMediaUsuario.reconstitute(
                obterNumero(avaliacaoComodante, "quantidade").longValue(),
                obterNumero(avaliacaoComodante, "media").doubleValue(),
                obterNumero(avaliacaoComodatario, "quantidade").longValue(),
                obterNumero(avaliacaoComodatario, "media").doubleValue()
        );

        return Usuario.reconstitute(
                idToUUID(id),
                (String) dados.get("nome"),
                (String) dados.get("email"),
                (String) dados.get("telefone"),
                toLocalDate((Timestamp) dados.get("nascimento")),
                avaliacaoMedia,
                toInstant((Timestamp) dados.get("cadastradoEm")),
                StatusUsuario.valueOf((String) dados.get("status"))
        );
    }

    private static Map<?, ?> obterMapa(Map<String, Object> dados, String campo) {
        if (dados.get(campo) instanceof Map<?, ?> mapa) {
            return mapa;
        }
        throw new IllegalStateException("Campo '%s' ausente ou inválido no documento.".formatted(campo));
    }

    private static Number obterNumero(Map<?, ?> dados, String campo) {
        if (dados.get(campo) instanceof Number numero) {
            return numero;
        }
        throw new IllegalStateException("Campo numérico '%s' ausente ou inválido.".formatted(campo));
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
    public CompletableFuture<Optional<Usuario>> buscarPorEmail(String email) {
        Objects.requireNonNull(email);

        return queryOne(collection.whereEqualTo("email", Validar.email(email)));
    }

    @Override
    public CompletableFuture<Optional<Usuario>> buscarPorTelefone(String telefone) {
        Objects.requireNonNull(telefone);

        return queryOne(collection.whereEqualTo("telefone", Validar.telefone(telefone)));
    }

    @Override
    public CompletableFuture<Page<Usuario, UUID>> listarPorStatus(StatusUsuario status, int pageSize, UUID afterId) {
        Objects.requireNonNull(status);

        return queryPage(collection.whereEqualTo("status", status.name()), pageSize, afterId);
    }
}
