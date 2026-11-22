package br.ufms.jogai.domain.reserva;

import br.ufms.jogai.domain.jogo.Jogo;
import br.ufms.jogai.domain.shared.Entidade;
import br.ufms.jogai.domain.usuario.StatusUsuario;
import br.ufms.jogai.domain.usuario.Usuario;
import br.ufms.jogai.domain.util.Validar;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Intenção de um usuário de pegar um jogo emprestado. Nasce ativa e pode ser
 * atendida ou cancelada uma única vez.
 */
public final class Reserva extends Entidade<UUID> {

    private final UUID usuarioId;
    private final UUID jogoId;
    private final Instant dataReserva;
    private StatusReserva status;

    private Reserva(UUID id, UUID usuarioId, UUID jogoId, Instant dataReserva, StatusReserva status) {
        super(Validar.uuid(id));
        this.usuarioId = Validar.notNull(usuarioId, "O usuário da reserva é obrigatório");
        this.jogoId = Validar.notNull(jogoId, "O jogo da reserva é obrigatório");
        this.dataReserva = Objects.requireNonNull(dataReserva, "A data da reserva é obrigatória");
        this.status = Objects.requireNonNull(status, "O status da reserva é obrigatório");
    }

    /** Cria uma reserva ativa; somente usuários ativos podem reservar jogos ativos. */
    public static Reserva criar(Usuario usuario, Jogo jogo) {
        Objects.requireNonNull(usuario, "O usuário da reserva é obrigatório");
        Objects.requireNonNull(jogo, "O jogo da reserva é obrigatório");
        if (usuario.getStatus() != StatusUsuario.ATIVO) {
            throw new IllegalArgumentException("Somente usuários ativos podem fazer reservas");
        }
        if (!jogo.isAtivo()) {
            throw new IllegalArgumentException("Não é possível reservar um jogo inativo");
        }
        return new Reserva(UUID.randomUUID(), usuario.getId(), jogo.getId(), Instant.now(), StatusReserva.ATIVA);
    }

    /** Reconstrói uma reserva persistida aplicando as mesmas validações. */
    public static Reserva reconstituir(UUID id, UUID usuarioId, UUID jogoId,
                                       Instant dataReserva, StatusReserva status) {
        return new Reserva(id, usuarioId, jogoId, dataReserva, status);
    }

    public void atender() {
        exigirAtiva();
        status = StatusReserva.ATENDIDA;
    }

    public void cancelar() {
        exigirAtiva();
        status = StatusReserva.CANCELADA;
    }

    public boolean isAtiva() {
        return status == StatusReserva.ATIVA;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public UUID getJogoId() {
        return jogoId;
    }

    public Instant getDataReserva() {
        return dataReserva;
    }

    public StatusReserva getStatus() {
        return status;
    }

    private void exigirAtiva() {
        if (!isAtiva()) {
            throw new IllegalStateException("A reserva já foi " + status.name().toLowerCase());
        }
    }
}
