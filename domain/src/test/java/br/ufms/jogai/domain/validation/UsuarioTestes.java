package br.ufms.jogai.domain.validation;

import br.ufms.jogai.domain.usuario.AvaliacaoMediaUsuario;
import br.ufms.jogai.domain.usuario.StatusUsuario;
import br.ufms.jogai.domain.usuario.Usuario;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Pequenos adaptadores de teste para construir usuários com a assinatura atual. */
final class UsuarioTestes {
    private UsuarioTestes() {}

    static Usuario reconstituir(UUID id, String nome, String email, String telefone,
                                LocalDate nascimento, Instant cadastro, StatusUsuario status) {
        return Usuario.reconstitute(id, nome, "usuarioteste", email, telefone, nascimento,
                AvaliacaoMediaUsuario.create(), status, cadastro);
    }
}
