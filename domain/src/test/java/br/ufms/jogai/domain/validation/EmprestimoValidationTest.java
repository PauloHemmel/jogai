package br.ufms.jogai.domain.validation;

import br.ufms.jogai.domain.emprestimo.Emprestimo;
import br.ufms.jogai.domain.emprestimo.ItemEmprestimo;
import br.ufms.jogai.domain.jogo.CategoriaJogo;
import br.ufms.jogai.domain.jogo.EstadoConservacao;
import br.ufms.jogai.domain.jogo.Exemplar;
import br.ufms.jogai.domain.jogo.JogoInfo;
import br.ufms.jogai.domain.usuario.UsuarioInfo;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EmprestimoValidationTest {

    private static final Instant INICIO = Instant.parse("2026-01-01T15:30:00.123456Z");
    private static final Instant VENCIMENTO = INICIO.plus(7, ChronoUnit.DAYS);
    private final UsuarioInfo proprietario = participante("Maria Silva", "maria@example.com");
    private final UsuarioInfo tomador = participante("João Souza", "joao@example.com");
    private final Exemplar exemplar = exemplar(proprietario);
    private final ItemEmprestimo item = ItemEmprestimo.criar(exemplar);

    @Test
    void rejeitaIdentidadeNula() {
        assertThrows(NullPointerException.class, () -> reconstituir(null, List.of(item)));
        assertThrows(NullPointerException.class,
                () -> ItemEmprestimo.reconstituir(null, item.getExemplarInfo(), null));
    }

    @Test
    void preservaIdentidadeInformada() {
        var id = UUID.randomUUID();
        assertEquals(id, reconstituir(id, List.of(item)).getId());
        assertEquals(item.getId(), ItemEmprestimo.reconstituir(item.getId(), item.getExemplarInfo(), null).getId());
    }

    @Test
    void criaComParticipantesPrazoEItensInformados() {
        var emprestimo = criar(List.of(item));
        assertNotNull(emprestimo.getId());
        assertEquals(proprietario, emprestimo.getProprietario());
        assertEquals(tomador, emprestimo.getTomador());
        assertEquals(INICIO, emprestimo.getDataEmprestimo());
        assertEquals(VENCIMENTO, emprestimo.getDataPrevistaDevolucao());
        assertEquals(List.of(item), emprestimo.getItens());
        assertNull(item.getDataEfetivaDevolucao());
    }

    @Test
    void exigeParticipantesDatasEItens() {
        assertAll(
                () -> assertThrows(NullPointerException.class,
                        () -> Emprestimo.criar(null, tomador, INICIO, VENCIMENTO, List.of(item))),
                () -> assertThrows(NullPointerException.class,
                        () -> Emprestimo.criar(proprietario, null, INICIO, VENCIMENTO, List.of(item))),
                () -> assertThrows(NullPointerException.class,
                        () -> Emprestimo.criar(proprietario, tomador, null, VENCIMENTO, List.of(item))),
                () -> assertThrows(NullPointerException.class,
                        () -> Emprestimo.criar(proprietario, tomador, INICIO, null, List.of(item))),
                () -> assertThrows(NullPointerException.class, () -> criar(null)),
                () -> assertThrows(IllegalArgumentException.class, () -> criar(List.of())),
                () -> assertThrows(NullPointerException.class, () -> criar(Arrays.asList(item, null))),
                () -> assertThrows(NullPointerException.class, () -> ItemEmprestimo.criar(null)),
                () -> assertThrows(NullPointerException.class,
                        () -> ItemEmprestimo.reconstituir(UUID.randomUUID(), null, null))
        );
    }

    @Test
    void rejeitaMesmoParticipanteNosDoisPapeis() {
        assertThrows(IllegalArgumentException.class,
                () -> Emprestimo.criar(proprietario, proprietario, INICIO, VENCIMENTO, List.of(item)));
    }

    @Test
    void exigeVencimentoPosteriorAoInicio() {
        for (var vencimento : List.of(INICIO, INICIO.minusSeconds(1))) {
            assertThrows(IllegalArgumentException.class,
                    () -> Emprestimo.criar(proprietario, tomador, INICIO, vencimento, List.of(item)));
        }
    }

    @Test
    void rejeitaExemplarDeOutroProprietario() {
        assertThrows(IllegalArgumentException.class,
                () -> criar(List.of(ItemEmprestimo.criar(exemplar(tomador)))));
    }

    @Test
    void rejeitaItensEExemplaresRepetidosNaReconstituicao() {
        var mesmoExemplar = ItemEmprestimo.criar(exemplar);
        var mesmoId = ItemEmprestimo.reconstituir(item.getId(),
                ItemEmprestimo.criar(exemplar(proprietario)).getExemplarInfo(), null);
        assertThrows(IllegalArgumentException.class, () -> reconstituir(UUID.randomUUID(), List.of(item, item)));
        assertThrows(IllegalArgumentException.class, () -> reconstituir(UUID.randomUUID(), List.of(item, mesmoExemplar)));
        assertThrows(IllegalArgumentException.class, () -> reconstituir(UUID.randomUUID(), List.of(item, mesmoId)));
    }

    @Test
    void protegeListaRecebidaEListaRetornada() {
        var itens = new ArrayList<>(List.of(item));
        var emprestimo = criar(itens);
        itens.clear();
        assertEquals(1, emprestimo.getItens().size());
        assertThrows(UnsupportedOperationException.class, () -> emprestimo.getItens().clear());
    }

    @Test
    void mantemDadosDaEpocaAposAlteracaoDoExemplar() {
        exemplar.setEstadoConservacao(EstadoConservacao.DANIFICADO);
        exemplar.setObservacao("Peça perdida depois do empréstimo");
        assertEquals(EstadoConservacao.BOM, item.getExemplarInfo().estadoConservacao());
        assertNull(item.getExemplarInfo().observacao());
        assertEquals(proprietario.id(), item.getExemplarInfo().idProprietario());
    }

    @Test
    void adicionaERemoveItemSemPermitirOperacoesQueInvalidemORegistro() {
        var emprestimo = criar(List.of(item));
        var segundoExemplar = exemplar(proprietario);
        emprestimo.adicionarItem(segundoExemplar);
        var segundoItem = emprestimo.getItens().getLast();
        assertThrows(IllegalArgumentException.class, () -> emprestimo.adicionarItem(segundoExemplar));
        assertThrows(IllegalArgumentException.class, () -> emprestimo.adicionarItem(exemplar(tomador)));
        assertThrows(IllegalArgumentException.class, () -> emprestimo.removerItem(UUID.randomUUID()));
        assertEquals(2, emprestimo.getItens().size());
        emprestimo.removerItem(segundoItem.getId());
        assertThrows(IllegalArgumentException.class, () -> emprestimo.removerItem(item.getId()));
        assertEquals(List.of(item), emprestimo.getItens());
    }

    @Test
    void registraECorrigeDevolucaoSemMudarItemAnterior() {
        var emprestimo = criar(List.of(item));
        emprestimo.registrarDevolucao(item.getId(), INICIO);
        var primeiraVersao = emprestimo.getItens().getFirst();
        emprestimo.registrarDevolucao(item.getId(), VENCIMENTO.plusSeconds(3600));
        var ultimaVersao = emprestimo.getItens().getFirst();
        assertEquals(VENCIMENTO.plusSeconds(3600), ultimaVersao.getDataEfetivaDevolucao());
        assertEquals(item.getId(), ultimaVersao.getId());
        assertEquals(item.getExemplarInfo(), ultimaVersao.getExemplarInfo());
        assertEquals(INICIO, primeiraVersao.getDataEfetivaDevolucao());
        assertNull(item.getDataEfetivaDevolucao());
    }

    @Test
    void rejeitaDevolucaoInvalidaSemAlterarItem() {
        var emprestimo = criar(List.of(item));
        assertThrows(NullPointerException.class, () -> emprestimo.registrarDevolucao(item.getId(), null));
        assertThrows(IllegalArgumentException.class,
                () -> emprestimo.registrarDevolucao(item.getId(), INICIO.minusSeconds(1)));
        assertThrows(IllegalArgumentException.class,
                () -> emprestimo.registrarDevolucao(UUID.randomUUID(), INICIO));
        var itemInvalido = ItemEmprestimo.reconstituir(item.getId(), item.getExemplarInfo(), INICIO.minusSeconds(1));
        assertThrows(IllegalArgumentException.class, () -> reconstituir(UUID.randomUUID(), List.of(itemInvalido)));
        assertNull(emprestimo.getItens().getFirst().getDataEfetivaDevolucao());
    }

    @Test
    void rejeitaParticipanteSemIdentidadeOuComMediaInvalida() {
        for (double media : new double[]{-1, 6, Double.NaN, Double.POSITIVE_INFINITY}) {
            var invalido = new UsuarioInfo(tomador.id(), tomador.nome(), tomador.email(), tomador.telefone(),
                    tomador.dataNascimento(), media, 0);
            assertThrows(IllegalArgumentException.class,
                    () -> Emprestimo.criar(proprietario, invalido, INICIO, VENCIMENTO, List.of(item)));
        }
        var semId = new UsuarioInfo(null, tomador.nome(), tomador.email(), tomador.telefone(),
                tomador.dataNascimento(), 0, 0);
        assertThrows(NullPointerException.class,
                () -> Emprestimo.criar(proprietario, semId, INICIO, VENCIMENTO, List.of(item)));
    }

    @Test
    void validaNascimentoNaEpocaDoEmprestimoSemExpirarHistorico() {
        var inicioHistorico = Instant.parse("1950-01-01T12:00:00Z");
        var proprietarioHistorico = new UsuarioInfo(proprietario.id(), proprietario.nome(), proprietario.email(),
                proprietario.telefone(), LocalDate.of(1850, 1, 1), 0, 0);
        var tomadorHistorico = new UsuarioInfo(tomador.id(), tomador.nome(), tomador.email(), tomador.telefone(),
                LocalDate.of(1900, 1, 1), 0, 0);
        var historico = Emprestimo.reconstituir(UUID.randomUUID(), proprietarioHistorico, tomadorHistorico,
                inicioHistorico, inicioHistorico.plus(7, ChronoUnit.DAYS), List.of(item));
        assertEquals(proprietarioHistorico, historico.getProprietario());
        assertThrows(IllegalArgumentException.class, () -> Emprestimo.criar(proprietario, tomador,
                inicioHistorico, inicioHistorico.plus(7, ChronoUnit.DAYS), List.of(item)));
    }

    private Emprestimo criar(List<ItemEmprestimo> itens) {
        return Emprestimo.criar(proprietario, tomador, INICIO, VENCIMENTO, itens);
    }

    private Emprestimo reconstituir(UUID id, List<ItemEmprestimo> itens) {
        return Emprestimo.reconstituir(id, proprietario, tomador, INICIO, VENCIMENTO, itens);
    }

    private static UsuarioInfo participante(String nome, String email) {
        return new UsuarioInfo(UUID.randomUUID(), nome, email, "67996122801", LocalDate.of(1990, 1, 1), 4.25, 3.5);
    }

    private static Exemplar exemplar(UsuarioInfo proprietario) {
        var jogo = new JogoInfo(UUID.randomUUID(), "Catan", "Devir", Set.of(CategoriaJogo.create("Estratégia", null)));
        return Exemplar.create(jogo, proprietario, EstadoConservacao.BOM, null);
    }
}
