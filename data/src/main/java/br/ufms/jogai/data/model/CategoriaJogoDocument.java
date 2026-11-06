package br.ufms.jogai.data.model;

import br.ufms.jogai.data.model.shared.EntityDocument;
import br.ufms.jogai.data.model.shared.Unique;
import br.ufms.jogai.domain.jogo.CategoriaJogo;
import com.google.cloud.firestore.annotation.DocumentId;

public record CategoriaJogoDocument(
        @DocumentId String tag,
        @Unique String nome,
        String descricao
) implements EntityDocument<CategoriaJogo> {

    public CategoriaJogoDocument(CategoriaJogo categoria) {
        this(categoria.getTag(), categoria.getNome(), categoria.getDescricao());
    }

    @Override
    public CategoriaJogo toEntity() {
        return CategoriaJogo.reconstitute(tag, nome, descricao);
    }
}
