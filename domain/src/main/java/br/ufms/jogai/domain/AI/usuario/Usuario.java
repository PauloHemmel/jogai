package br.ufms.jogai.domain.AI.usuario;

import br.ufms.jogai.domain.AI.shared.Entidade;
import br.ufms.jogai.domain.AI.shared.Texto;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Aggregate root do ciclo de vida e das permissões básicas de um usuário.
 */
public final class Usuario extends Entidade<UsuarioId> {

    private final String nome;
    private final Email email;
    private final String telefone;
    private final Instant dataCadastro;
    private final StatusUsuario status;

    private Usuario(UsuarioId id, String nome, Email email, String telefone,
                   Instant dataCadastro, StatusUsuario status) {
        super(id);
        this.nome = Texto.obrigatorio(nome, "nome");
        this.email = Objects.requireNonNull(email, "email não pode ser nulo");
        this.telefone = Texto.opcional(telefone).orElse(null);
        this.dataCadastro = Objects.requireNonNull(dataCadastro, "dataCadastro não pode ser nula");
        this.status = Objects.requireNonNull(status, "status não pode ser nulo");
    }

    public static Usuario create(String nome, Email email, String telefone) {
        return new Usuario(UsuarioId.novo(), nome, email, telefone, Instant.now(), StatusUsuario.ATIVO);
    }

    public static Usuario reconstitute(UsuarioId id, String nome, Email email, String telefone,
                                       Instant dataCadastro, StatusUsuario status) {
        return new Usuario(id, nome, email, telefone, dataCadastro, status);
    }

    public static Usuario cadastrar(UsuarioId id, String nome, Email email, String telefone, Instant instante) {
        return new Usuario(id, nome, email, telefone, instante, StatusUsuario.ATIVO);
    }

    public String nome() {
        return nome;
    }

    public Email email() {
        return email;
    }

    public Optional<String> telefone() {
        return Optional.ofNullable(telefone);
    }

    public Instant dataCadastro() {
        return dataCadastro;
    }

    public StatusUsuario status() {
        return status;
    }

    public boolean podeRealizarEmprestimo() {
        return status == StatusUsuario.ATIVO;
    }

    public Usuario alterarStatus(StatusUsuario novoStatus) {
        return new Usuario(id(), nome, email, telefone, dataCadastro, novoStatus);
    }
}
