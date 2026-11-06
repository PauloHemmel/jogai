package br.ufms.jogai.data;

import br.ufms.jogai.data.repository.CategoriaFirestoreRepository;
import br.ufms.jogai.domain.jogo.CategoriaJogo;
import br.ufms.jogai.domain.jogo.CategoriaRepository;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Popula o repositório com categorias comuns de jogos de tabuleiro.
 */
public class DefaultCategoriasJogo {

    private static final List<CategoriaJogo> CATEGORIAS = List.of(
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

    private final CategoriaRepository repository;

    public DefaultCategoriasJogo(CategoriaRepository repository) {
        this.repository = repository;
    }

    /**
     * Insere somente as categorias ausentes, podendo ser executado novamente com segurança.
     */
    public void executar() throws ExecutionException, InterruptedException {
        for (CategoriaJogo categoria : CATEGORIAS) {
            if (repository.get(categoria.getTag()).get().isEmpty()) {
                repository.create(categoria).get();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        new DefaultCategoriasJogo(new CategoriaFirestoreRepository()).executar();
        System.out.println("Categorias padrão inseridas ou já existentes.");
    }
}
