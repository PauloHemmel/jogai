package br.ufms.jogai.domain.emprestimo;

import br.ufms.jogai.domain.jogo.EstadoConservacao;
import br.ufms.jogai.domain.jogo.Exemplar;
import br.ufms.jogai.domain.shared.Entidade;

import java.time.Instant;
import java.util.UUID;

public class ItemEmprestimo extends Entidade<UUID> {

    private Exemplar exemplar;
    private Instant dataEfetivaDevolucao;

    protected ItemEmprestimo(UUID uuid) {
        super(uuid);
    }

    public void relatarDano(EstadoConservacao novoEstado, String texto) {
        exemplar.setEstadoConservacao(novoEstado);
    }
}
