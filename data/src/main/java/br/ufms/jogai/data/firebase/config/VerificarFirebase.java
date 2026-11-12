package br.ufms.jogai.data.firebase.config;

import java.util.concurrent.TimeUnit;

/** Confere acesso ao Firestore sem criar, alterar ou excluir documentos. */
public final class VerificarFirebase {

    private VerificarFirebase() {
    }

    public static void main(String[] args) throws Exception {
        try (var firestore = FirebaseConfig.getFirestore()) {
            System.out.println("Projeto Firebase: " + firestore.getOptions().getProjectId());
            firestore.collection("categoriasJogo").limit(1).select(new String[0]).get().get(30, TimeUnit.SECONDS);
            System.out.println("Conexão com o Cloud Firestore confirmada.");
            System.out.println("Nenhum documento foi alterado.");
        }
    }
}
