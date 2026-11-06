package br.ufms.jogai.domain.AI.jogo;

import br.ufms.jogai.domain.AI.shared.Entidade;
import br.ufms.jogai.domain.AI.shared.Texto;

import java.util.Optional;

/**
 * Aggregate root de classificação; jogos guardam sua identidade, não a entidade inteira.
 */
public final class Categoria extends Entidade<CategoriaId> {

    private final String nome;
    private final String descricao;

    private Categoria(CategoriaId id, String nome, String descricao) {
        super(id);
        this.nome = Texto.obrigatorio(nome, "nome");
        this.descricao = Texto.opcional(descricao).orElse(null);
    }

    public static Categoria create(String nome, String descricao) {
        return new Categoria(CategoriaId.novo(), nome, descricao);
    }

    public static Categoria reconstitute(CategoriaId id, String nome, String descricao) {
        return new Categoria(id, nome, descricao);
    }

    public String nome() {
        return nome;
    }

    public Optional<String> descricao() {
        return Optional.ofNullable(descricao);
    }
}
