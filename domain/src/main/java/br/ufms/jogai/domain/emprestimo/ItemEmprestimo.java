package br.ufms.jogai.domain.emprestimo;

import br.ufms.jogai.domain.jogo.Exemplar;
import br.ufms.jogai.domain.jogo.ExemplarInfo;
import br.ufms.jogai.domain.shared.Entidade;
import br.ufms.jogai.domain.util.Validar;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Item imutável com os dados do exemplar na inclusão e sua data efetiva de devolução, se houver. */
public final class ItemEmprestimo extends Entidade<UUID> {

    private final ExemplarInfo exemplar;
    private final Instant dataEfetivaDevolucao;

    private ItemEmprestimo(UUID id, ExemplarInfo exemplar, Instant dataEfetivaDevolucao) {
        super(Validar.uuid(id));
        this.exemplar = Objects.requireNonNull(exemplar, "Os dados do exemplar são obrigatórios");
        this.dataEfetivaDevolucao = dataEfetivaDevolucao;
    }

    /** Cria um item pendente, sem manter referência ao exemplar mutável do cadastro. */
    public static ItemEmprestimo criar(Exemplar exemplar) {
        return reconstituir(UUID.randomUUID(), new ExemplarInfo(exemplar), null);
    }

    /** Reconstrói um item com ID e dados históricos preservados; a data de devolução é opcional. */
    public static ItemEmprestimo reconstituir(UUID id, ExemplarInfo exemplar, Instant dataEfetivaDevolucao) {
        return new ItemEmprestimo(id, exemplar, dataEfetivaDevolucao);
    }

    public ExemplarInfo getExemplarInfo() {
        return exemplar;
    }

    public Instant getDataEfetivaDevolucao() {
        return dataEfetivaDevolucao;
    }

    // Somente o empréstimo conhece o início e pode validar a cronologia da devolução.
    ItemEmprestimo comDevolucao(Instant dataDevolucao) {
        return reconstituir(getId(), exemplar, dataDevolucao);
    }
}
