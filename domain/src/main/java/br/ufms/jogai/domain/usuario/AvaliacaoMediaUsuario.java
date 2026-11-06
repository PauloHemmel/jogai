package br.ufms.jogai.domain.usuario;

/**
 * Mantém as quantidades e médias das avaliações recebidas por um usuário
 * nos papéis de comodante e comodatário.
 */
public class AvaliacaoMediaUsuario {

    private long qtdAvaliacoesComodante;
    private double mediaAvaliacaoComodante;
    private long qtdAvaliacoesComodatario;
    private double mediaAvaliacaoComodatario;

    /**
     * Cria o estado das avaliações com os valores informados.
     */
    private AvaliacaoMediaUsuario(
            long qtdAvaliacoesComodante, double mediaAvaliacaoComodante,
            long qtdAvaliacoesComodatario, double mediaAvaliacaoComodatario
    ) {
        this.qtdAvaliacoesComodante = qtdAvaliacoesComodante;
        this.mediaAvaliacaoComodante = mediaAvaliacaoComodante;
        this.qtdAvaliacoesComodatario = qtdAvaliacoesComodatario;
        this.mediaAvaliacaoComodatario = mediaAvaliacaoComodatario;
    }

    /**
     * Valida uma nota de avaliação, que deve estar entre zero e cinco.
     */
    public static void validarNotaAvaliacao(int nota) {
        if (nota < 0 || nota > 5) {
            throw new IllegalArgumentException("A nota deve estar entre 0 e 5");
        }
    }

    /**
     * Registra uma avaliação recebida no papel de comodante e atualiza a média.
     */
    public void avaliarComoComodante(int nota) {
        validarNotaAvaliacao(nota);
        mediaAvaliacaoComodante = (mediaAvaliacaoComodante * qtdAvaliacoesComodante + nota) /
                ++qtdAvaliacoesComodante;
    }

    /**
     * Registra uma avaliação recebida no papel de comodatário e atualiza a média.
     */
    public void avaliarComoComodatario(int nota) {
        validarNotaAvaliacao(nota);
        mediaAvaliacaoComodatario = (mediaAvaliacaoComodatario * qtdAvaliacoesComodatario + nota) /
                ++qtdAvaliacoesComodatario;
    }

    /**
     * @return quantidade de avaliações recebidas como comodante
     */
    public long getQuantidadeAvaliacoesComodante() {
        return qtdAvaliacoesComodante;
    }

    /**
     * @return média das avaliações recebidas como comodante
     */
    public double getMediaAvaliacaoComodante() {
        return mediaAvaliacaoComodante;
    }

    /**
     * @return quantidade de avaliações recebidas como comodatário
     */
    public long getQuantidadeAvaliacoesComodatario() {
        return qtdAvaliacoesComodatario;
    }

    /**
     * @return média das avaliações recebidas como comodatário
     */
    public double getMediaAvaliacaoComodatario() {
        return mediaAvaliacaoComodatario;
    }

    /**
     * @return estado inicial, sem avaliações registradas
     */
    public static AvaliacaoMediaUsuario create() {
        return new AvaliacaoMediaUsuario(
                0, 0.0,
                0, 0.0
        );
    }

    /**
     * Reconstrói as estatísticas persistidas do usuário.
     *
     * @param qtdAvaliacoesComodante    quantidade de avaliações como comodante
     * @param mediaAvaliacaoComodante   média como comodante
     * @param qtdAvaliacoesComodatario  quantidade de avaliações como comodatário
     * @param mediaAvaliacaoComodatario média como comodatário
     * @return estatísticas reconstruídas
     */
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
