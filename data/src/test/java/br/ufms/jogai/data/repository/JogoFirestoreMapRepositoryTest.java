package br.ufms.jogai.data.repository;

import br.ufms.jogai.data.firebase.DuplicateUniqueValueException;
import br.ufms.jogai.domain.jogo.CategoriaJogo;
import br.ufms.jogai.domain.jogo.DuracaoPartida;
import br.ufms.jogai.domain.jogo.Jogo;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "FIRESTORE_EMULATOR_HOST", matches = ".+")
class JogoFirestoreMapRepositoryTest {

    private Firestore banco;
    private JogoFirestoreMapRepository repository;
    private final CategoriaJogo categoria = CategoriaJogo.create("Estratégia", null);

    @BeforeEach
    void configurar() {
        banco = FirestoreOptions.newBuilder()
                .setProjectId("demo-jogai-jogos-" + UUID.randomUUID().toString().substring(0, 8))
                .setEmulatorHost(System.getenv("FIRESTORE_EMULATOR_HOST"))
                .build().getService();
        repository = new JogoFirestoreMapRepository(banco);
    }

    @AfterEach
    void fechar() throws Exception {
        if (banco != null) banco.close();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void preservaDadosOpcionaisCategoriasEPrecisaoDaDuracao(boolean completo) throws Exception {
        var duracao = completo ? Duration.ofSeconds(90, 123456789) : null;
        var jogo = Jogo.create().nomeEditoraLancamento("Jogo de Teste", "Editora Teste", completo ? 2024 : null)
                .numeroJogadores(2, completo ? 6 : null)
                .faixaEtaria(completo ? 10 : null, null)
                .duracaoPartida(new DuracaoPartida(duracao, null))
                .categorias(categoria).descricao(completo ? "Descrição completa" : null).build();
        jogo.inativar();

        aguardar(repository.create(jogo));
        var salvo = aguardar(repository.get(jogo.getId())).orElseThrow();

        assertEquals(jogo.getId(), salvo.getId());
        assertEquals(jogo.getNome(), salvo.getNome());
        assertEquals(jogo.getEditora(), salvo.getEditora());
        assertEquals(jogo.getAnoLancamento(), salvo.getAnoLancamento());
        assertEquals(jogo.getMaximoJogadores(), salvo.getMaximoJogadores());
        assertEquals(jogo.getIdadeMinimaRecomendada(), salvo.getIdadeMinimaRecomendada());
        assertEquals(duracao, salvo.getTempoMinimoEstimado());
        assertNull(salvo.getTempoMaximoEstimado());
        assertEquals(jogo.getDescricao(), salvo.getDescricao());
        assertFalse(salvo.isAtivo());
        var categoriaSalva = salvo.getCategorias().iterator().next();
        assertEquals("estrategia", categoriaSalva.getTag());
        assertEquals("Estratégia", categoriaSalva.getNome());
        assertNull(categoriaSalva.getDescricao());
        assertTrue(banco.collection("categoriasJogo").get().get().isEmpty());
    }

    @Test
    void nomeUnicoEAtualizacaoLiberamSomenteReservaAnterior() throws Exception {
        var jogo = jogo("Nome Original", categoria);
        aguardar(repository.create(jogo));

        var falha = assertThrows(ExecutionException.class,
                () -> aguardar(repository.create(jogo("Nome Original", categoria))));
        assertInstanceOf(DuplicateUniqueValueException.class, falha.getCause());

        jogo.setNome("Nome Atualizado");
        aguardar(repository.save(jogo));
        var outro = jogo("Nome Original", categoria);
        aguardar(repository.create(outro));
        assertTrue(aguardar(repository.buscarPorNome("Nome Atualizado")).isPresent());
        assertEquals(outro.getId(), aguardar(repository.buscarPorNome("Nome   Original")).orElseThrow().getId());

        aguardar(repository.delete(jogo.getId()));
        assertTrue(aguardar(repository.buscarPorNome("Nome Atualizado")).isEmpty());
        aguardar(repository.create(jogo("Nome Atualizado", categoria)));
        assertEquals(2, aguardar(repository.getAll()).size());
    }

    @Test
    void filtraPorCategoriaEditoraEAtivosComContinuacaoDePagina() throws Exception {
        var outraCategoria = CategoriaJogo.create("Cartas", "Baralhos");
        var primeiro = jogo("Primeiro Jogo", categoria);
        var segundo = jogo("Segundo Jogo", categoria);
        var terceiro = jogo("Terceiro Jogo", categoria);
        terceiro.inativar();
        var outro = jogo("Outro Jogo", outraCategoria);
        outro.setEditora("Outra Editora");
        for (var jogo : List.of(primeiro, segundo, terceiro, outro)) aguardar(repository.create(jogo));

        var pagina = aguardar(repository.listarPorCategoria(categoria, 2, null));
        var seguinte = aguardar(repository.listarPorCategoria(categoria, 2, pagina.nextCursor().orElseThrow()));
        var ids = new java.util.HashSet<UUID>();
        pagina.items().forEach(jogo -> assertTrue(ids.add(jogo.getId())));
        seguinte.items().forEach(jogo -> assertTrue(ids.add(jogo.getId())));

        assertEquals(Set.of(primeiro.getId(), segundo.getId(), terceiro.getId()), ids);
        assertTrue(seguinte.nextCursor().isEmpty());
        assertEquals(3, aguardar(repository.listarPorEditora("Editora  Teste")).items().size());
        assertEquals(3, aguardar(repository.listarAtivos()).items().size());
        assertTrue(aguardar(repository.buscarPorNome("Inexistente")).isEmpty());
    }

    @Test
    void suportaLimitesDeDuracaoAusentesNoBuilderEmMinutos() throws Exception {
        var jogo = Jogo.create().nomeEditora("Duração Opcional", "Editora Teste")
                .numeroJogadores(1, null).duracaoPartida(null, 30).categorias(categoria).build();

        aguardar(repository.save(jogo));

        var salvo = aguardar(repository.get(jogo.getId())).orElseThrow();
        assertNull(salvo.getTempoMinimoEstimado());
        assertEquals(Duration.ofMinutes(30), salvo.getTempoMaximoEstimado());
    }

    private Jogo jogo(String nome, CategoriaJogo categoria) {
        return Jogo.create().nomeEditora(nome, "Editora Teste")
                .numeroJogadores(2, 4).categorias(categoria).build();
    }

    private static <T> T aguardar(CompletableFuture<T> future) throws Exception {
        return future.get(15, TimeUnit.SECONDS);
    }
}
