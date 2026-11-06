package br.ufms.jogai.data.model;

import br.ufms.jogai.data.model.shared.EntityDocument;
import br.ufms.jogai.domain.emprestimo.Emprestimo;
import com.google.cloud.firestore.annotation.DocumentId;

public record EmprestimoDocument(
        @DocumentId String id
) implements EntityDocument<Emprestimo> {

    @Override
    public Emprestimo toEntity() {
        return null;
    }
}
