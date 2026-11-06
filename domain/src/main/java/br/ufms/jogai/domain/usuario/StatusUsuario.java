package br.ufms.jogai.domain.usuario;

/**
 * Situações possíveis de um usuário e as suas descrições para apresentação.
 */
public enum StatusUsuario {

    ATIVO("Ativo", "Usuário ativo e apto a realizar empréstimos"),
    BLOQUEADO("Bloqueado", "Usuário bloqueado, impedido de realizar novos empréstimos"),
    INATIVO("Inativo", "Usuário inativo, sem permissão para realizar operações");

    private final String nome;
    private final String descricao;

    /**
     * Define os textos associados ao estado.
     */
    StatusUsuario(String nome, String descricao) {
        this.nome = nome;
        this.descricao = descricao;
    }

    /**
     * @return nome legível do estado
     */
    public String nome() {
        return nome;
    }

    /**
     * @return descrição das permissões e condições do estado
     */
    public String descricao() {
        return descricao;
    }
}
