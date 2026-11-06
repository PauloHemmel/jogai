package br.ufms.jogai.domain.AI.jogo;

import br.ufms.jogai.domain.AI.shared.Entidade;
import br.ufms.jogai.domain.AI.shared.Texto;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Aggregate root do catálogo. Exemplares têm ciclo de vida independente e são referenciados por JogoId.
 */
public final class Jogo extends Entidade<JogoId> {

    private final String nome;
    private final String descricao;
    private final int numeroMinimoJogadores;
    private final int numeroMaximoJogadores;
    private final int idadeMinima;
    private final int duracaoMinima;
    private final int duracaoMaxima;
    private final Integer anoLancamento;
    private final String editora;
    private final Instant dataCadastro;
    private final boolean ativo;
    private final Set<CategoriaId> categorias;

    private Jogo(JogoId id, String nome, String descricao, int numeroMinimoJogadores,
                int numeroMaximoJogadores, int idadeMinima, int duracaoMinima, int duracaoMaxima,
                Integer anoLancamento, String editora, Instant dataCadastro, boolean ativo,
                Set<CategoriaId> categorias) {
        super(id);
        this.nome = Texto.obrigatorio(nome, "nome");
        this.descricao = Texto.obrigatorio(descricao, "descrição");
        if (numeroMinimoJogadores < 1 || numeroMaximoJogadores < numeroMinimoJogadores) {
            throw new IllegalArgumentException("intervalo de jogadores inválido");
        }
        if (idadeMinima < 0 || duracaoMinima < 1 || duracaoMaxima < duracaoMinima) {
            throw new IllegalArgumentException("idade ou intervalo de duração inválido");
        }
        if (anoLancamento != null && anoLancamento < 0) {
            throw new IllegalArgumentException("ano de lançamento não pode ser negativo");
        }
        this.numeroMinimoJogadores = numeroMinimoJogadores;
        this.numeroMaximoJogadores = numeroMaximoJogadores;
        this.idadeMinima = idadeMinima;
        this.duracaoMinima = duracaoMinima;
        this.duracaoMaxima = duracaoMaxima;
        this.anoLancamento = anoLancamento;
        this.editora = Texto.opcional(editora).orElse(null);
        this.dataCadastro = Objects.requireNonNull(dataCadastro, "dataCadastro não pode ser nula");
        this.ativo = ativo;
        Objects.requireNonNull(categorias, "categorias não pode ser nulo");
        if (categorias.isEmpty() || categorias.contains(null)) {
            throw new IllegalArgumentException("o jogo deve possuir categorias válidas");
        }
        this.categorias = Set.copyOf(categorias);
    }

    public static Jogo create(String nome, String descricao, int numeroMinimoJogadores,
                              int numeroMaximoJogadores, int idadeMinima, int duracaoMinima,
                              int duracaoMaxima, Integer anoLancamento, String editora,
                              Set<CategoriaId> categorias) {
        return new Jogo(JogoId.novo(), nome, descricao, numeroMinimoJogadores, numeroMaximoJogadores,
                idadeMinima, duracaoMinima, duracaoMaxima, anoLancamento, editora,
                Instant.now(), true, categorias);
    }

    public static Jogo reconstitute(JogoId id, String nome, String descricao, int numeroMinimoJogadores,
                                    int numeroMaximoJogadores, int idadeMinima, int duracaoMinima,
                                    int duracaoMaxima, Integer anoLancamento, String editora,
                                    Instant dataCadastro, boolean ativo, Set<CategoriaId> categorias) {
        return new Jogo(id, nome, descricao, numeroMinimoJogadores, numeroMaximoJogadores,
                idadeMinima, duracaoMinima, duracaoMaxima, anoLancamento, editora,
                dataCadastro, ativo, categorias);
    }

    public String nome() {
        return nome;
    }

    public String descricao() {
        return descricao;
    }

    public int numeroMinimoJogadores() {
        return numeroMinimoJogadores;
    }

    public int numeroMaximoJogadores() {
        return numeroMaximoJogadores;
    }

    public int idadeMinima() {
        return idadeMinima;
    }

    public int duracaoMinima() {
        return duracaoMinima;
    }

    public int duracaoMaxima() {
        return duracaoMaxima;
    }

    public Optional<Integer> anoLancamento() {
        return Optional.ofNullable(anoLancamento);
    }

    public Optional<String> editora() {
        return Optional.ofNullable(editora);
    }

    public Instant dataCadastro() {
        return dataCadastro;
    }

    public boolean ativo() {
        return ativo;
    }

    public Set<CategoriaId> categorias() {
        return categorias;
    }

    public Jogo inativar() {
        return copiar(false);
    }

    public Jogo ativar() {
        return copiar(true);
    }

    private Jogo copiar(boolean novoEstado) {
        return new Jogo(id(), nome, descricao, numeroMinimoJogadores, numeroMaximoJogadores,
                idadeMinima, duracaoMinima, duracaoMaxima, anoLancamento, editora,
                dataCadastro, novoEstado, categorias);
    }
}
