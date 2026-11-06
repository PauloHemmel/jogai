package br.ufms.jogai.data.model;

import br.ufms.jogai.data.model.shared.EntityDocument;
import br.ufms.jogai.data.model.shared.Unique;
import br.ufms.jogai.domain.jogo.Jogo;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.annotation.DocumentId;

import java.util.Set;
import java.util.UUID;

public record JogoDocument(
        @DocumentId String id,
        @Unique String nome,
        String editora,
        Integer anoLancamento,
        int minimoJogadores,
        Integer maximoJogadores,
        Integer idadeMinima,
        Integer idadeMaxima,
        Long duracaoMinima,
        Long duracaoMaxima,
        Set<DocumentReference> categorias,
        String descricao,
        boolean ativo
) implements EntityDocument<Jogo> {

    public JogoDocument(Jogo jogo) {
        this(
                jogo.getId().toString(),
                jogo.getNome(),
                jogo.getEditora(),
                jogo.getAnoLancamento().getValue(),
                jogo.getMinimoJogadores(),
                jogo.getMaximoJogadores(),
                jogo.getIdadeMinimaRecomendada(),
                jogo.getIdadeMaximaRecomendada(),
                jogo.getTempoMinimoEstimado().toMinutes(),
                jogo.getTempoMaximoEstimado().toMinutes(),
//                Set.of(jogo.getCategorias()),
                Set.of(),
                jogo.getDescricao(),
                jogo.isAtivo()
        );
    }

    @Override
    public Jogo toEntity() {
        return Jogo.reconstitute(UUID.fromString(id), ativo).build();
    }
}
