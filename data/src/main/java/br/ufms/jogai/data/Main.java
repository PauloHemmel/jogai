package br.ufms.jogai.data;

import br.ufms.jogai.data.repository.UsuarioFirestoreRepository;
import br.ufms.jogai.domain.usuario.Usuario;

import java.time.LocalDate;
import java.util.concurrent.ExecutionException;

public class Main {

    private static final UsuarioFirestoreRepository repository = new UsuarioFirestoreRepository();
    private static final String EMAIL_TESTE = "kleberkruger@gmail.com";

    static void main() {
        executar("Listar usuários", Main::testarListagem);
        executar("Criar usuário", Main::testarCriacao);
        executar("Buscar usuário por ID", Main::testarBuscaPorId);
        executar("Buscar usuário por e-mail", Main::testarBuscaPorEmail);
        executar("Excluir usuário", Main::testarExclusao);
    }

    private static void testarListagem() throws Exception {
        var page = repository.getPage(50).get();

        System.out.println("Página de usuários cadastrados:");
        page.items().forEach(usuario ->
                System.out.println("- " + usuario.getNome())
        );
        page.nextCursor().ifPresent(cursor ->
                System.out.println("Próximo cursor: " + cursor)
        );
    }

    private static void testarCriacao() throws Exception {
        var usuario = Usuario.create(
                "Kleber Kruger",
                EMAIL_TESTE,
                "67996122809",
                LocalDate.of(1988, 12, 8)
        );

        var criado = repository.create(usuario).get();
        System.out.println(criado.getNome() + " inserido com sucesso com o id: " + criado.getId());
    }

    private static void testarBuscaPorId() throws Exception {
        var usuario = repository.buscarPorEmail(EMAIL_TESTE)
                .get()
                .orElseThrow(() ->
                        new IllegalStateException("Usuário de teste não encontrado.")
                );

        var resultado = repository.get(usuario.getId()).get();
        resultado.ifPresentOrElse(
                u -> System.out.println("Encontrado: " + u.getNome()),
                () -> System.out.println("Usuário não encontrado.")
        );
    }

    private static void testarBuscaPorEmail() throws Exception {
        var resultado = repository.buscarPorEmail(EMAIL_TESTE).get();
        resultado.ifPresentOrElse(
                u -> System.out.println("Encontrado: " + u.getNome()),
                () -> System.out.println("Usuário não encontrado.")
        );
    }

    private static void testarExclusao() throws Exception {
        var usuario = repository.buscarPorEmail(EMAIL_TESTE)
                .get()
                .orElseThrow(() ->
                        new IllegalStateException("Usuário de teste não encontrado.")
                );

        repository.delete(usuario.getId()).get();
        System.out.println(usuario.getNome() + " excluído com sucesso.");
    }

    private static void executar(String nome, Teste teste) {
        System.out.println("\n=== " + nome + " ===");
        try {
            teste.executar();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Operação interrompida.");
        } catch (ExecutionException e) {
            Throwable causa = e.getCause();
            while (causa != null && causa.getCause() != null) {
                causa = causa.getCause();
            }
            System.err.println(causa != null ? causa.getMessage() : e.getMessage());
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    @FunctionalInterface
    private interface Teste {
        void executar() throws Exception;
    }
}
