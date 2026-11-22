package br.ufms.jogai.domain.emprestimo;

import br.ufms.jogai.domain.shared.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Persistência das devoluções; cada empréstimo possui no máximo uma devolução. */
public interface DevolucaoRepository extends Repository<Devolucao, UUID> {

    CompletableFuture<Optional<Devolucao>> buscarPorEmprestimo(UUID emprestimoId);
}
