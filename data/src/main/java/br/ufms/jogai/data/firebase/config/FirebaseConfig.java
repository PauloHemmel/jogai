package br.ufms.jogai.data.firebase.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.NoCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Compartilha o cliente Firebase mantendo credenciais fora do código e do JAR.
 */
public final class FirebaseConfig {
    private static final Path LOCAL_CREDENTIALS = Path.of("config", "firebase", "serviceAccountKey.json");

    private FirebaseConfig() {
    }

    private static final class Holder {
        private static final Firestore FIRESTORE = createFirestore(System.getenv(), Path.of(""));
    }

    static Firestore createFirestore(Map<String, String> environment, Path workingDirectory) {
        String projectId = configuredProjectId(environment);
        String emulator = nonBlank(environment.get("FIRESTORE_EMULATOR_HOST"));
        if (emulator != null) {
            return FirestoreOptions.newBuilder()
                    .setProjectId(projectId == null ? "demo-jogai" : projectId)
                    .setEmulatorHost(emulator)
                    .setCredentials(NoCredentials.getInstance())
                    .build().getService();
        }

        try {
            GoogleCredentials credentials = loadCredentials(environment, workingDirectory);
            if (projectId == null && credentials instanceof ServiceAccountCredentials account) {
                projectId = account.getProjectId();
            }
            if (projectId == null) {
                throw new IllegalStateException("Defina GOOGLE_CLOUD_PROJECT com o ID do seu projeto Firebase.");
            }
            FirebaseOptions options = FirebaseOptions.builder()
                    .setProjectId(projectId)
                    .setCredentials(credentials)
                    .build();
            FirebaseApp app = FirebaseApp.initializeApp(options, "jogai");
            return FirestoreClient.getFirestore(app);
        } catch (IOException error) {
            throw new IllegalStateException("Não foi possível carregar a credencial do Firebase. "
                    + "Defina GOOGLE_APPLICATION_CREDENTIALS ou use config/firebase/serviceAccountKey.json.", error);
        }
    }

    static GoogleCredentials loadCredentials(Map<String, String> environment, Path workingDirectory)
            throws IOException {
        Path credentialsFile = credentialsFile(environment, workingDirectory);
        if (credentialsFile == null) {
            return GoogleCredentials.getApplicationDefault();
        }
        try (var input = Files.newInputStream(credentialsFile)) {
            return GoogleCredentials.fromStream(input);
        }
    }

    static Path credentialsFile(Map<String, String> environment, Path workingDirectory) throws IOException {
        Path directory = workingDirectory.toAbsolutePath().normalize();
        String configured = nonBlank(environment.get("GOOGLE_APPLICATION_CREDENTIALS"));
        if (configured != null) {
            Path path = directory.resolve(configured).normalize();
            if (!Files.isRegularFile(path)) {
                throw new IOException("O arquivo indicado em GOOGLE_APPLICATION_CREDENTIALS não existe.");
            }
            return path;
        }

        // Permite execução pela raiz do projeto ou pelo módulo data.
        Path local = directory.resolve(LOCAL_CREDENTIALS);
        if (Files.isRegularFile(local)) return local;
        Path parent = directory.getParent();
        if (parent != null) {
            local = parent.resolve(LOCAL_CREDENTIALS);
            if (Files.isRegularFile(local)) return local;
        }
        return null;
    }

    static String configuredProjectId(Map<String, String> environment) {
        for (String name : new String[]{"GOOGLE_CLOUD_PROJECT", "GCLOUD_PROJECT"}) {
            String value = nonBlank(environment.get(name));
            if (value != null) return value;
        }
        return null;
    }

    private static String nonBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** O chamador encerra o cliente compartilhado ao finalizar a aplicação. */
    public static Firestore getFirestore() {
        return Holder.FIRESTORE;
    }
}
