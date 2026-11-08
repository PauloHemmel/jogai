package br.ufms.jogai.data;

import br.ufms.jogai.data.repository.CategoriaFirestoreMapRepository;
import br.ufms.jogai.domain.jogo.CategoriaJogo;
import br.ufms.jogai.domain.jogo.CategoriaRepository;

import java.util.concurrent.ExecutionException;

/**
 * Popula o repositório com categorias comuns de jogos de tabuleiro.
 */
public class DefaultCategoriasJogo {

    private final CategoriaRepository repository;

    public DefaultCategoriasJogo(CategoriaRepository repository) {
        this.repository = repository;
    }

    /**
     * Insere somente as categorias ausentes, podendo ser executado novamente com segurança.
     */
    public void executar() throws ExecutionException, InterruptedException {
        for (CategoriaJogo categoria : CategoriaJogo.CATEGORIAS) {
            if (repository.get(categoria.getTag()).get().isEmpty()) {
                repository.create(categoria).get();
            }
        }
    }

    static void main(String[] args) throws Exception {
        new DefaultCategoriasJogo(new CategoriaFirestoreMapRepository()).executar();
        System.out.println("Categorias padrão inseridas ou já existentes.");
    }
}
