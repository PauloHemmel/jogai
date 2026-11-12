package br.ufms.jogai.domain.jogo;

import br.ufms.jogai.domain.shared.Page;
import br.ufms.jogai.domain.shared.Repository;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface CategoriaRepository extends Repository<CategoriaJogo, String> {

    /**
     * Busca uma categoria pelo nome completo, normalizado pelas regras do domínio.
     * Não realiza busca por prefixo nem ignora diferenças entre maiúsculas e minúsculas.
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
    default CompletableFuture<Page<CategoriaJogo, String>> listarPorNome(String nome, int tamanhoPagina) {
        return listarPorNome(nome, tamanhoPagina, null);
    }

    /**
     * Lista categorias por nome em páginas, continuando após o identificador informado.
     */
    CompletableFuture<Page<CategoriaJogo, String>> listarPorNome(String nome, int tamanhoPagina, String aposTag);
}
