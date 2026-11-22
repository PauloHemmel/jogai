package br.ufms.jogai.domain.validation;

import br.ufms.jogai.domain.emprestimo.Devolucao;
import br.ufms.jogai.domain.emprestimo.Emprestimo;
import br.ufms.jogai.domain.emprestimo.ItemEmprestimo;
import br.ufms.jogai.domain.jogo.CategoriaJogo;
import br.ufms.jogai.domain.jogo.EstadoConservacao;
import br.ufms.jogai.domain.jogo.Exemplar;
import br.ufms.jogai.domain.jogo.Jogo;
import br.ufms.jogai.domain.jogo.JogoInfo;
import br.ufms.jogai.domain.reserva.Reserva;
import br.ufms.jogai.domain.reserva.StatusReserva;
import br.ufms.jogai.domain.usuario.Usuario;
import br.ufms.jogai.domain.usuario.UsuarioInfo;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReservaDevolucaoValidationTest {

    private final Usuario usuario = Usuario.create("Maria Silva", "maria", "maria@example.com",
            "67996122801", LocalDate.of(1990, 1, 1));
    private final Jogo jogo = Jogo.create().nomeEditora("Jogo de Teste", "Editora Teste")
            .numeroJogadores(2, 4).categorias(CategoriaJogo.create("Estratégia", null)).build();

    @Test
    void reservaNasceAtivaEPodeSerAtendidaOuCanceladaUmaVez() {
        var reserva = Reserva.criar(usuario, jogo);
        assertEquals(StatusReserva.ATIVA, reserva.getStatus());
        assertEquals(usuario.getId(), reserva.getUsuarioId());
        assertEquals(jogo.getId(), reserva.getJogoId());

        reserva.atender();
        assertEquals(StatusReserva.ATENDIDA, reserva.getStatus());
        assertThrows(IllegalStateException.class, reserva::cancelar);

        var outra = Reserva.criar(usuario, jogo);
        outra.cancelar();
        assertEquals(StatusReserva.CANCELADA, outra.getStatus());
        assertThrows(IllegalStateException.class, outra::atender);
    }

    @Test
    void reservaRejeitaUsuarioOuJogoInativoEDadosNulos() {
        usuario.inativar();
        assertThrows(IllegalArgumentException.class, () -> Reserva.criar(usuario, jogo));
        usuario.ativar();
        jogo.inativar();
        assertThrows(IllegalArgumentException.class, () -> Reserva.criar(usuario, jogo));

        assertThrows(NullPointerException.class, () -> Reserva.criar(null, jogo));
        assertThrows(IllegalArgumentException.class,
                () -> Reserva.reconstituir(UUID.randomUUID(), null, jogo.getId(), Instant.now(), StatusReserva.ATIVA));
        assertThrows(NullPointerException.class,
                () -> Reserva.reconstituir(UUID.randomUUID(), usuario.getId(), jogo.getId(), Instant.now(), null));
    }

    @Test
    void devolucaoValidaDataEAvaliacoes() {
        var emprestimo = emprestimo();
        var devolucao = Devolucao.criar(emprestimo, emprestimo.getDataEmprestimo().plus(3, ChronoUnit.DAYS));
        assertEquals(emprestimo.getId(), devolucao.getEmprestimoId());
        assertNull(devolucao.getAvaliacaoDoProprietario());
        assertNull(devolucao.getAvaliacaoDoTomador());

        devolucao.avaliarPeloProprietario(5);
        devolucao.avaliarPeloTomador(4);
        assertEquals(5, devolucao.getAvaliacaoDoProprietario());
        assertEquals(4, devolucao.getAvaliacaoDoTomador());

        assertThrows(IllegalArgumentException.class, () -> devolucao.avaliarPeloProprietario(6));
        assertThrows(IllegalArgumentException.class, () -> devolucao.avaliarPeloTomador(-1));
        assertThrows(IllegalArgumentException.class,
                () -> Devolucao.criar(emprestimo, emprestimo.getDataEmprestimo().minusSeconds(1)));
        assertThrows(NullPointerException.class, () -> Devolucao.criar(null, Instant.now()));
        assertThrows(IllegalArgumentException.class,
                () -> Devolucao.reconstituir(UUID.randomUUID(), emprestimo.getId(), Instant.now(), 9, null));
    }

    private Emprestimo emprestimo() {
        var proprietario = new UsuarioInfo(usuario);
        var tomador = new UsuarioInfo(Usuario.create("João Souza", "joao", "joao@example.com",
                "67996122802", LocalDate.of(1992, 5, 10)));
        var exemplar = Exemplar.create(new JogoInfo(jogo), proprietario, EstadoConservacao.BOM, null);
        var inicio = Instant.now();
        return Emprestimo.criar(proprietario, tomador, inicio, inicio.plus(7, ChronoUnit.DAYS),
                List.of(ItemEmprestimo.criar(exemplar)));
    }
}
