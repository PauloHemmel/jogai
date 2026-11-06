package br.ufms.jogai.domain.emprestimo;

import br.ufms.jogai.domain.shared.Entidade;

import java.io.Serializable;
import java.util.UUID;

public class Devolucao extends Entidade<UUID> {

    private Emprestimo emprestimo;
    private Integer avaliacaoDoProprietario;
    private Integer avaliacaoDoTomador;

    protected Devolucao(UUID uuid) {
        super(uuid);
    }
}
