package br.ufms.jogai.domain.jogo;

import br.ufms.jogai.domain.shared.Page;
import br.ufms.jogai.domain.shared.Repository;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface CategoriaRepository extends Repository<CategoriaJogo, String> {

    /**
     * Busca assincronamente uma categoria pelo nome
     */
    CompletableFuture<Optional<CategoriaJogo>> buscarPorNome(String nome);

    /**
     * Lista a primeira página das categorias que correspondem ao nome.
     */
    default CompletableFuture<Page<CategoriaJogo, String>> listarPorNome(String nome) {
        return listarPorNome(nome, DEFAULT_PAGE_SIZE);
    }

    /**
     * Lista a primeira página das categorias que correspondem ao nome.
     */
    default CompletableFuture<Page<CategoriaJogo, String>> listarPorNome(String nome, int pageSize) {
        return listarPorNome(nome, pageSize, null);
    }

    /**
     * Lista categorias por nome em páginas, continuando após o identificador informado.
     */
    CompletableFuture<Page<CategoriaJogo, String>> listarPorNome(String nome, int pageSize, String afterTag);
}
