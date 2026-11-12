package br.ufms.jogai.data.repository;

import br.ufms.jogai.domain.jogo.CategoriaJogo;
import br.ufms.jogai.domain.jogo.EstadoConservacao;
import br.ufms.jogai.domain.jogo.Exemplar;
import br.ufms.jogai.domain.jogo.Jogo;
import br.ufms.jogai.domain.jogo.JogoInfo;
import br.ufms.jogai.domain.usuario.Usuario;
import br.ufms.jogai.domain.usuario.UsuarioInfo;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "FIRESTORE_EMULATOR_HOST", matches = ".+")
class ExemplarFirestoreMapRepositoryTest {

    private Firestore banco;
    private ExemplarFirestoreMapRepository repository;
    private final CategoriaJogo categoria = CategoriaJogo.create("Estratégia", null);
    private final Jogo jogo = Jogo.create().nomeEditora("Jogo de Teste", "Editora Teste")
            .numeroJogadores(2, 4).categorias(categoria).build();
    private final UsuarioInfo proprietario = usuario("Maria Silva", "maria", "maria@example.com");

    @BeforeEach
    void configurar() {
        banco = FirestoreOptions.newBuilder()
                .setProjectId("demo-jogai-exemplares-" + UUID.randomUUID().toString().substring(0, 8))
                .setEmulatorHost(System.getenv("FIRESTORE_EMULATOR_HOST"))
                .build().getService();
        repository = new ExemplarFirestoreMapRepository(banco);
    }

    @AfterEach
    void fechar() throws Exception {
        if (banco != null) banco.close();
    }

    @Test
    void preservaProprietarioJogoECategoriaSemDependerDeOutrosDocumentos() throws Exception {
        var exemplar = Exemplar.create(new JogoInfo(jogo), proprietario, EstadoConservacao.BOM, null);
        aguardar(repository.create(exemplar));
        jogo.setNome("Nome Alterado Depois");
        categoria.setDescricao("Descrição alterada depois");

        var salvo = aguardar(repository.get(exemplar.getId())).orElseThrow();

        assertEquals(exemplar.getId(), salvo.getId());
        assertEquals(proprietario, salvo.getProprietarioInfo());
        assertEquals(jogo.getId(), salvo.getJogoInfo().id());
        assertEquals("Jogo de Teste", salvo.getJogoInfo().nome());
        assertEquals("Editora Teste", salvo.getJogoInfo().editora());
        assertEquals("Estratégia", salvo.getJogoInfo().categorias().iterator().next().getNome());
        assertNull(salvo.getJogoInfo().categorias().iterator().next().getDescricao());
        assertEquals(EstadoConservacao.BOM, salvo.getEstadoConservacao());
        assertNull(salvo.getObservacao());
        assertEquals(exemplar.getDataCadastro().truncatedTo(ChronoUnit.MICROS), salvo.getDataCadastro());
        for (String collection : List.of("usuarios", "jogos", "categoriasJogo")) {
            assertTrue(banco.collection(collection).get().get().isEmpty());
        }
    }

    @Test
    void atualizaObservacaoEEstadoDepoisExcluiSomenteOExemplarInformado() throws Exception {
        var exemplar = exemplar(proprietario);
        var outro = exemplar(proprietario);
        aguardar(repository.save(exemplar));
        aguardar(repository.create(outro));
        exemplar.setObservacao("Peça faltando");
        exemplar.setEstadoConservacao(EstadoConservacao.DANIFICADO);
        aguardar(repository.save(exemplar));

        var salvo = aguardar(repository.get(exemplar.getId())).orElseThrow();
        assertEquals("Peça faltando", salvo.getObservacao());
        assertFalse(salvo.isJogavel());

        exemplar.setObservacao(null);
        aguardar(repository.save(exemplar));
        assertNull(aguardar(repository.get(exemplar.getId())).orElseThrow().getObservacao());
        aguardar(repository.delete(exemplar.getId()));
        assertTrue(aguardar(repository.get(exemplar.getId())).isEmpty());
        assertEquals(outro.getId(), aguardar(repository.getAll()).iterator().next().getId());
    }

    @Test
    void aceitaMultiplasCopiasEFiltraJogoProprietarioEEstadoComPaginacao() throws Exception {
        var primeiro = exemplar(proprietario);
        var segundo = exemplar(proprietario);
        segundo.setEstadoConservacao(EstadoConservacao.DANIFICADO);
        var outroProprietario = usuario("João Souza", "joao", "joao@example.com");
        var terceiro = exemplar(outroProprietario);
        var outroJogo = new JogoInfo(UUID.randomUUID(), "Outro Jogo", "Editora Teste", Set.of(categoria));
        var quarto = Exemplar.create(outroJogo, proprietario, EstadoConservacao.BOM, null);
        for (var exemplar : List.of(primeiro, segundo, terceiro, quarto)) aguardar(repository.create(exemplar));

        var pagina = aguardar(repository.listarPorJogo(jogo.getId(), 2, null));
        var seguinte = aguardar(repository.listarPorJogo(jogo.getId(), 2, pagina.nextCursor().orElseThrow()));
        var ids = new java.util.HashSet<UUID>();
        pagina.items().forEach(exemplar -> assertTrue(ids.add(exemplar.getId())));
        seguinte.items().forEach(exemplar -> assertTrue(ids.add(exemplar.getId())));

        assertEquals(Set.of(primeiro.getId(), segundo.getId(), terceiro.getId()), ids);
        assertTrue(seguinte.nextCursor().isEmpty());
        assertEquals(3, aguardar(repository.listarPorProprietario(proprietario.id())).items().size());
        assertEquals(segundo.getId(), aguardar(repository.listarPorEstadoConservacao(EstadoConservacao.DANIFICADO))
                .items().getFirst().getId());
        assertTrue(aguardar(repository.listarPorJogo(UUID.randomUUID())).items().isEmpty());
    }

    private Exemplar exemplar(UsuarioInfo proprietario) {
        return Exemplar.create(new JogoInfo(jogo), proprietario, EstadoConservacao.BOM, "Completo");
    }

    private static UsuarioInfo usuario(String nome, String username, String email) {
        return new UsuarioInfo(Usuario.create(nome, username, email, "67996122801", LocalDate.of(1990, 1, 1)));
    }

    private static <T> T aguardar(CompletableFuture<T> future) throws Exception {
        return future.get(15, TimeUnit.SECONDS);
    }
}
