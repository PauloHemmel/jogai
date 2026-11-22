package br.ufms.jogai.domain.reserva;

import br.ufms.jogai.domain.shared.Page;
import br.ufms.jogai.domain.shared.Repository;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ReservaRepository extends Repository<Reserva, UUID> {

    default CompletableFuture<Page<Reserva, UUID>> listarPorUsuario(UUID usuarioId) {
        return listarPorUsuario(usuarioId, DEFAULT_PAGE_SIZE, null);
    }

    CompletableFuture<Page<Reserva, UUID>> listarPorUsuario(UUID usuarioId, int pageSize, UUID afterId);

    default CompletableFuture<Page<Reserva, UUID>> listarAtivasPorJogo(UUID jogoId) {
        return listarAtivasPorJogo(jogoId, DEFAULT_PAGE_SIZE, null);
    }

    CompletableFuture<Page<Reserva, UUID>> listarAtivasPorJogo(UUID jogoId, int pageSize, UUID afterId);

    /** Usado para impedir que o mesmo usuário tenha duas reservas ativas para o mesmo jogo. */
    CompletableFuture<Boolean> existeAtivaPorUsuarioEJogo(UUID usuarioId, UUID jogoId);
}
