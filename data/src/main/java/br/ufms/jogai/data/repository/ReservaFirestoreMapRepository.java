package br.ufms.jogai.data.repository;

import br.ufms.jogai.data.firebase.FirestoreMapRepository;
import br.ufms.jogai.data.firebase.config.FirebaseConfig;
import br.ufms.jogai.domain.reserva.Reserva;
import br.ufms.jogai.domain.reserva.ReservaRepository;
import br.ufms.jogai.domain.reserva.StatusReserva;
import br.ufms.jogai.domain.shared.Page;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ReservaFirestoreMapRepository extends FirestoreMapRepository<Reserva, UUID>
        implements ReservaRepository {

    public ReservaFirestoreMapRepository() {
        this(FirebaseConfig.getFirestore());
    }

    public ReservaFirestoreMapRepository(Firestore firestore) {
        super(Objects.requireNonNull(firestore).collection("reservas"));
    }

    @Override
    protected Map<String, Object> entityToMap(Reserva reserva) {
        return Map.of(
                "usuarioId", reserva.getUsuarioId().toString(),
                "jogoId", reserva.getJogoId().toString(),
                "dataReserva", toTimestamp(reserva.getDataReserva()),
                "status", reserva.getStatus().name());
    }

    @Override
    protected Reserva documentToEntity(DocumentSnapshot documento) {
        return Reserva.reconstituir(
                idToUUID(documento.getId()),
                idToUUID(documento.getString("usuarioId")),
                idToUUID(documento.getString("jogoId")),
                toInstant(documento.getTimestamp("dataReserva")),
                StatusReserva.valueOf(documento.getString("status")));
    }

    @Override
    public CompletableFuture<Page<Reserva, UUID>> listarPorUsuario(UUID usuarioId, int pageSize, UUID afterId) {
        return queryPage(collection.whereEqualTo("usuarioId", Objects.requireNonNull(usuarioId).toString()),
                pageSize, afterId);
    }

    @Override
    public CompletableFuture<Page<Reserva, UUID>> listarAtivasPorJogo(UUID jogoId, int pageSize, UUID afterId) {
        return queryPage(collection
                        .whereEqualTo("jogoId", Objects.requireNonNull(jogoId).toString())
                        .whereEqualTo("status", StatusReserva.ATIVA.name()),
                pageSize, afterId);
    }

    @Override
    public CompletableFuture<Boolean> existeAtivaPorUsuarioEJogo(UUID usuarioId, UUID jogoId) {
        return queryOne(collection
                .whereEqualTo("usuarioId", Objects.requireNonNull(usuarioId).toString())
                .whereEqualTo("jogoId", Objects.requireNonNull(jogoId).toString())
                .whereEqualTo("status", StatusReserva.ATIVA.name()))
                .thenApply(Optional::isPresent);
    }
}
