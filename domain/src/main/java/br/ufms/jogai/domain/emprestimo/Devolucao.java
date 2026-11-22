package br.ufms.jogai.domain.emprestimo;

import br.ufms.jogai.domain.shared.Entidade;
import br.ufms.jogai.domain.usuario.AvaliacaoMediaUsuario;
import br.ufms.jogai.domain.util.Validar;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Registro da devolução de um empréstimo e das avaliações trocadas entre os participantes.
 * As avaliações são opcionais e, quando informadas, devem estar entre 0 e 5.
 */
public final class Devolucao extends Entidade<UUID> {

    private final UUID emprestimoId;
    private final Instant dataDevolucao;
    /** Nota dada pelo proprietário ao tomador. */
    private Integer avaliacaoDoProprietario;
    /** Nota dada pelo tomador ao proprietário. */
    private Integer avaliacaoDoTomador;

    private Devolucao(UUID id, UUID emprestimoId, Instant dataDevolucao,
                      Integer avaliacaoDoProprietario, Integer avaliacaoDoTomador) {
        super(Validar.uuid(id));
        this.emprestimoId = Validar.notNull(emprestimoId, "O empréstimo da devolução é obrigatório");
        this.dataDevolucao = Objects.requireNonNull(dataDevolucao, "A data da devolução é obrigatória");
        this.avaliacaoDoProprietario = validarNota(avaliacaoDoProprietario);
        this.avaliacaoDoTomador = validarNota(avaliacaoDoTomador);
    }

    /** Registra a devolução de um empréstimo; a data não pode anteceder o início do empréstimo. */
    public static Devolucao criar(Emprestimo emprestimo, Instant dataDevolucao) {
        Objects.requireNonNull(emprestimo, "O empréstimo da devolução é obrigatório");
        Objects.requireNonNull(dataDevolucao, "A data da devolução é obrigatória");
        if (dataDevolucao.isBefore(emprestimo.getDataEmprestimo())) {
            throw new IllegalArgumentException("A devolução não pode anteceder o início do empréstimo");
        }
        return new Devolucao(UUID.randomUUID(), emprestimo.getId(), dataDevolucao, null, null);
    }

    /** Reconstrói uma devolução persistida aplicando as mesmas validações. */
    public static Devolucao reconstituir(UUID id, UUID emprestimoId, Instant dataDevolucao,
                                         Integer avaliacaoDoProprietario, Integer avaliacaoDoTomador) {
        return new Devolucao(id, emprestimoId, dataDevolucao, avaliacaoDoProprietario, avaliacaoDoTomador);
    }

    public void avaliarPeloProprietario(int nota) {
        this.avaliacaoDoProprietario = validarNota(nota);
    }

    public void avaliarPeloTomador(int nota) {
        this.avaliacaoDoTomador = validarNota(nota);
    }

    public UUID getEmprestimoId() {
        return emprestimoId;
    }

    public Instant getDataDevolucao() {
        return dataDevolucao;
    }

    /** @return nota dada pelo proprietário, ou {@code null} se ainda não avaliou */
    public Integer getAvaliacaoDoProprietario() {
        return avaliacaoDoProprietario;
    }

    /** @return nota dada pelo tomador, ou {@code null} se ainda não avaliou */
    public Integer getAvaliacaoDoTomador() {
        return avaliacaoDoTomador;
    }

    private static Integer validarNota(Integer nota) {
        if (nota != null) AvaliacaoMediaUsuario.validarNotaAvaliacao(nota);
        return nota;
    }
}
