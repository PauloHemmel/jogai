package br.ufms.jogai.domain.jogo;

import br.ufms.jogai.domain.util.Validar;

import java.util.Set;
import java.util.UUID;

public record JogoInfo(UUID id, String nome, String editora, Set<CategoriaJogo> categorias) {

    public JogoInfo {
        id = Validar.notNull(id, "A referência ao jogo deve conter um identificador");
    }

    public JogoInfo(Jogo jogo) {
        this(jogo.getId(), jogo.getNome(), jogo.getEditora(), jogo.getCategorias());
    }
}
