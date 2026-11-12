package br.ufms.jogai.domain.jogo;

import br.ufms.jogai.domain.util.Validar;

import java.util.Objects;
import java.util.UUID;

/** Cópia imutável dos dados de um exemplar usados em consultas e registros de empréstimo. */
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
        nomeJogo = Validar.nomeJogo(nomeJogo);
        editoraJogo = Validar.nomeEditora(editoraJogo);
        estadoConservacao = Objects.requireNonNull(estadoConservacao, "O estado de conservação é obrigatório");
        observacao = observacao == null ? null : observacao.strip();
        nomePropretario = Validar.nomePessoa(nomePropretario, true);
        emailPropretario = Validar.email(emailPropretario);
    }

    /** Captura os dados atuais sem conservar referências às entidades mutáveis do cadastro. */
    public ExemplarInfo(Exemplar exemplar) {
        this(
                Objects.requireNonNull(exemplar, "O exemplar é obrigatório").getId(),
                exemplar.getJogoInfo().id(),
                exemplar.getJogoInfo().nome(),
                exemplar.getJogoInfo().editora(),
                exemplar.getEstadoConservacao(),
                exemplar.getObservacao(),
                exemplar.getProprietarioInfo().id(),
                exemplar.getProprietarioInfo().nome(),
                exemplar.getProprietarioInfo().email()
        );
    }
}
