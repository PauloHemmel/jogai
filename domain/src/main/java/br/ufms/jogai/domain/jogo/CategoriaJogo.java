package br.ufms.jogai.domain.jogo;

import br.ufms.jogai.domain.shared.Entidade;
import br.ufms.jogai.domain.util.Validar;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;


public class CategoriaJogo extends Entidade<String> {

    private String nome;
    private String descricao;

    private CategoriaJogo(String tag, String nome, String descricao) {
        super(tag);

        setNome(nome);
        setDescricao(descricao);
    }

    public String getNome() {
        return nome;
    }

    private void setNome(String nome) {
        this.nome = Validar.nomeCategoriaJogo(nome);
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao != null ? descricao.strip() : null;
    }

    public String getTag() {
        return getId();
    }

    private static String gerarTag(String nomeCategoria) {
        Objects.requireNonNull(nomeCategoria,
                "A tag da categoria não pode ser gerada a partir de um nome nulo");

        return Normalizer.normalize(nomeCategoria, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_|_$", "");
    }

    public static CategoriaJogo create(String nome, String descricao) {
        return new CategoriaJogo(gerarTag(nome), nome, descricao);
    }

    public static CategoriaJogo reconstitute(String tag, String nome, String descricao) {
        return new CategoriaJogo(tag, nome, descricao);
    }
}
