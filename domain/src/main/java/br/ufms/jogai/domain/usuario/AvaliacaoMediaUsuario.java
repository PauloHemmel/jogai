package br.ufms.jogai.domain.usuario;

public class AvaliacaoMediaUsuario {

    private long qtdAvaliacoesComodante;
    private double mediaAvaliacaoComodante;
    private long qtdAvaliacoesComodatario;
    private double mediaAvaliacaoComodatario;

    private AvaliacaoMediaUsuario(
            long qtdAvaliacoesComodante, double mediaAvaliacaoComodante,
            long qtdAvaliacoesComodatario, double mediaAvaliacaoComodatario
    ) {
        this.qtdAvaliacoesComodante = qtdAvaliacoesComodante;
        this.mediaAvaliacaoComodante = mediaAvaliacaoComodante;
        this.qtdAvaliacoesComodatario = qtdAvaliacoesComodatario;
        this.mediaAvaliacaoComodatario = mediaAvaliacaoComodatario;
    }

    public static void validarNotaAvaliacao(int nota) {
        if (nota < 0 || nota > 5) {
            throw new IllegalArgumentException("A nota deve estar entre 0 e 5");
        }
    }

    public void avaliarComoComodante(int nota) {
        validarNotaAvaliacao(nota);
        mediaAvaliacaoComodante = (mediaAvaliacaoComodante * qtdAvaliacoesComodante + nota) /
                ++qtdAvaliacoesComodante;
    }

    public void avaliarComoComodatario(int nota) {
        validarNotaAvaliacao(nota);
        mediaAvaliacaoComodatario = (mediaAvaliacaoComodatario * qtdAvaliacoesComodatario + nota) /
                ++qtdAvaliacoesComodatario;
    }

    public long getQtdAvaliacoesComodante() {
        return qtdAvaliacoesComodante;
    }

    public double getMediaAvaliacaoComodante() {
        return mediaAvaliacaoComodante;
    }

    public long getQtdAvaliacoesComodatario() {
        return qtdAvaliacoesComodatario;
    }

    public double getMediaAvaliacaoComodatario() {
        return mediaAvaliacaoComodatario;
    }

    public static AvaliacaoMediaUsuario create() {
        return new AvaliacaoMediaUsuario(
                0, 0.0,
                0, 0.0
        );
    }

    public static AvaliacaoMediaUsuario reconstitute(
            long qtdAvaliacoesComodante, double mediaAvaliacaoComodante,
            long qtdAvaliacoesComodatario, double mediaAvaliacaoComodatario
    ) {
        return new AvaliacaoMediaUsuario(
                qtdAvaliacoesComodante, mediaAvaliacaoComodante,
                qtdAvaliacoesComodatario, mediaAvaliacaoComodatario
        );
    }
}
