package br.ufms.jogai.domain.jogo;

import br.ufms.jogai.domain.shared.Entidade;
import br.ufms.jogai.domain.util.Validar;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Objects;


public class CategoriaJogo extends Entidade<String> {
    public static final List<CategoriaJogo> CATEGORIAS = List.of(
            CategoriaJogo.create("Ação", "Jogos com decisões rápidas, conflito ou interação direta."),
            CategoriaJogo.create("Aventura", "Jogos focados em exploração e descoberta."),
            CategoriaJogo.create("Cartas", "Jogos em que as cartas são o principal componente."),
            CategoriaJogo.create("Cooperativo", "Jogadores trabalham juntos para vencer o jogo."),
            CategoriaJogo.create("Dedução", "Jogos de investigação, lógica e dedução."),
            CategoriaJogo.create("Estratégia", "Jogos que valorizam planejamento e tomada de decisões."),
            CategoriaJogo.create("Familiar", "Jogos acessíveis para diferentes idades e experiências."),
            CategoriaJogo.create("Festa", "Jogos leves para grupos e encontros sociais."),
            CategoriaJogo.create("Fantasia", "Jogos ambientados em mundos fantásticos."),
            CategoriaJogo.create("Guerra", "Jogos de conflito e confronto entre exércitos ou facções."),
            CategoriaJogo.create("Infantil", "Jogos desenvolvidos para crianças."),
            CategoriaJogo.create("Miniaturas", "Jogos que usam miniaturas como elemento central."),
            CategoriaJogo.create("Temático", "Jogos guiados por ambientação e narrativa."),
            CategoriaJogo.create("Território", "Jogos de disputa por áreas e controle de mapa."),
            CategoriaJogo.create("Economia", "Jogos de negociação, comércio e gestão de recursos."),
            CategoriaJogo.create("Quebra-cabeça", "Jogos de desafios espaciais, padrões ou resolução de problemas.")
    );

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
