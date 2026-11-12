package br.ufms.jogai.data.firebase.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Não usa chaves reais, a rede ou o projeto Firebase da aplicação.
 */
class FirebaseConfigTest {

    @TempDir
    Path directory;

    @Test
    void encontraCredencialLocalPelaRaizEPeloModulo() throws Exception {
        Path credentials = createLocalCredentials();
        Files.createDirectories(directory.resolve("data"));

        assertEquals(credentials, FirebaseConfig.credentialsFile(Map.of(), directory));
        assertEquals(credentials, FirebaseConfig.credentialsFile(Map.of(), directory.resolve("data")));
    }

    @Test
    void caminhoConfiguradoTemPrioridadeSobreArquivoLocal() throws Exception {
        createLocalCredentials();
        Path explicit = Files.writeString(directory.resolve("explicit.json"), "synthetic");

        assertEquals(explicit, FirebaseConfig.credentialsFile(
                Map.of("GOOGLE_APPLICATION_CREDENTIALS", explicit.toString()), directory));
    }

    @Test
    void caminhoRelativoConfiguradoParteDoDiretorioDeExecucao() throws Exception {
        Path explicit = Files.writeString(directory.resolve("explicit.json"), "synthetic");

        assertEquals(explicit, FirebaseConfig.credentialsFile(
                Map.of("GOOGLE_APPLICATION_CREDENTIALS", "explicit.json"), directory));
    }

    @Test
    void caminhoExplicitoInvalidoNaoTrocaSilenciosamenteDeCredencial() throws Exception {
        createLocalCredentials();

        var error = assertThrows(IOException.class, () -> FirebaseConfig.credentialsFile(
                Map.of("GOOGLE_APPLICATION_CREDENTIALS", "missing.json"), directory));
        assertTrue(error.getMessage().contains("GOOGLE_APPLICATION_CREDENTIALS"));
    }

    @Test
    void ausenciaDeArquivoLocalPermiteApplicationDefaultCredentials() throws Exception {
        assertNull(FirebaseConfig.credentialsFile(Map.of(), directory));
    }

    @Test
    void carregaCredencialExternaSemClasspathNemInicializacaoDoFirebase() throws Exception {
        Path credentials = createLocalCredentials();
        Files.writeString(credentials, """
                {"type":"authorized_user","client_id":"synthetic-client",
                 "client_secret":"synthetic-secret","refresh_token":"synthetic-token"}
                """);

        assertNotNull(FirebaseConfig.loadCredentials(Map.of(), directory));
    }

    @Test
    void emuladorNaoPrecisaDeArquivoDeCredencial() throws Exception {
        var environment = Map.of("FIRESTORE_EMULATOR_HOST", "127.0.0.1:8185",
                "GOOGLE_APPLICATION_CREDENTIALS", "missing.json");

        try (var firestore = FirebaseConfig.createFirestore(environment, directory)) {
            assertEquals("demo-jogai", firestore.getOptions().getProjectId());
            assertEquals("127.0.0.1:8185", firestore.getOptions().getEmulatorHost());
        }
    }

    @Test
    void permiteSelecionarProjetoSemAlterarCredencial() {
        assertEquals("jogai-paulo", FirebaseConfig.configuredProjectId(
                Map.of("GOOGLE_CLOUD_PROJECT", "jogai-paulo", "GCLOUD_PROJECT", "other")));
        assertEquals("other", FirebaseConfig.configuredProjectId(Map.of("GCLOUD_PROJECT", "other")));
        assertNull(FirebaseConfig.configuredProjectId(Map.of("GOOGLE_CLOUD_PROJECT", "  ")));
    }

    private Path createLocalCredentials() throws Exception {
        Path credentials = directory.resolve("config/firebase/serviceAccountKey.json");
        Files.createDirectories(credentials.getParent());
        return Files.writeString(credentials, "synthetic");
    }
}
