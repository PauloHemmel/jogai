package br.ufms.jogai.data;

import br.ufms.jogai.data.firebase.config.VerificarFirebase;

/** Ponto de entrada para conferir a conexão com o projeto Firebase configurado. */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        VerificarFirebase.main(args);
    }
}
