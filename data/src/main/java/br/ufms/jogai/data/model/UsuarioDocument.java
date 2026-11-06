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
        @Unique String usuario,
        @Unique String email,
        @Unique String telefone,
        Timestamp nascimento,
        EstatisticaAvaliacao avaliacaoComodante,
        EstatisticaAvaliacao avaliacaoComodatario,
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
                new EstatisticaAvaliacao(usuario.getQuantidadeAvaliacoesComodante(), usuario.getNotaAvaliacaoComodante()),
                new EstatisticaAvaliacao(usuario.getQuantidadeAvaliacoesComodatario(), usuario.getNotaAvaliacaoComodatario()),
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
                        avaliacaoComodante().quantidade(), avaliacaoComodante.media(),
                        avaliacaoComodatario().quantidade(), avaliacaoComodatario.media()
                ),
                StatusUsuario.valueOf(status),
                EntityDocument.toInstant(cadastradoEm)
        );
    }
}
