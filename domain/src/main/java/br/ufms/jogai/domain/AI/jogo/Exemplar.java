package br.ufms.jogai.domain.AI.jogo;

import br.ufms.jogai.domain.AI.shared.Entidade;
import br.ufms.jogai.domain.AI.shared.Texto;
import br.ufms.jogai.domain.AI.reserva.Reserva;
import br.ufms.jogai.domain.AI.reserva.ReservaId;
import br.ufms.jogai.domain.AI.reserva.StatusReserva;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Aggregate root da unidade física; suas mudanças são validadas por transições explícitas.
 */
public final class Exemplar extends Entidade<ExemplarId> {

    private final String codigo;
    private final JogoId jogoId;
    private final Instant dataCadastro;
    private final EstadoConservacao estadoConservacao;
    private final StatusExemplar status;
    private final ReservaId reservaId;
    private final String observacoes;

    private Exemplar(ExemplarId id, String codigo, JogoId jogoId, Instant dataCadastro,
                    EstadoConservacao estadoConservacao, StatusExemplar status,
                    ReservaId reservaId, String observacoes) {
        super(id);
        this.codigo = Texto.obrigatorio(codigo, "código");
        this.jogoId = Objects.requireNonNull(jogoId, "jogoId não pode ser nulo");
        this.dataCadastro = Objects.requireNonNull(dataCadastro, "dataCadastro não pode ser nula");
        this.estadoConservacao = Objects.requireNonNull(estadoConservacao, "estadoConservacao não pode ser nulo");
        this.status = Objects.requireNonNull(status, "status não pode ser nulo");
        if ((status == StatusExemplar.RESERVADO) == (reservaId == null)) {
            throw new IllegalArgumentException("somente exemplares reservados possuem reserva associada");
        }
        this.reservaId = reservaId;
        this.observacoes = Texto.opcional(observacoes).orElse(null);
    }

    public static Exemplar create(String codigo, JogoId jogoId, EstadoConservacao estadoConservacao,
                                  String observacoes) {
        return new Exemplar(ExemplarId.novo(), codigo, jogoId, Instant.now(), estadoConservacao,
                StatusExemplar.DISPONIVEL, null, observacoes);
    }

    public static Exemplar reconstitute(ExemplarId id, String codigo, JogoId jogoId, Instant dataCadastro,
                                        EstadoConservacao estadoConservacao, StatusExemplar status,
                                        ReservaId reservaId, String observacoes) {
        return new Exemplar(id, codigo, jogoId, dataCadastro, estadoConservacao, status, reservaId, observacoes);
    }

    public String codigo() {
        return codigo;
    }

    public JogoId jogoId() {
        return jogoId;
    }

    public Instant dataCadastro() {
        return dataCadastro;
    }

    public EstadoConservacao estadoConservacao() {
        return estadoConservacao;
    }

    public StatusExemplar status() {
        return status;
    }

    public Optional<ReservaId> reservaId() {
        return Optional.ofNullable(reservaId);
    }

    public Optional<String> observacoes() {
        return Optional.ofNullable(observacoes);
    }

    public boolean disponivelParaEmprestimo() {
        return status == StatusExemplar.DISPONIVEL;
    }

    public Exemplar reservar(ReservaId reservaId) {
        exigir(StatusExemplar.DISPONIVEL, "somente exemplares disponíveis podem ser reservados");
        return copiar(estadoConservacao, StatusExemplar.RESERVADO,
                Objects.requireNonNull(reservaId, "reservaId não pode ser nulo"));
    }

    public Exemplar emprestar() {
        exigir(StatusExemplar.DISPONIVEL, "somente exemplares disponíveis podem ser emprestados");
        return copiar(estadoConservacao, StatusExemplar.EMPRESTADO, null);
    }

    public Exemplar emprestarReservado(ReservaId reservaId) {
        exigir(StatusExemplar.RESERVADO, "somente exemplares reservados podem ser retirados por reserva");
        if (!this.reservaId.equals(Objects.requireNonNull(reservaId, "reservaId não pode ser nulo"))) {
            throw new IllegalStateException("o exemplar está associado a outra reserva");
        }
        return copiar(estadoConservacao, StatusExemplar.EMPRESTADO, null);
    }

    public Exemplar devolver(EstadoConservacao novoEstado, Reserva reservaPendente) {
        exigir(StatusExemplar.EMPRESTADO, "somente exemplares emprestados podem ser devolvidos");
        Objects.requireNonNull(novoEstado, "estadoConservacao não pode ser nulo");
        if (reservaPendente != null && (reservaPendente.status() != StatusReserva.ATIVA
                || !reservaPendente.jogoId().equals(jogoId))) {
            throw new IllegalArgumentException("a reserva pendente deve estar ativa e pertencer ao jogo do exemplar");
        }
        if (novoEstado == EstadoConservacao.DANIFICADO) {
            return copiar(novoEstado, StatusExemplar.MANUTENCAO, null);
        }
        return reservaPendente == null
                ? copiar(novoEstado, StatusExemplar.DISPONIVEL, null)
                : copiar(novoEstado, StatusExemplar.RESERVADO, reservaPendente.id());
    }

    public Exemplar colocarEmManutencao() {
        if (status == StatusExemplar.EMPRESTADO || status == StatusExemplar.RESERVADO) {
            throw new IllegalStateException("exemplares emprestados ou reservados não podem ir para manutenção");
        }
        return copiar(estadoConservacao, StatusExemplar.MANUTENCAO, null);
    }

    public Exemplar liberarDaManutencao() {
        exigir(StatusExemplar.MANUTENCAO, "somente exemplares em manutenção podem ser liberados");
        return copiar(estadoConservacao, StatusExemplar.DISPONIVEL, null);
    }

    public Exemplar inativar() {
        if (status == StatusExemplar.EMPRESTADO || status == StatusExemplar.RESERVADO) {
            throw new IllegalStateException("exemplares emprestados ou reservados não podem ser inativados");
        }
        return copiar(estadoConservacao, StatusExemplar.INDISPONIVEL, null);
    }

    private void exigir(StatusExemplar esperado, String mensagem) {
        if (status != esperado) throw new IllegalStateException(mensagem);
    }

    private Exemplar copiar(EstadoConservacao estado, StatusExemplar novoStatus, ReservaId novaReservaId) {
        return new Exemplar(id(), codigo, jogoId, dataCadastro, estado, novoStatus, novaReservaId, observacoes);
    }
}
