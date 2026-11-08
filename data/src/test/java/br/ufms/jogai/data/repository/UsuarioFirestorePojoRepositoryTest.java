package br.ufms.jogai.data.repository;

import br.ufms.jogai.data.firebase.DuplicateUniqueValueException;
import br.ufms.jogai.data.model.UsuarioDocument;
import br.ufms.jogai.data.firebase.FirestorePojoRepository;
import br.ufms.jogai.data.model.shared.EntityDocument;
import br.ufms.jogai.data.model.shared.Unique;
import br.ufms.jogai.domain.shared.Entidade;
import br.ufms.jogai.domain.usuario.Usuario;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.annotation.DocumentId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "FIRESTORE_EMULATOR_HOST", matches = ".+")
class UsuarioFirestorePojoRepositoryTest {

    private Firestore db;
    private UsuarioFirestorePojoRepository repository;

    @BeforeEach
    void setUp() {
        db = FirestoreOptions.newBuilder()
                .setProjectId("demo-jogai-v2-" + UUID.randomUUID().toString().substring(0, 8))
                .setEmulatorHost(System.getenv("FIRESTORE_EMULATOR_HOST"))
                .build().getService();
        repository = new UsuarioFirestorePojoRepository(db);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (db != null) db.close();
    }

    @Test
    void serializesRecordAndReadsItBackThroughFirestoreMapper() throws Exception {
        Usuario usuario = usuario("v2@example.com", "67996122801");
        Usuario created = await(repository.create(usuario));

        var snapshot = db.collection("usuarios").document(usuario.getId().toString()).get().get();
        assertEquals(usuario.getNome(), snapshot.getString("nome"));
        assertEquals(usuario.getEmail(), snapshot.getString("email"));
        assertFalse(snapshot.getData().containsKey("id"));

        UsuarioDocument document = snapshot.toObject(UsuarioDocument.class);
        assertNotNull(document);
        assertEquals(usuario.getId().toString(), document.id());
        assertEquals(usuario.getId(), created.getId());
        assertEquals(usuario.getDataNascimento(), document.toEntity().getDataNascimento());
        assertEquals(usuario.getDataCadastro(), document.toEntity().getDataCadastro());

        Usuario loaded = await(repository.get(usuario.getId())).orElseThrow();
        assertEquals(usuario.getEmail(), loaded.getEmail());
    }

    @Test
    void preservesUniqueEmailConstraint() throws Exception {
        await(repository.create(usuario("same@example.com", "67996122802")));
        DuplicateUniqueValueException error = assertInstanceOf(
                DuplicateUniqueValueException.class,
                usuarioException(assertThrows(Exception.class,
                        () -> await(repository.create(usuario("same@example.com", "67996122803")))))
        );
        assertEquals("email", error.getField());
    }

    @Test
    void searchMethodsQueryExactlyTheCanonicalValuesSuppliedByTheCallingLayer() throws Exception {
        Usuario user = usuario("canonical@example.com", "67996122812");
        await(repository.create(user));

        assertEquals(user.getId(), await(repository.buscarPorEmail("canonical@example.com")).orElseThrow().getId());
        assertEquals(user.getId(), await(repository.buscarPorTelefone("67996122812")).orElseThrow().getId());
        assertEquals(user.getId(), await(repository.buscarPorUsuario(user.getUsuario())).orElseThrow().getId());
        assertTrue(await(repository.buscarPorEmail(" CANONICAL@example.com ")).isEmpty());
        assertTrue(await(repository.buscarPorTelefone("(67) 99612-2812")).isEmpty());
        assertTrue(await(repository.buscarPorUsuario(" " + user.getUsuario() + " ")).isEmpty());
    }

    @Test
    void sharesStringReservationsWithTheExistingRepository() throws Exception {
        UsuarioFirestoreMapRepository existingRepository = new UsuarioFirestoreMapRepository(db);
        await(existingRepository.create(usuario("shared@example.com", "67996122804")));

        DuplicateUniqueValueException error = assertInstanceOf(
                DuplicateUniqueValueException.class,
                usuarioException(assertThrows(Exception.class,
                        () -> await(repository.create(usuario("shared@example.com", "67996122805")))))
        );
        assertEquals("email", error.getField());
    }

    @Test
    void writesReturnInputAndPersistTheSynchronousSnapshot() throws Exception {
        Usuario usuario = usuario("captured@example.com", "67996122806");
        var write = repository.create(usuario);
        usuario.setEmail("changed@example.com");
        usuario.setTelefone("67996122807");

        assertSame(usuario, await(write));
        Usuario persisted = await(repository.get(usuario.getId())).orElseThrow();
        assertEquals("captured@example.com", persisted.getEmail());
        assertEquals("67996122806", persisted.getTelefone());
    }

    @Test
    void idCursorPagesAreBoundedAndReturnEveryDocumentOnce() throws Exception {
        for (int i = 0; i < 7; i++) {
            await(repository.create(usuario("page" + i + "@example.com", "6799612290" + i)));
        }

        var ids = new java.util.HashSet<UUID>();
        UUID cursor = null;
        int pages = 0;
        do {
            var page = await(repository.getPage(2, cursor));
            assertTrue(page.items().size() <= 2);
            for (Usuario item : page.items()) assertTrue(ids.add(item.getId()));
            cursor = page.nextCursor().orElse(null);
            pages++;
        } while (cursor != null);

        assertEquals(7, ids.size());
        assertEquals(4, pages);
    }

    @Test
    void duplicateInBatchRollsBackAllDocumentsAndReservations() throws Exception {
        Usuario first = usuario("duplicate@example.com", "67996122808");
        Usuario second = usuario("duplicate@example.com", "67996122809");

        assertThrows(Exception.class, () -> await(repository.cadastrarTodos(java.util.List.of(first, second))));
        assertTrue(await(repository.getAll()).isEmpty());
        assertEquals(0, db.collection("valores_unicos_controle").get().get().size());
    }

    @Test
    void invalidPageSizesThrowSynchronously() {
        assertThrows(IllegalArgumentException.class, () -> repository.getPage(0));
        assertThrows(IllegalArgumentException.class, () -> repository.getPage(201));
    }

    @Test
    void pojoSnapshotProtectsMutableNestedValuesForDirectAndTransactionalWrites() throws Exception {
        var mutableRepository = new MutablePojoRepository(db.collection("mutable_v2"));
        MutableEntity direct = new MutableEntity("direct", List.of("captured"), "direct-key");
        var directWrite = mutableRepository.create(direct);
        direct.tags.add("mutated-after-call");
        assertSame(direct, await(directWrite));
        assertEquals(List.of("captured"), db.collection("mutable_v2").document("direct").get().get().get("tags"));

        MutableEntity transactional = new MutableEntity("transactional", List.of("captured"), "transaction-key");
        var transactionWrite = mutableRepository.createTransactionally(transactional);
        transactional.tags.add("mutated-after-plan");
        assertSame(transactional, await(transactionWrite));
        assertEquals(List.of("captured"), db.collection("mutable_v2").document("transactional").get().get().get("tags"));
    }

    private static Usuario usuario(String email, String telefone) {
        return Usuario.create("Pessoa V", "pessoa_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12),
                email, telefone, LocalDate.of(1990, 1, 1));
    }

    private static <T> T await(CompletableFuture<T> future) throws Exception {
        return future.get(45, TimeUnit.SECONDS);
    }

    private static Throwable usuarioException(Throwable error) {
        while (error.getCause() != null && !(error instanceof DuplicateUniqueValueException)) {
            error = error.getCause();
        }
        return error;
    }

    private static final class MutableEntity extends Entidade<String> {
        private final List<String> tags;
        private final String key;

        private MutableEntity(String id, List<String> tags, String key) {
            super(id);
            this.tags = new ArrayList<>(tags);
            this.key = key;
        }
    }

    public record MutableDocument(@DocumentId String id, List<String> tags, @Unique String key)
            implements EntityDocument<MutableEntity> {
        private MutableDocument(MutableEntity entity) {
            this(entity.getId(), entity.tags, entity.key);
        }

        @Override
        public MutableEntity toEntity() {
            return new MutableEntity(id, tags, key);
        }
    }

    private static final class MutablePojoRepository
            extends FirestorePojoRepository<MutableEntity, String, MutableDocument> {
        private MutablePojoRepository(CollectionReference collection) {
            super(collection, MutableDocument.class, MutableDocument::new);
        }

        private CompletableFuture<MutableEntity> createTransactionally(MutableEntity entity) {
            return writeTransaction(writes -> writes.create(this, entity));
        }
    }
}
