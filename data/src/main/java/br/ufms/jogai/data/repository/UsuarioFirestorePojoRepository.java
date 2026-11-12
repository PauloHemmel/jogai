package br.ufms.jogai.data.repository;

import br.ufms.jogai.data.firebase.FirestorePojoRepository;
import br.ufms.jogai.data.firebase.config.FirebaseConfig;
import br.ufms.jogai.data.model.UsuarioDocument;
import br.ufms.jogai.domain.shared.Page;
import br.ufms.jogai.domain.usuario.StatusUsuario;
import br.ufms.jogai.domain.usuario.Usuario;
import br.ufms.jogai.domain.usuario.UsuarioRepository;
import com.google.cloud.firestore.Firestore;

import java.util.Objects;
import java.util.Optional;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class UsuarioFirestorePojoRepository extends FirestorePojoRepository<Usuario, UUID, UsuarioDocument>
        implements UsuarioRepository {

    public UsuarioFirestorePojoRepository() {
        this(FirebaseConfig.getFirestore());
    }

    public UsuarioFirestorePojoRepository(Firestore firestore) {
        super(Objects.requireNonNull(firestore).collection("usuarios"),
                UsuarioDocument.class, UsuarioDocument::new);
    }

    /**
     * Cadastra todos os usuários atomicamente, incluindo reservas de email e telefone.
     */
    public CompletableFuture<Collection<Usuario>> cadastrarTodos(Collection<Usuario> usuarios) {
        Objects.requireNonNull(usuarios, "Users must not be null");
        return writeTransaction(writes -> usuarios.stream()
                .map(usuario -> writes.create(this, usuario))
                .toList());
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
