package br.ufms.jogai.domain.jogo;

import br.ufms.jogai.domain.shared.Page;
import br.ufms.jogai.domain.shared.Repository;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ExemplarRepository extends Repository<Exemplar, UUID> {

    default CompletableFuture<Page<Exemplar, UUID>> listarPorJogo(UUID jogoId) {
        return listarPorJogo(jogoId, DEFAULT_PAGE_SIZE, null);
    }

    CompletableFuture<Page<Exemplar, UUID>> listarPorJogo(UUID jogoId, int pageSize, UUID afterId);

    default CompletableFuture<Page<Exemplar, UUID>> listarPorProprietario(UUID proprietarioId) {
        return listarPorProprietario(proprietarioId, DEFAULT_PAGE_SIZE, null);
    }

    CompletableFuture<Page<Exemplar, UUID>> listarPorProprietario(UUID proprietarioId, int pageSize, UUID afterId);

    default CompletableFuture<Page<Exemplar, UUID>> listarPorEstadoConservacao(EstadoConservacao estado) {
        return listarPorEstadoConservacao(estado, DEFAULT_PAGE_SIZE, null);
    }

    CompletableFuture<Page<Exemplar, UUID>> listarPorEstadoConservacao(
            EstadoConservacao estado, int pageSize, UUID afterId);
}
