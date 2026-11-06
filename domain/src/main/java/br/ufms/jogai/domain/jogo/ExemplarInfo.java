package br.ufms.jogai.domain.jogo;

import br.ufms.jogai.domain.util.Validar;

import java.util.UUID;

public record ExemplarInfo(
        UUID id,
        UUID idJogo,
        String nomeJogo,
        String editoraJogo,
        EstadoConservacao estadoConservacao,
        String observacao,
        UUID idProprietario,
        String nomePropretario,
        String emailPropretario
) {

    public ExemplarInfo {
        id = Validar.uuid(id);
        idJogo = Validar.uuid(idJogo);
        idProprietario = Validar.uuid(idProprietario);
    }
}
