package br.ufms.jogai.domain.jogo;

import br.ufms.jogai.domain.shared.Page;
import br.ufms.jogai.domain.shared.Repository;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface JogoRepository extends Repository<Jogo, UUID> {


    /**Busca um jogo pelo nome exato. */
    CompletableFuture<Optional<Jogo>> buscarPorNome(String nome);

    /** Lista jogos que pertencem à categoria informada. */
    default CompletableFuture<Page<Jogo, UUID>> listarPorCategoria(CategoriaJogo categoria) {
        return listarPorCategoria(categoria, DEFAULT_PAGE_SIZE, null);
    }

    CompletableFuture<Page<Jogo, UUID>> listarPorCategoria(CategoriaJogo categoria, int pageSize, UUID afterId);


    /** Lista jogos de uma editora. */
    default CompletableFuture<Page<Jogo, UUID>> listarPorEditora(String editora){
        return listarPorEditora(editora, DEFAULT_PAGE_SIZE, null);
    }

    CompletableFuture<Page<Jogo, UUID>> listarPorEditora(String editora, int pageSize, UUID afterId);


    /** Lista apenas os jogos ativos(os inativos não aparecem para empréstimo). */
    default CompletableFuture<Page<Jogo, UUID>> listarAtivos(){
        return listarAtivos(DEFAULT_PAGE_SIZE, null);
    }

    CompletableFuture<Page<Jogo, UUID>> listarAtivos(int pageSize, UUID afterId);

}
