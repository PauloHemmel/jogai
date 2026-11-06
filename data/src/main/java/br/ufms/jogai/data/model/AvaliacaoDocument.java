package br.ufms.jogai.data.model;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.annotation.DocumentId;

public record AvaliacaoDocument(
        @DocumentId String id,
        DocumentReference usuarioId,
        float avaliacao,
        String comentario
) {
}
