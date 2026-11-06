package br.ufms.jogai.data.model;

import br.ufms.jogai.data.model.shared.EntityDocument;
import br.ufms.jogai.domain.jogo.Exemplar;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.annotation.DocumentId;

public record ExemplarDocument(
        @DocumentId String id,
        DocumentReference jogoId,
        DocumentReference usuarioId,
        String estadoConservacao,
        String status,
        String observacao
) implements EntityDocument<Exemplar> {

    @Override
    public Exemplar toEntity() {
        return null;
    }
}
