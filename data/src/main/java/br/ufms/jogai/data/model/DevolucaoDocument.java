package br.ufms.jogai.data.model;

import br.ufms.jogai.data.model.shared.EntityDocument;
import br.ufms.jogai.domain.emprestimo.Devolucao;
import com.google.cloud.firestore.annotation.DocumentId;

public record DevolucaoDocument(
        @DocumentId String id
) implements EntityDocument<Devolucao> {

    @Override
    public Devolucao toEntity() {
        return null;
    }
}
