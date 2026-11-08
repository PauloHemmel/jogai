package br.ufms.jogai.data.repository;

import br.ufms.jogai.data.DefaultCategoriasJogo;
import br.ufms.jogai.domain.jogo.CategoriaJogo;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Executa contra o emulador Firestore, com projeto isolado por teste. */
@EnabledIfEnvironmentVariable(named = "FIRESTORE_EMULATOR_HOST", matches = ".+")
class CategoriaFirestoreRepositoryTest {

    private Firestore db;
    private CategoriaFirestoreRepository repository;

    @BeforeEach
    void configurar() {
        db = FirestoreOptions.newBuilder()
                .setProjectId("demo-jogai-" + UUID.randomUUID().toString().substring(0, 8))
                .setEmulatorHost(System.getenv("FIRESTORE_EMULATOR_HOST"))
                .build().getService();
        repository = new CategoriaFirestoreRepository(db);
    }

    @AfterEach
    void fechar() throws Exception {
        if (db != null) db.close();
    }

    @Test
    void createGetEGetAllPersistemCategoria() throws Exception {
        var categoria = categoria("RPG", "Jogos de interpretação");

        var criada = aguardar(repository.create(categoria));

        assertEquals("rpg", criada.getTag());
        assertEquals("RPG", criada.getNome());
        assertEquals("Jogos de interpretação", criada.getDescricao());
        assertSame(categoria, criada);
        assertCategoria(criada, aguardar(repository.get("rpg")).orElseThrow());
        assertEquals(1, aguardar(repository.getAll()).size());
        assertEquals("rpg", aguardar(repository.getAll()).iterator().next().getTag());
    }

    @Test
    void createRejeitaTagEValorUnicoDuplicados() throws Exception {
        aguardar(repository.create(categoria("Ação", "Descrição inicial")));

        assertThrows(Exception.class, () -> aguardar(repository.create(categoria("Acao", "Outra descrição"))));
        assertThrows(Exception.class, () -> aguardar(repository.create(categoria("Ação", "Outra"))));
        assertEquals(1, aguardar(repository.getAll()).size());
    }

    @Test
    void saveCriaEAtualizaDescricaoMantendoNomeImutavel() throws Exception {
        var categoria = categoria("Aventura", "Descrição antiga");
        aguardar(repository.save(categoria));
        categoria.setDescricao("Descrição nova");

        var salva = aguardar(repository.save(categoria));

        assertEquals("Descrição nova", salva.getDescricao());
        var persistida = aguardar(repository.get("aventura")).orElseThrow();
        assertEquals("Descrição nova", persistida.getDescricao());
        assertEquals("Aventura", persistida.getNome());
    }

    @Test
    void getDeCategoriaAusenteRetornaVazio() throws Exception {
        assertTrue(aguardar(repository.get("inexistente")).isEmpty());
    }

    @Test
    void deleteRemoveCategoriaEValorUnico() throws Exception {
        aguardar(repository.create(categoria("Estratégia", "Tabuleiro")));

        aguardar(repository.delete("estrategia"));

        assertTrue(aguardar(repository.get("estrategia")).isEmpty());
        assertTrue(aguardar(repository.getAll()).isEmpty());
        aguardar(repository.create(categoria("Estratégia", "Agora permitida")));
        assertEquals(1, aguardar(repository.getAll()).size());
    }

    @Test
    void deleteDeIdAusenteConcluiSemErro() throws Exception {
        assertDoesNotThrow(() -> aguardar(repository.delete("inexistente")));
    }

    @Test
    void buscarPorNomeRetornaCorrespondenciaOuVazio() throws Exception {
        aguardar(repository.create(categoria("Corrida", "Velocidade")));

        assertEquals("corrida", aguardar(repository.buscarPorNome("Corrida")).orElseThrow().getTag());
        assertTrue(aguardar(repository.buscarPorNome("Luta")).isEmpty());
    }

    @Test
    void listarPorNomePaginaResultadosEmOrdemDeTag() throws Exception {
        aguardar(repository.create(categoria("RPG", "Interpretação")));
        aguardar(repository.create(categoria("Corrida", "Velocidade")));

        var resultado = aguardar(repository.listarPorNome("RPG", 2));
        assertEquals(java.util.List.of("rpg"), resultado.items().stream().map(CategoriaJogo::getTag).toList());
        assertTrue(resultado.nextCursor().isEmpty());
    }

    @Test
    void getPagePaginaTodosOsDocumentosPorId() throws Exception {
        aguardar(repository.create(categoria("Categoria A", "A")));
        aguardar(repository.create(categoria("Categoria B", "B")));
        aguardar(repository.create(categoria("Categoria C", "C")));

        var primeira = aguardar(repository.getPage(2));
        assertEquals(java.util.List.of("categoria_a", "categoria_b"), primeira.items().stream().map(CategoriaJogo::getTag).toList());
        var segunda = aguardar(repository.getPage(2, primeira.nextCursor().orElseThrow()));
        assertEquals(java.util.List.of("categoria_c"), segunda.items().stream().map(CategoriaJogo::getTag).toList());
        assertTrue(segunda.nextCursor().isEmpty());
    }

    @Test
    void scriptInsereCategoriasPadraoSemDuplicarQuandoExecutadoNovamente() throws Exception {
        var script = new DefaultCategoriasJogo(repository);

        script.executar();
        int quantidadeInicial = aguardar(repository.getAll()).size();
        assertTrue(quantidadeInicial > 0);
        assertTrue(aguardar(repository.buscarPorNome("Estratégia")).isPresent());
        assertTrue(aguardar(repository.buscarPorNome("Cooperativo")).isPresent());

        script.executar();

        assertEquals(quantidadeInicial, aguardar(repository.getAll()).size());
    }

    private static CategoriaJogo categoria(String nome, String descricao) {
        return CategoriaJogo.create(nome, descricao);
    }

    private static void assertCategoria(CategoriaJogo esperado, CategoriaJogo atual) {
        assertEquals(esperado.getTag(), atual.getTag());
        assertEquals(esperado.getNome(), atual.getNome());
        assertEquals(esperado.getDescricao(), atual.getDescricao());
    }

    private static <T> T aguardar(CompletableFuture<T> future) throws Exception {
        return future.get(10, TimeUnit.SECONDS);
    }
}
