package br.ufms.jogai.data.repository;

import br.ufms.jogai.domain.jogo.CategoriaJogo;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Mapeia as categorias presentes nos documentos de jogos e exemplares, sem leituras adicionais. */
final class CategoriasFirestore {

    private CategoriasFirestore() {
    }

    static List<Map<String, Object>> toMaps(Set<CategoriaJogo> categorias) {
        return categorias.stream().sorted(Comparator.comparing(CategoriaJogo::getTag)).map(categoria -> {
            Map<String, Object> dados = new HashMap<>();
            dados.put("tag", categoria.getTag());
            dados.put("nome", categoria.getNome());
            dados.put("descricao", categoria.getDescricao());
            return dados;
        }).toList();
    }

    static Set<CategoriaJogo> fromMaps(Object value) {
        if (!(value instanceof List<?> values)) {
            throw new IllegalArgumentException("As categorias devem ser uma lista de mapas");
        }
        Set<CategoriaJogo> categorias = new HashSet<>();
        for (Object item : values) {
            if (!(item instanceof Map<?, ?> dados)) {
                throw new IllegalArgumentException("Categoria persistida inválida");
            }
            categorias.add(CategoriaJogo.reconstitute(
                    (String) Objects.requireNonNull(dados.get("tag")),
                    (String) Objects.requireNonNull(dados.get("nome")),
                    (String) dados.get("descricao")));
        }
        return categorias;
    }
}
