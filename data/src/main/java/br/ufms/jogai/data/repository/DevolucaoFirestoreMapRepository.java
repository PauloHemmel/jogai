package br.ufms.jogai.data.repository;

import br.ufms.jogai.data.firebase.FirestoreMapRepository;
import br.ufms.jogai.data.firebase.config.FirebaseConfig;
import br.ufms.jogai.domain.emprestimo.Devolucao;
import br.ufms.jogai.domain.emprestimo.DevolucaoRepository;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DevolucaoFirestoreMapRepository extends FirestoreMapRepository<Devolucao, UUID>
        implements DevolucaoRepository {

    public DevolucaoFirestoreMapRepository() {
        this(FirebaseConfig.getFirestore());
    }

    public DevolucaoFirestoreMapRepository(Firestore firestore) {
        super(Objects.requireNonNull(firestore).collection("devolucoes"));
    }

    @Override
    protected Map<String, Object> entityToMap(Devolucao devolucao) {
        var dados = new HashMap<String, Object>();
        dados.put("emprestimoId", devolucao.getEmprestimoId().toString());
        dados.put("dataDevolucao", toTimestamp(devolucao.getDataDevolucao()));
        dados.put("avaliacaoDoProprietario", devolucao.getAvaliacaoDoProprietario());
        dados.put("avaliacaoDoTomador", devolucao.getAvaliacaoDoTomador());
        return dados;
    }

    @Override
    protected Devolucao documentToEntity(DocumentSnapshot documento) {
        return Devolucao.reconstituir(
                idToUUID(documento.getId()),
                idToUUID(documento.getString("emprestimoId")),
                toInstant(documento.getTimestamp("dataDevolucao")),
                toInteger(documento.getLong("avaliacaoDoProprietario")),
                toInteger(documento.getLong("avaliacaoDoTomador")));
    }

    @Override
    public CompletableFuture<Optional<Devolucao>> buscarPorEmprestimo(UUID emprestimoId) {
        return queryOne(collection.whereEqualTo("emprestimoId", Objects.requireNonNull(emprestimoId).toString()));
    }

    private static Integer toInteger(Long valor) {
        return valor == null ? null : Math.toIntExact(valor);
    }
}
