package br.ufms.jogai.data.repository;

import br.ufms.jogai.domain.emprestimo.Devolucao;
import br.ufms.jogai.domain.reserva.Reserva;
import br.ufms.jogai.domain.reserva.StatusReserva;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "FIRESTORE_EMULATOR_HOST", matches = ".+")
class ReservaDevolucaoFirestoreMapRepositoryTest {

    private Firestore banco;
    private ReservaFirestoreMapRepository reservas;
    private DevolucaoFirestoreMapRepository devolucoes;

    @BeforeEach
    void configurar() {
        banco = FirestoreOptions.newBuilder()
                .setProjectId("demo-jogai-reservas-" + UUID.randomUUID().toString().substring(0, 8))
                .setEmulatorHost(System.getenv("FIRESTORE_EMULATOR_HOST"))
                .build().getService();
        reservas = new ReservaFirestoreMapRepository(banco);
        devolucoes = new DevolucaoFirestoreMapRepository(banco);
    }

    @AfterEach
    void fechar() throws Exception {
        if (banco != null) banco.close();
    }

    @Test
    void salvaConsultaEAtualizaReservas() throws Exception {
        UUID usuario = UUID.randomUUID();
        UUID jogo = UUID.randomUUID();
        var ativa = reserva(usuario, jogo, StatusReserva.ATIVA);
        var cancelada = reserva(usuario, jogo, StatusReserva.CANCELADA);
        var deOutroUsuario = reserva(UUID.randomUUID(), jogo, StatusReserva.ATIVA);
        for (var reserva : new Reserva[]{ativa, cancelada, deOutroUsuario}) aguardar(reservas.create(reserva));

        var salva = aguardar(reservas.get(ativa.getId())).orElseThrow();
        assertEquals(usuario, salva.getUsuarioId());
        assertEquals(jogo, salva.getJogoId());
        assertEquals(StatusReserva.ATIVA, salva.getStatus());
        assertEquals(2, aguardar(reservas.listarPorUsuario(usuario)).items().size());
        assertEquals(2, aguardar(reservas.listarAtivasPorJogo(jogo)).items().size());
        assertTrue(aguardar(reservas.existeAtivaPorUsuarioEJogo(usuario, jogo)));

        salva.atender();
        aguardar(reservas.save(salva));
        assertFalse(aguardar(reservas.existeAtivaPorUsuarioEJogo(usuario, jogo)));
        assertEquals(1, aguardar(reservas.listarAtivasPorJogo(jogo)).items().size());

        aguardar(reservas.delete(cancelada.getId()));
        assertTrue(aguardar(reservas.get(cancelada.getId())).isEmpty());
    }

    @Test
    void salvaConsultaEAvaliaDevolucoes() throws Exception {
        UUID emprestimo = UUID.randomUUID();
        var data = Instant.now().truncatedTo(ChronoUnit.MICROS);
        var devolucao = Devolucao.reconstituir(UUID.randomUUID(), emprestimo, data, null, null);
        aguardar(devolucoes.create(devolucao));

        var salva = aguardar(devolucoes.buscarPorEmprestimo(emprestimo)).orElseThrow();
        assertEquals(devolucao.getId(), salva.getId());
        assertEquals(data, salva.getDataDevolucao());
        assertNull(salva.getAvaliacaoDoProprietario());

        salva.avaliarPeloProprietario(5);
        salva.avaliarPeloTomador(3);
        aguardar(devolucoes.save(salva));
        var avaliada = aguardar(devolucoes.get(devolucao.getId())).orElseThrow();
        assertEquals(5, avaliada.getAvaliacaoDoProprietario());
        assertEquals(3, avaliada.getAvaliacaoDoTomador());
        assertTrue(aguardar(devolucoes.buscarPorEmprestimo(UUID.randomUUID())).isEmpty());
    }

    private static Reserva reserva(UUID usuario, UUID jogo, StatusReserva status) {
        return Reserva.reconstituir(UUID.randomUUID(), usuario, jogo, Instant.now(), status);
    }

    private static <T> T aguardar(CompletableFuture<T> future) throws Exception {
        return future.get(15, TimeUnit.SECONDS);
    }
}
