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
        @Unique String email,
        @Unique String telefone,
        Timestamp nascimento,
        EstatisticaAvaliacao avaliacaoComodante,
        EstatisticaAvaliacao avaliacaoComodatario,
        Timestamp cadastradoEm,
        String status
) implements EntityDocument<Usuario> {

    public UsuarioDocument(Usuario usuario) {
        this(
                usuario.getId().toString(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getTelefone(),
                EntityDocument.toTimestamp(usuario.getDataNascimento()),
                new EstatisticaAvaliacao(usuario.getQuantidadeAvaliacoesComodante(), usuario.getNotaAvaliacaoComodante()),
                new EstatisticaAvaliacao(usuario.getQuantidadeAvaliacoesComodatario(), usuario.getNotaAvaliacaoComodatario()),
                EntityDocument.toTimestamp(usuario.getDataCadastro()),
                usuario.getStatus().name()
        );
    }

    public Usuario toEntity() {
        return Usuario.reconstitute(
                UUID.fromString(id),
                nome,
                email,
                telefone,
                EntityDocument.toLocalDate(nascimento),
                AvaliacaoMediaUsuario.reconstitute(
                        avaliacaoComodante().quantidade(), avaliacaoComodante.media(),
                        avaliacaoComodatario().quantidade(), avaliacaoComodatario.media()
                ),
                EntityDocument.toInstant(cadastradoEm),
                StatusUsuario.valueOf(status)
        );
    }
}
