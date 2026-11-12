package br.ufms.jogai.data.repository;

import br.ufms.jogai.data.firebase.FirestoreMapRepository;
import br.ufms.jogai.data.firebase.config.FirebaseConfig;
import br.ufms.jogai.domain.emprestimo.Emprestimo;
import br.ufms.jogai.domain.emprestimo.EmprestimoRepository;
import br.ufms.jogai.domain.emprestimo.ItemEmprestimo;
import br.ufms.jogai.domain.jogo.EstadoConservacao;
import br.ufms.jogai.domain.jogo.ExemplarInfo;
import br.ufms.jogai.domain.shared.Page;
import br.ufms.jogai.domain.usuario.UsuarioInfo;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Persiste cada empréstimo e suas cópias históricas em um único documento.
 * A reconstrução é local: não busca usuários, exemplares, jogos ou categorias.
 * CRUD, captura das alterações e paginação são fornecidos pela classe base.
 */
public class EmprestimoFirestoreMapRepository extends FirestoreMapRepository<Emprestimo, UUID>
        implements EmprestimoRepository {

    /** Usa o cliente compartilhado de Firestore configurado pela aplicação. */
    public EmprestimoFirestoreMapRepository() {
        this(FirebaseConfig.getFirestore());
    }

    /** Usa o cliente recebido, cujo encerramento continua sendo responsabilidade do chamador. */
    public EmprestimoFirestoreMapRepository(Firestore firestore) {
        super(Objects.requireNonNull(firestore, "O cliente Firestore é obrigatório").collection("emprestimos"));
    }

    @Override
    protected Map<String, Object> entityToMap(Emprestimo emprestimo) {
        // O arredondamento do Firestore não pode transformar um prazo válido em dois instantes iguais.
        if (!emprestimo.getDataPrevistaDevolucao().truncatedTo(ChronoUnit.MICROS)
                .isAfter(emprestimo.getDataEmprestimo().truncatedTo(ChronoUnit.MICROS))) {
            throw new IllegalArgumentException("O vencimento deve ser posterior ao início também em microssegundos");
        }
        return Map.of(
                "proprietario", converterUsuarioParaMapa(emprestimo.getProprietario()),
                "tomador", converterUsuarioParaMapa(emprestimo.getTomador()),
                "dataEmprestimo", toTimestamp(emprestimo.getDataEmprestimo()),
                "dataPrevistaDevolucao", toTimestamp(emprestimo.getDataPrevistaDevolucao()),
                "itens", emprestimo.getItens().stream().map(this::converterItemParaMapa).toList()
        );
    }

    private Map<String, Object> converterUsuarioParaMapa(UsuarioInfo usuario) {
        return Map.of(
                "id", usuario.id().toString(),
                "nome", usuario.nome(),
                "email", usuario.email(),
                "telefone", usuario.telefone(),
                "dataNascimento", toTimestamp(usuario.dataNascimento()),
                "notaAvaliacaoComodante", usuario.notaAvaliacaoComodante(),
                "notaAvaliacaoComodatario", usuario.notaAvaliacaoComodatario()
        );
    }

    private Map<String, Object> converterItemParaMapa(ItemEmprestimo item) {
        var dados = new HashMap<String, Object>();
        dados.put("id", item.getId().toString());
        dados.put("exemplar", converterExemplarParaMapa(item.getExemplarInfo()));
        dados.put("dataEfetivaDevolucao", item.getDataEfetivaDevolucao() == null
                ? null : toTimestamp(item.getDataEfetivaDevolucao()));
        return dados;
    }

    private Map<String, Object> converterExemplarParaMapa(ExemplarInfo exemplar) {
        var dados = new HashMap<String, Object>();
        dados.put("id", exemplar.id().toString());
        dados.put("idJogo", exemplar.idJogo().toString());
        dados.put("nomeJogo", exemplar.nomeJogo());
        dados.put("editoraJogo", exemplar.editoraJogo());
        dados.put("estadoConservacao", exemplar.estadoConservacao().name());
        dados.put("observacao", exemplar.observacao());
        dados.put("idProprietario", exemplar.idProprietario().toString());
        dados.put("nomeProprietario", exemplar.nomePropretario());
        dados.put("emailProprietario", exemplar.emailPropretario());
        return dados;
    }

    @Override
    protected Emprestimo documentToEntity(DocumentSnapshot documento) {
        try {
            var dados = Objects.requireNonNull(documento.getData(), "O documento não contém dados");
            var itensPersistidos = lerObrigatorio(dados, "itens", List.class);
            var itens = new ArrayList<ItemEmprestimo>(itensPersistidos.size());
            for (var item : itensPersistidos) {
                if (!(item instanceof Map<?, ?> dadosItem)) {
                    throw new IllegalArgumentException("A lista de itens contém um registro inválido");
                }
                itens.add(converterMapaParaItem(dadosItem));
            }
            return Emprestimo.reconstituir(
                    idToUUID(documento.getId()),
                    converterMapaParaUsuario(lerObrigatorio(dados, "proprietario", Map.class)),
                    converterMapaParaUsuario(lerObrigatorio(dados, "tomador", Map.class)),
                    toInstant(lerObrigatorio(dados, "dataEmprestimo", Timestamp.class)),
                    toInstant(lerObrigatorio(dados, "dataPrevistaDevolucao", Timestamp.class)),
                    itens
            );
        } catch (IllegalArgumentException | NullPointerException erro) {
            throw new IllegalArgumentException("Documento de empréstimo inválido: " + documento.getId(), erro);
        }
    }

    private UsuarioInfo converterMapaParaUsuario(Map<?, ?> dados) {
        return new UsuarioInfo(
                UUID.fromString(lerObrigatorio(dados, "id", String.class)),
                lerObrigatorio(dados, "nome", String.class),
                lerObrigatorio(dados, "email", String.class),
                lerObrigatorio(dados, "telefone", String.class),
                toLocalDate(lerObrigatorio(dados, "dataNascimento", Timestamp.class)),
                lerObrigatorio(dados, "notaAvaliacaoComodante", Number.class).doubleValue(),
                lerObrigatorio(dados, "notaAvaliacaoComodatario", Number.class).doubleValue()
        );
    }

    private ItemEmprestimo converterMapaParaItem(Map<?, ?> dados) {
        var devolucao = lerOpcional(dados, "dataEfetivaDevolucao", Timestamp.class);
        return ItemEmprestimo.reconstituir(
                UUID.fromString(lerObrigatorio(dados, "id", String.class)),
                converterMapaParaExemplar(lerObrigatorio(dados, "exemplar", Map.class)),
                devolucao == null ? null : toInstant(devolucao)
        );
    }

    private ExemplarInfo converterMapaParaExemplar(Map<?, ?> dados) {
        return new ExemplarInfo(
                UUID.fromString(lerObrigatorio(dados, "id", String.class)),
                UUID.fromString(lerObrigatorio(dados, "idJogo", String.class)),
                lerObrigatorio(dados, "nomeJogo", String.class),
                lerObrigatorio(dados, "editoraJogo", String.class),
                EstadoConservacao.valueOf(lerObrigatorio(dados, "estadoConservacao", String.class)),
                lerOpcional(dados, "observacao", String.class),
                UUID.fromString(lerObrigatorio(dados, "idProprietario", String.class)),
                lerObrigatorio(dados, "nomeProprietario", String.class),
                lerObrigatorio(dados, "emailProprietario", String.class)
        );
    }

    // Campos obrigatórios incorretos não recebem valores padrão, pois isso ocultaria corrupção do registro.
    private static <T> T lerObrigatorio(Map<?, ?> dados, String campo, Class<T> tipo) {
        var valor = lerOpcional(dados, campo, tipo);
        if (valor == null) throw new IllegalArgumentException("Campo obrigatório ausente: " + campo);
        return valor;
    }

    private static <T> T lerOpcional(Map<?, ?> dados, String campo, Class<T> tipo) {
        var valor = dados.get(campo);
        if (valor == null) return null;
        if (!tipo.isInstance(valor)) throw new IllegalArgumentException("Tipo inválido no campo: " + campo);
        return tipo.cast(valor);
    }

    @Override
    public CompletableFuture<Page<Emprestimo, UUID>> listarPorProprietario(
            UUID proprietarioId, int tamanhoPagina, UUID aposId) {
        Objects.requireNonNull(proprietarioId, "O identificador do proprietário é obrigatório");
        return queryPage(collection.whereEqualTo("proprietario.id", proprietarioId.toString()), tamanhoPagina, aposId);
    }

    @Override
    public CompletableFuture<Page<Emprestimo, UUID>> listarPorTomador(
            UUID tomadorId, int tamanhoPagina, UUID aposId) {
        Objects.requireNonNull(tomadorId, "O identificador do tomador é obrigatório");
        return queryPage(collection.whereEqualTo("tomador.id", tomadorId.toString()), tamanhoPagina, aposId);
    }
}
