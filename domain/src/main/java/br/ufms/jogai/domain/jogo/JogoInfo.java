package br.ufms.jogai.domain.jogo;

import br.ufms.jogai.domain.util.Validar;

import java.util.Set;
import java.util.UUID;

public record JogoInfo(UUID id, String nome, String editora, Set<CategoriaJogo> categorias) {

    public JogoInfo {
        id = Validar.uuid(id);
    }

    public JogoInfo(Jogo jogo) {
        this(jogo.getId(), jogo.getNome(), jogo.getEditora(), jogo.getCategorias());
    }
}
