package br.ufms.jogai.domain.usuario;

/**
 * Sinaliza que já existe um usuário com valor duplicado em determinado campo.
 */
public class UsuarioDuplicadoException extends RuntimeException {

    private final String campo;

    /**
     * Cria a exceção para o campo duplicado informado.
     */
    public UsuarioDuplicadoException(String campo) {
        super("Já existe um usuário com este " + campo + ".");
        this.campo = campo;
    }

    /**
     * @return nome do campo cujo valor já está cadastrado
     */
    public String getCampo() {
        return campo;
    }
}
