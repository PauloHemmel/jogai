package br.ufms.jogai.domain.AI.reserva;

import br.ufms.jogai.domain.AI.jogo.ExemplarId;
import br.ufms.jogai.domain.AI.jogo.Exemplar;
import br.ufms.jogai.domain.AI.jogo.Jogo;
import br.ufms.jogai.domain.AI.jogo.JogoId;
import br.ufms.jogai.domain.AI.jogo.StatusExemplar;
import br.ufms.jogai.domain.AI.shared.Entidade;
import br.ufms.jogai.domain.AI.shared.Texto;
import br.ufms.jogai.domain.AI.usuario.UsuarioId;
import br.ufms.jogai.domain.AI.usuario.Usuario;
import br.ufms.jogai.domain.AI.usuario.StatusUsuario;

import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * Aggregate root que representa a intenção de um usuário de obter um jogo.
 */
public final class Reserva extends Entidade<ReservaId> {

    private final UsuarioId usuarioId;
    private final JogoId jogoId;
    private final Instant dataReserva;
    private final StatusReserva status;
    private final Instant dataAtendimento;
    private final Instant dataCancelamento;
    private final ExemplarId exemplarAtendidoId;
    private final String observacoes;

    private Reserva(ReservaId id, UsuarioId usuarioId, JogoId jogoId, Instant dataReserva,
                   StatusReserva status, Instant dataAtendimento, Instant dataCancelamento,
                   ExemplarId exemplarAtendidoId, String observacoes) {
        super(id);
        this.usuarioId = Objects.requireNonNull(usuarioId, "usuarioId não pode ser nulo");
        this.jogoId = Objects.requireNonNull(jogoId, "jogoId não pode ser nulo");
        this.dataReserva = Objects.requireNonNull(dataReserva, "dataReserva não pode ser nula");
        this.status = Objects.requireNonNull(status, "status não pode ser nulo");
        boolean atendida = status == StatusReserva.ATENDIDA;
        if (atendida != (dataAtendimento != null && exemplarAtendidoId != null)
                || !atendida && dataAtendimento != null) {
            throw new IllegalArgumentException("reservas atendidas exigem data e exemplar de atendimento");
        }
        if ((status == StatusReserva.CANCELADA) == (dataCancelamento == null)) {
            throw new IllegalArgumentException("somente reservas canceladas possuem data de cancelamento");
        }
        if (dataAtendimento != null && dataAtendimento.isBefore(dataReserva)
                || dataCancelamento != null && dataCancelamento.isBefore(dataReserva)) {
            throw new IllegalArgumentException("a transição não pode anteceder a reserva");
        }
        if (status != StatusReserva.ATENDIDA && exemplarAtendidoId != null) {
            throw new IllegalArgumentException("somente reservas atendidas podem referenciar exemplar");
        }

        this.dataAtendimento = dataAtendimento;
        this.dataCancelamento = dataCancelamento;
        this.exemplarAtendidoId = exemplarAtendidoId;
        this.observacoes = Texto.opcional(observacoes).orElse(null);
    }

    public static Reserva reconstitute(ReservaId id, UsuarioId usuarioId, JogoId jogoId, Instant dataReserva,
                                       StatusReserva status, Instant dataAtendimento, Instant dataCancelamento,
                                       ExemplarId exemplarAtendidoId, String observacoes) {
        return new Reserva(id, usuarioId, jogoId, dataReserva, status, dataAtendimento,
                dataCancelamento, exemplarAtendidoId, observacoes);
    }

    public static Reserva criar(ReservaId id, Usuario usuario, Jogo jogo, Instant instante,
                                Collection<Exemplar> exemplares, Collection<Reserva> reservasAtivas,
                                String observacoes) {

        Objects.requireNonNull(usuario, "usuario não pode ser nulo");
        Objects.requireNonNull(jogo, "jogo não pode ser nulo");
        Objects.requireNonNull(instante, "instante não pode ser nulo");
        Objects.requireNonNull(exemplares, "exemplares não pode ser nulo");
        Objects.requireNonNull(reservasAtivas, "reservasAtivas não pode ser nulo");

        if (usuario.status() == StatusUsuario.INATIVO) {
            throw new IllegalStateException("usuário inativo não pode criar reservas");
        }

        if (!jogo.ativo()) throw new IllegalStateException("jogos inativos não podem ser reservados");
        boolean existeDisponivel = exemplares.stream().anyMatch(exemplar -> exemplar.jogoId().equals(jogo.id())
                && exemplar.status() == StatusExemplar.DISPONIVEL);
        if (existeDisponivel) throw new IllegalStateException("o jogo possui exemplar disponível");
        boolean duplicada = reservasAtivas.stream().anyMatch(reserva ->
                reserva.status() == StatusReserva.ATIVA
                        && reserva.usuarioId().equals(usuario.id()) && reserva.jogoId().equals(jogo.id()));
        if (duplicada) throw new IllegalStateException("já existe reserva ativa deste usuário para o jogo");
        return new Reserva(id, usuario.id(), jogo.id(), instante, StatusReserva.ATIVA,
                null, null, null, observacoes);
    }

    public UsuarioId usuarioId() {
        return usuarioId;
    }

    public JogoId jogoId() {
        return jogoId;
    }

    public Instant dataReserva() {
        return dataReserva;
    }

    public StatusReserva status() {
        return status;
    }

    public Optional<Instant> dataAtendimento() {
        return Optional.ofNullable(dataAtendimento);
    }

    public Optional<Instant> dataCancelamento() {
        return Optional.ofNullable(dataCancelamento);
    }

    public Optional<ExemplarId> exemplarAtendidoId() {
        return Optional.ofNullable(exemplarAtendidoId);
    }

    public Optional<String> observacoes() {
        return Optional.ofNullable(observacoes);
    }

    public Reserva atender(Exemplar exemplar, Instant instante) {
        exigirAtiva();
        Objects.requireNonNull(exemplar, "exemplar não pode ser nulo");
        Objects.requireNonNull(instante, "instante não pode ser nulo");
        if (!exemplar.jogoId().equals(jogoId)
                || exemplar.status() != StatusExemplar.RESERVADO
                || exemplar.reservaId().filter(id()::equals).isEmpty()) {
            throw new IllegalArgumentException("exemplar não está reservado para esta reserva");
        }
        if (instante.isBefore(dataReserva)) throw new IllegalArgumentException("atendimento anterior à reserva");
        return new Reserva(id(), usuarioId, jogoId, dataReserva, StatusReserva.ATENDIDA,
                instante, null, exemplar.id(), observacoes);
    }

    public Reserva cancelar(Instant instante) {
        exigirAtiva();
        Objects.requireNonNull(instante, "instante não pode ser nulo");
        if (instante.isBefore(dataReserva)) throw new IllegalArgumentException("cancelamento anterior à reserva");
        return new Reserva(id(), usuarioId, jogoId, dataReserva, StatusReserva.CANCELADA,
                null, instante, null, observacoes);
    }

    public Reserva expirar() {
        exigirAtiva();
        return new Reserva(id(), usuarioId, jogoId, dataReserva, StatusReserva.EXPIRADA,
                null, null, null, observacoes);
    }

    private void exigirAtiva() {
        if (status != StatusReserva.ATIVA)
            throw new IllegalStateException("somente reservas ativas podem mudar de estado");
    }
}
