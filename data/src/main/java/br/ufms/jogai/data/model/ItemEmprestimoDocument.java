package br.ufms.jogai.data.model;

import br.ufms.jogai.data.model.shared.EntityDocument;
import br.ufms.jogai.domain.emprestimo.ItemEmprestimo;

public record ItemEmprestimoDocument(

) implements EntityDocument<ItemEmprestimo> {

    @Override
    public ItemEmprestimo toEntity() {
        return null;
    }
}
