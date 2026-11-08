package br.ufms.jogai.data.model;

/** Firestore representation of the user's aggregate ratings. */
public record AvaliacaoUsuarioDocument(
        long qtdAvaliacoesComodante,
        double mediaAvaliacaoComodante,
        long qtdAvaliacoesComodatario,
        double mediaAvaliacaoComodatario
) {
}
