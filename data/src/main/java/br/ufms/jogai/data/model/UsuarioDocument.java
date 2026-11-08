package br.ufms.jogai.data.model;

import br.ufms.jogai.data.model.shared.EntityDocument;
import br.ufms.jogai.data.model.shared.Unique;
import br.ufms.jogai.domain.usuario.AvaliacaoMediaUsuario;
import br.ufms.jogai.domain.usuario.StatusUsuario;
import br.ufms.jogai.domain.usuario.Usuario;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;

import java.util.UUID;

public record UsuarioDocument(
        @DocumentId String id,
        String nome,
        String usuario,
        @Unique String email,
        @Unique String telefone,
        Timestamp nascimento,
        AvaliacaoUsuarioDocument avaliacao,
        String status,
        Timestamp cadastradoEm
) implements EntityDocument<Usuario> {

    public UsuarioDocument(Usuario usuario) {
        this(
                usuario.getId().toString(),
                usuario.getNome(),
                usuario.getUsuario(),
                usuario.getEmail(),
                usuario.getTelefone(),
                EntityDocument.toTimestamp(usuario.getDataNascimento()),
                new AvaliacaoUsuarioDocument(usuario.getQuantidadeAvaliacoesComodante(), usuario.getNotaAvaliacaoComodante(),
                        usuario.getQuantidadeAvaliacoesComodatario(), usuario.getNotaAvaliacaoComodatario()),
                usuario.getStatus().name(),
                EntityDocument.toTimestamp(usuario.getDataCadastro())
        );
    }

    public Usuario toEntity() {
        return Usuario.reconstitute(
                UUID.fromString(id),
                nome,
                usuario,
                email,
                telefone,
                EntityDocument.toLocalDate(nascimento),
                AvaliacaoMediaUsuario.reconstitute(
                        avaliacao.qtdAvaliacoesComodante(), avaliacao.mediaAvaliacaoComodante(),
                        avaliacao.qtdAvaliacoesComodatario(), avaliacao.mediaAvaliacaoComodatario()
                ),
                StatusUsuario.valueOf(status),
                EntityDocument.toInstant(cadastradoEm)
        );
    }
}
