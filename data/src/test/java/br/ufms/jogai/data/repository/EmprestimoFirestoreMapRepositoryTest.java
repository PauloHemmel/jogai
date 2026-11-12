package br.ufms.jogai.data.repository;

import br.ufms.jogai.data.firebase.DuplicateUniqueValueException;
import br.ufms.jogai.domain.emprestimo.Emprestimo;
import br.ufms.jogai.domain.emprestimo.EmprestimoRepository;
import br.ufms.jogai.domain.emprestimo.ItemEmprestimo;
import br.ufms.jogai.domain.jogo.CategoriaJogo;
import br.ufms.jogai.domain.jogo.EstadoConservacao;
import br.ufms.jogai.domain.jogo.Exemplar;
import br.ufms.jogai.domain.jogo.JogoInfo;
import br.ufms.jogai.domain.usuario.UsuarioInfo;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Exercita a persistência real no emulador, sem documentos relacionados ou credenciais de produção. */
@EnabledIfEnvironmentVariable(named = "FIRESTORE_EMULATOR_HOST", matches = ".+")
class EmprestimoFirestoreMapRepositoryTest {

    private static final Instant INICIO = Instant.parse("2026-01-01T15:30:00.123456Z");
    private static final Instant VENCIMENTO = INICIO.plus(7, ChronoUnit.DAYS);
    private Firestore banco;
    private EmprestimoFirestoreMapRepository repositorio;
    private final UsuarioInfo proprietario = participante("Maria Silva", "maria@example.com");
    private final UsuarioInfo tomador = participante("João Souza", "joao@example.com");

    @BeforeEach
    void configurar() {
        banco = FirestoreOptions.newBuilder()
                .setProjectId("demo-jogai-emprestimos-" + UUID.randomUUID().toString().substring(0, 8))
                .setEmulatorHost(System.getenv("FIRESTORE_EMULATOR_HOST"))
                .build().getService();
        repositorio = new EmprestimoFirestoreMapRepository(banco);
    }

    @AfterEach
    void fechar() throws Exception {
        if (banco != null) banco.close();
    }

    @Test
    void gravaEReconstroiTodosOsDadosComVariosItens() throws Exception {
        var emprestimo = emprestimo(UUID.randomUUID(), proprietario, tomador);
        emprestimo.adicionarItem(exemplar(proprietario, "Conferido na entrega"));
        emprestimo.registrarDevolucao(emprestimo.getItens().getLast().getId(), VENCIMENTO.plusSeconds(300));

        assertSame(emprestimo, aguardar(repositorio.create(emprestimo)));
        var carregado = aguardar(repositorio.get(emprestimo.getId())).orElseThrow();
        verificarEmprestimo(emprestimo, carregado);
        assertNotSame(emprestimo, carregado);
        assertNull(carregado.getItens().getFirst().getDataEfetivaDevolucao());
        assertNull(carregado.getItens().getFirst().getExemplarInfo().observacao());

        var documento = banco.collection("emprestimos").document(emprestimo.getId().toString()).get().get();
        assertFalse(documento.contains("id"));
        assertEquals(proprietario.id().toString(), documento.getString("proprietario.id"));
        assertEquals(tomador.id().toString(), documento.getString("tomador.id"));
        assertEquals(Timestamp.ofTimeSecondsAndNanos(INICIO.getEpochSecond(), INICIO.getNano()),
                documento.getTimestamp("dataEmprestimo"));
        assertTrue(banco.collection("usuarios").get().get().isEmpty());
        assertTrue(banco.collection("exemplares").get().get().isEmpty());
        assertTrue(banco.collection("jogos").get().get().isEmpty());
        assertTrue(banco.collection("categoriasJogo").get().get().isEmpty());
    }

    @Test
    void mantemHistoricoAposAlteracoesEExclusoesNosCadastros() throws Exception {
        var exemplar = exemplar(proprietario, "Completo");
        var emprestimo = Emprestimo.criar(proprietario, tomador, INICIO, VENCIMENTO,
                List.of(ItemEmprestimo.criar(exemplar)));
        aguardar(repositorio.create(emprestimo));

        exemplar.setEstadoConservacao(EstadoConservacao.DANIFICADO);
        exemplar.setObservacao("Alterado depois");
        var cadastroUsuario = banco.collection("usuarios").document(proprietario.id().toString());
        var cadastroExemplar = banco.collection("exemplares").document(exemplar.getId().toString());
        cadastroUsuario.set(Map.of("nome", "Outro Nome")).get();
        cadastroExemplar.set(Map.of("estadoConservacao", "DANIFICADO")).get();
        verificarEmprestimo(emprestimo, aguardar(repositorio.get(emprestimo.getId())).orElseThrow());
        cadastroUsuario.delete().get();
        cadastroExemplar.delete().get();

        var historico = aguardar(repositorio.get(emprestimo.getId())).orElseThrow();
        assertEquals("Maria Silva", historico.getProprietario().nome());
        assertEquals(EstadoConservacao.BOM, historico.getItens().getFirst().getExemplarInfo().estadoConservacao());
        assertEquals("Completo", historico.getItens().getFirst().getExemplarInfo().observacao());
    }

    @Test
    void saveCriaRegistroEAtualizaSomenteADevolucaoDoItemInformado() throws Exception {
        var emprestimo = emprestimo(UUID.randomUUID(), proprietario, tomador);
        emprestimo.adicionarItem(exemplar(proprietario, null));
        aguardar(repositorio.save(emprestimo));
        var item = emprestimo.getItens().getFirst();
        emprestimo.registrarDevolucao(item.getId(), VENCIMENTO);
        aguardar(repositorio.save(emprestimo));

        var carregado = aguardar(repositorio.get(emprestimo.getId())).orElseThrow();
        verificarEmprestimo(emprestimo, carregado);
        assertEquals(VENCIMENTO, carregado.getItens().getFirst().getDataEfetivaDevolucao());
        assertNull(carregado.getItens().getLast().getDataEfetivaDevolucao());
    }

    @Test
    void capturaOsDadosAntesDeConcluirAGravacaoAssincrona() throws Exception {
        var emprestimo = emprestimo(UUID.randomUUID(), proprietario, tomador);
        var gravacao = repositorio.create(emprestimo);
        emprestimo.registrarDevolucao(emprestimo.getItens().getFirst().getId(), VENCIMENTO);
        aguardar(gravacao);

        var carregado = aguardar(repositorio.get(emprestimo.getId())).orElseThrow();
        assertNull(carregado.getItens().getFirst().getDataEfetivaDevolucao());
    }

    @Test
    void rejeitaPrazoQueSeTornariaInvalidoNaPrecisaoDoFirestore() throws Exception {
        var emprestimo = Emprestimo.criar(proprietario, tomador, INICIO, INICIO.plusNanos(1),
                List.of(ItemEmprestimo.criar(exemplar(proprietario, null))));
        assertThrows(IllegalArgumentException.class, () -> repositorio.create(emprestimo));
        assertTrue(banco.collection("emprestimos").get().get().isEmpty());
    }

    @Test
    void createRejeitaIdDuplicadoSemSubstituirDocumento() throws Exception {
        var emprestimo = emprestimo(UUID.randomUUID(), proprietario, tomador);
        aguardar(repositorio.create(emprestimo));
        emprestimo.registrarDevolucao(emprestimo.getItens().getFirst().getId(), VENCIMENTO);
        var erro = assertThrows(ExecutionException.class, () -> aguardar(repositorio.create(emprestimo)));
        assertEquals("id", assertInstanceOf(DuplicateUniqueValueException.class, erro.getCause()).getField());
        assertNull(aguardar(repositorio.get(emprestimo.getId())).orElseThrow()
                .getItens().getFirst().getDataEfetivaDevolucao());
    }

    @Test
    void consultaAusenteEExclusaoRepetidaNaoCriamRegistros() throws Exception {
        var id = UUID.randomUUID();
        assertTrue(aguardar(repositorio.get(id)).isEmpty());
        aguardar(repositorio.delete(id));
        var emprestimo = emprestimo(id, proprietario, tomador);
        aguardar(repositorio.create(emprestimo));
        aguardar(repositorio.delete(id));
        aguardar(repositorio.delete(id));
        assertTrue(aguardar(repositorio.get(id)).isEmpty());
        assertTrue(aguardar(repositorio.getAll()).isEmpty());
    }

    @Test
    void paginaPorParticipanteSemMisturarOsPapeis() throws Exception {
        prepararConsultas();
        EmprestimoRepository contrato = repositorio;
        var primeira = aguardar(contrato.listarPorProprietario(proprietario.id(), 2));
        assertEquals(List.of(id(1), id(2)), primeira.items().stream().map(Emprestimo::getId).toList());
        var segunda = aguardar(contrato.listarPorProprietario(proprietario.id(), 2,
                primeira.nextCursor().orElseThrow()));
        assertEquals(List.of(id(3), id(5)), segunda.items().stream().map(Emprestimo::getId).toList());
        assertTrue(segunda.nextCursor().isEmpty());

        var tomados = aguardar(contrato.listarPorTomador(tomador.id(), 3));
        assertEquals(List.of(id(1), id(2), id(3)), tomados.items().stream().map(Emprestimo::getId).toList());
        var restantes = aguardar(contrato.listarPorTomador(tomador.id(), 3, tomados.nextCursor().orElseThrow()));
        assertEquals(List.of(id(4)), restantes.items().stream().map(Emprestimo::getId).toList());
        assertTrue(restantes.nextCursor().isEmpty());
        assertEquals(4, aguardar(contrato.listarPorProprietario(proprietario.id())).items().size());
        assertEquals(4, aguardar(contrato.listarPorTomador(tomador.id())).items().size());
        assertTrue(aguardar(contrato.listarPorProprietario(tomador.id())).items().isEmpty());
        assertTrue(aguardar(contrato.listarPorTomador(proprietario.id())).items().isEmpty());
    }

    @Test
    void listaTodosEPaginaPeloIdentificador() throws Exception {
        prepararConsultas();
        assertEquals(5, aguardar(repositorio.getAll()).size());
        var primeira = aguardar(repositorio.getPage(3));
        assertEquals(List.of(id(1), id(2), id(3)), primeira.items().stream().map(Emprestimo::getId).toList());
        var segunda = aguardar(repositorio.getPage(3, primeira.nextCursor().orElseThrow()));
        assertEquals(List.of(id(4), id(5)), segunda.items().stream().map(Emprestimo::getId).toList());
        assertTrue(segunda.nextCursor().isEmpty());
    }

    @Test
    void rejeitaParametrosInvalidosECursorAusente() throws Exception {
        assertThrows(NullPointerException.class, () -> repositorio.listarPorProprietario(null));
        assertThrows(NullPointerException.class, () -> repositorio.listarPorTomador(null));
        for (int tamanho : new int[]{0, 201}) {
            assertThrows(IllegalArgumentException.class, () -> repositorio.listarPorProprietario(proprietario.id(), tamanho));
            assertThrows(IllegalArgumentException.class, () -> repositorio.listarPorTomador(tomador.id(), tamanho));
        }
        var erro = assertThrows(ExecutionException.class,
                () -> aguardar(repositorio.listarPorProprietario(proprietario.id(), 2, UUID.randomUUID())));
        assertInstanceOf(IllegalArgumentException.class, erro.getCause());
    }

    @ParameterizedTest
    @ValueSource(strings = {"itens", "proprietario", "tomador", "dataEmprestimo", "dataPrevistaDevolucao"})
    void rejeitaDocumentoComCampoObrigatorioDeTipoInvalido(String campo) throws Exception {
        var emprestimo = emprestimo(UUID.randomUUID(), proprietario, tomador);
        var dados = new HashMap<>(repositorio.entityToMap(emprestimo));
        dados.put(campo, "tipo incorreto");
        banco.collection("emprestimos").document(emprestimo.getId().toString()).set(dados).get();
        verificarDocumentoInvalido(emprestimo.getId());
    }

    @Test
    void rejeitaItemIncompletoEListaVaziaEmVezDeInventarDados() throws Exception {
        var emprestimo = emprestimo(UUID.randomUUID(), proprietario, tomador);
        aguardar(repositorio.create(emprestimo));
        var documento = banco.collection("emprestimos").document(emprestimo.getId().toString());
        documento.update("itens", List.of(Map.of("id", UUID.randomUUID().toString()))).get();
        verificarDocumentoInvalido(emprestimo.getId());
        documento.update("itens", List.of()).get();
        verificarDocumentoInvalido(emprestimo.getId());
    }

    private void verificarDocumentoInvalido(UUID id) {
        var erro = assertThrows(ExecutionException.class, () -> aguardar(repositorio.get(id)));
        assertInstanceOf(IllegalArgumentException.class, erro.getCause());
        assertTrue(erro.getCause().getMessage().contains(id.toString()));
        assertNotNull(erro.getCause().getCause());
    }

    private void prepararConsultas() throws Exception {
        var terceiro = participante("Pedro Santos", "pedro@example.com");
        for (int numero = 1; numero <= 3; numero++) {
            aguardar(repositorio.create(emprestimo(id(numero), proprietario, tomador)));
        }
        aguardar(repositorio.create(emprestimo(id(4), terceiro, tomador)));
        aguardar(repositorio.create(emprestimo(id(5), proprietario, terceiro)));
    }

    private static void verificarEmprestimo(Emprestimo esperado, Emprestimo atual) {
        assertEquals(esperado.getId(), atual.getId());
        assertEquals(esperado.getProprietario(), atual.getProprietario());
        assertEquals(esperado.getTomador(), atual.getTomador());
        assertEquals(esperado.getDataEmprestimo(), atual.getDataEmprestimo());
        assertEquals(esperado.getDataPrevistaDevolucao(), atual.getDataPrevistaDevolucao());
        assertEquals(esperado.getItens().size(), atual.getItens().size());
        for (int indice = 0; indice < esperado.getItens().size(); indice++) {
            var itemEsperado = esperado.getItens().get(indice);
            var itemAtual = atual.getItens().get(indice);
            assertEquals(itemEsperado.getId(), itemAtual.getId());
            assertEquals(itemEsperado.getExemplarInfo(), itemAtual.getExemplarInfo());
            assertEquals(itemEsperado.getDataEfetivaDevolucao(), itemAtual.getDataEfetivaDevolucao());
        }
    }

    private static UUID id(int numero) {
        return new UUID(0, numero);
    }

    private static UsuarioInfo participante(String nome, String email) {
        return new UsuarioInfo(UUID.randomUUID(), nome, email, "67996122801", LocalDate.of(1990, 1, 1), 4.25, 3.5);
    }

    private static Exemplar exemplar(UsuarioInfo proprietario, String observacao) {
        var jogo = new JogoInfo(UUID.randomUUID(), "Catan", "Devir", Set.of(CategoriaJogo.create("Estratégia", null)));
        return Exemplar.create(jogo, proprietario, EstadoConservacao.BOM, observacao);
    }

    private static Emprestimo emprestimo(UUID id, UsuarioInfo proprietario, UsuarioInfo tomador) {
        return Emprestimo.reconstituir(id, proprietario, tomador, INICIO, VENCIMENTO,
                List.of(ItemEmprestimo.criar(exemplar(proprietario, null))));
    }

    private static <T> T aguardar(CompletableFuture<T> operacao) throws Exception {
        return operacao.get(15, TimeUnit.SECONDS);
    }
}
