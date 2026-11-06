package br.ufms.jogai.domain.usuario;

import br.ufms.jogai.domain.shared.Repository;
import br.ufms.jogai.domain.shared.Page;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Define operações de persistência e consulta específicas para usuários.
 */
public interface UsuarioRepository extends Repository<Usuario, UUID> {

    /**
     * Lista a primeira página de usuários que correspondem ao nome.
     */
    default CompletableFuture<Page<Usuario, UUID>> listarPorNome(String nome) {
        return listarPorNome(nome, DEFAULT_PAGE_SIZE);
    }

    /**
     * Lista a primeira página de usuários que correspondem ao nome.
     */
    default CompletableFuture<Page<Usuario, UUID>> listarPorNome(String nome, int pageSize) {
        return listarPorNome(nome, pageSize, null);
    }

    /**
     * Lista usuários por nome em páginas, continuando após o identificador informado.
     */
    CompletableFuture<Page<Usuario, UUID>> listarPorNome(String nome, int pageSize, UUID afterId);

    /**
     * Busca assincronamente um usuário pelo nome de usuário.
     */
    CompletableFuture<Optional<Usuario>> buscarPorUsuario(String usuario);

    /**
     * Busca assincronamente um usuário pelo endereço de e-mail.
     */
    CompletableFuture<Optional<Usuario>> buscarPorEmail(String email);

    /**
     * Busca assincronamente um usuário pelo telefone.
     */
    CompletableFuture<Optional<Usuario>> buscarPorTelefone(String telefone);

    /**
     * Lista a primeira página de usuários no estado informado.
     */
    default CompletableFuture<Page<Usuario, UUID>> listarPorStatus(StatusUsuario status) {
        return listarPorStatus(status, DEFAULT_PAGE_SIZE);
    }

    /**
     * Lista a primeira página de usuários no estado informado.
     */
    default CompletableFuture<Page<Usuario, UUID>> listarPorStatus(StatusUsuario status, int pageSize) {
        return listarPorStatus(status, pageSize, null);
    }

    /**
     * Lista usuários de um estado em páginas, continuando após o identificador informado.
     */
    CompletableFuture<Page<Usuario, UUID>> listarPorStatus(StatusUsuario status, int pageSize, UUID afterId);
}
