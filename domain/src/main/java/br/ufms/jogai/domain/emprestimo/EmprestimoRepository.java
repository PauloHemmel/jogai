package br.ufms.jogai.domain.emprestimo;

import br.ufms.jogai.domain.shared.Page;
import br.ufms.jogai.domain.shared.Repository;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Persistência de empréstimos e consultas paginadas pelos identificadores dos participantes. */
public interface EmprestimoRepository extends Repository<Emprestimo, UUID> {

    /** Lista a primeira página dos empréstimos do proprietário, com tamanho padrão. */
    default CompletableFuture<Page<Emprestimo, UUID>> listarPorProprietario(UUID proprietarioId) {
        return listarPorProprietario(proprietarioId, DEFAULT_PAGE_SIZE);
    }

    /** Lista a primeira página dos empréstimos do proprietário. */
    default CompletableFuture<Page<Emprestimo, UUID>> listarPorProprietario(UUID proprietarioId, int tamanhoPagina) {
        return listarPorProprietario(proprietarioId, tamanhoPagina, null);
    }

    /**
     * Lista empréstimos em ordem de ID, reutilizando o cursor somente com o mesmo proprietário.
     *
     * @param proprietarioId identificador de quem empresta
     * @param tamanhoPagina quantidade de registros por página, de 1 a 200
     * @param aposId último ID da página anterior, ou {@code null} para iniciar
     * @return página com os empréstimos e cursor opcional para continuação
     */
    CompletableFuture<Page<Emprestimo, UUID>> listarPorProprietario(
            UUID proprietarioId, int tamanhoPagina, UUID aposId);

    /** Lista a primeira página dos empréstimos do tomador, com tamanho padrão. */
    default CompletableFuture<Page<Emprestimo, UUID>> listarPorTomador(UUID tomadorId) {
        return listarPorTomador(tomadorId, DEFAULT_PAGE_SIZE);
    }

    /** Lista a primeira página dos empréstimos do tomador. */
    default CompletableFuture<Page<Emprestimo, UUID>> listarPorTomador(UUID tomadorId, int tamanhoPagina) {
        return listarPorTomador(tomadorId, tamanhoPagina, null);
    }

    /**
     * Lista empréstimos em ordem de ID, reutilizando o cursor somente com o mesmo tomador.
     *
     * @param tomadorId identificador de quem recebe os exemplares
     * @param tamanhoPagina quantidade de registros por página, de 1 a 200
     * @param aposId último ID da página anterior, ou {@code null} para iniciar
     * @return página com os empréstimos e cursor opcional para continuação
     */
    CompletableFuture<Page<Emprestimo, UUID>> listarPorTomador(UUID tomadorId, int tamanhoPagina, UUID aposId);
}
