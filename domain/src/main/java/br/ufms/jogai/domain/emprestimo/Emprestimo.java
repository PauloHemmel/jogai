package br.ufms.jogai.domain.emprestimo;

import br.ufms.jogai.domain.jogo.Exemplar;
import br.ufms.jogai.domain.shared.Entidade;
import br.ufms.jogai.domain.usuario.UsuarioInfo;
import br.ufms.jogai.domain.util.Validar;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Registro de um empréstimo com os dados dos participantes e exemplares na época
 * da inclusão. Alterações posteriores nos cadastros não modificam este registro.
 * O prazo é informado pelo chamador; aceite, entrega e disponibilidade pertencem
 * aos respectivos casos de uso.
 */
public final class Emprestimo extends Entidade<UUID> {

    private final UsuarioInfo proprietario;
    private final UsuarioInfo tomador;
    private final Instant dataEmprestimo;
    private final Instant dataPrevistaDevolucao;
    private final List<ItemEmprestimo> itens;

    private Emprestimo(UUID id, UsuarioInfo proprietario, UsuarioInfo tomador,
                       Instant dataEmprestimo, Instant dataPrevistaDevolucao,
                       Collection<ItemEmprestimo> itens) {
        super(Validar.uuid(id));
        this.dataEmprestimo = Objects.requireNonNull(dataEmprestimo, "A data do empréstimo é obrigatória");
        this.proprietario = validarParticipante(proprietario, dataEmprestimo);
        this.tomador = validarParticipante(tomador, dataEmprestimo);
        if (proprietario.id().equals(tomador.id())) {
            throw new IllegalArgumentException("Proprietário e tomador devem ser pessoas distintas");
        }
        this.dataPrevistaDevolucao = Objects.requireNonNull(dataPrevistaDevolucao,
                "A data prevista de devolução é obrigatória");
        if (!dataPrevistaDevolucao.isAfter(dataEmprestimo)) {
            throw new IllegalArgumentException("O vencimento deve ser posterior ao início do empréstimo");
        }
        Objects.requireNonNull(itens, "Os itens do empréstimo são obrigatórios");
        this.itens = new ArrayList<>(itens);
        if (this.itens.isEmpty()) {
            throw new IllegalArgumentException("O empréstimo deve conter pelo menos um item");
        }
        var identificadores = new HashSet<UUID>();
        var exemplares = new HashSet<UUID>();
        for (var item : this.itens) {
            validarItem(item);
            if (!identificadores.add(item.getId()) || !exemplares.add(item.getExemplarInfo().id())) {
                throw new IllegalArgumentException("O empréstimo não pode conter itens ou exemplares repetidos");
            }
        }
    }

    /**
     * Cria um registro com identificador novo e prazo explícito.
     *
     * @param proprietario dados de quem empresta
     * @param tomador dados de quem recebe os exemplares
     * @param dataEmprestimo instante de início, em UTC
     * @param dataPrevistaDevolucao vencimento comum a todos os itens
     * @param itens itens cujos exemplares pertencem ao proprietário
     * @return empréstimo validado, independente da coleção recebida
     * @throws IllegalArgumentException se participantes, prazo ou itens forem incompatíveis
     */
    public static Emprestimo criar(UsuarioInfo proprietario, UsuarioInfo tomador,
                                   Instant dataEmprestimo, Instant dataPrevistaDevolucao,
                                   Collection<ItemEmprestimo> itens) {
        return reconstituir(UUID.randomUUID(), proprietario, tomador, dataEmprestimo,
                dataPrevistaDevolucao, itens);
    }

    /** Reconstrói o registro preservando seu ID e aplicando as mesmas invariantes da criação. */
    public static Emprestimo reconstituir(UUID id, UsuarioInfo proprietario, UsuarioInfo tomador,
                                         Instant dataEmprestimo, Instant dataPrevistaDevolucao,
                                         Collection<ItemEmprestimo> itens) {
        return new Emprestimo(id, proprietario, tomador, dataEmprestimo, dataPrevistaDevolucao, itens);
    }

    public UsuarioInfo getProprietario() {
        return proprietario;
    }

    public UsuarioInfo getTomador() {
        return tomador;
    }

    public Instant getDataEmprestimo() {
        return dataEmprestimo;
    }

    public Instant getDataPrevistaDevolucao() {
        return dataPrevistaDevolucao;
    }

    /** @return cópia imutável da lista, cujos itens também são imutáveis */
    public List<ItemEmprestimo> getItens() {
        return List.copyOf(itens);
    }

    /** Inclui uma cópia dos dados do exemplar, rejeitando outro proprietário ou exemplar repetido. */
    public void adicionarItem(Exemplar exemplar) {
        var item = ItemEmprestimo.criar(exemplar);
        validarItem(item);
        if (itens.stream().anyMatch(atual -> atual.getExemplarInfo().id().equals(exemplar.getId()))) {
            throw new IllegalArgumentException("O exemplar já pertence ao empréstimo");
        }
        itens.add(item);
    }

    /** Remove um item existente, mantendo pelo menos um item no empréstimo. */
    public void removerItem(UUID itemId) {
        int indice = localizarItem(itemId);
        if (itens.size() == 1) {
            throw new IllegalArgumentException("Não é possível remover o último item do empréstimo");
        }
        itens.remove(indice);
    }

    /**
     * Registra ou corrige a data efetiva de devolução de um item.
     * Não altera o exemplar cadastrado nem registra avaliação ou ocorrência de dano.
     *
     * @param itemId identificador de um item deste empréstimo
     * @param dataDevolucao instante do recebimento, igual ou posterior ao início
     * @throws IllegalArgumentException se o item não existir ou a data anteceder o início
     */
    public void registrarDevolucao(UUID itemId, Instant dataDevolucao) {
        int indice = localizarItem(itemId);
        Objects.requireNonNull(dataDevolucao, "A data efetiva de devolução é obrigatória");
        validarDataDevolucao(dataDevolucao);
        itens.set(indice, itens.get(indice).comDevolucao(dataDevolucao));
    }

    private int localizarItem(UUID itemId) {
        Validar.uuid(itemId);
        for (int indice = 0; indice < itens.size(); indice++) {
            if (itens.get(indice).getId().equals(itemId)) return indice;
        }
        throw new IllegalArgumentException("O item informado não pertence ao empréstimo");
    }

    private void validarItem(ItemEmprestimo item) {
        Objects.requireNonNull(item, "O item do empréstimo não pode ser nulo");
        if (!proprietario.id().equals(item.getExemplarInfo().idProprietario())) {
            throw new IllegalArgumentException("Todos os exemplares devem pertencer ao proprietário do empréstimo");
        }
        validarDataDevolucao(item.getDataEfetivaDevolucao());
    }

    private void validarDataDevolucao(Instant dataDevolucao) {
        if (dataDevolucao != null && dataDevolucao.isBefore(dataEmprestimo)) {
            throw new IllegalArgumentException("A devolução não pode anteceder o início do empréstimo");
        }
    }

    private static UsuarioInfo validarParticipante(UsuarioInfo participante, Instant inicio) {
        Objects.requireNonNull(participante, "Os dados do participante são obrigatórios");
        Validar.uuid(participante.id());
        Validar.nomePessoa(participante.nome(), true);
        Validar.email(participante.email());
        Validar.telefone(participante.telefone());
        var nascimento = Objects.requireNonNull(participante.dataNascimento(), "O nascimento do participante é obrigatório");
        var diaEmprestimo = inicio.atZone(ZoneOffset.UTC).toLocalDate();
        // A passagem dos anos não deve invalidar um registro histórico antes válido.
        if (nascimento.isAfter(diaEmprestimo) || nascimento.isBefore(diaEmprestimo.minusYears(150))) {
            throw new IllegalArgumentException("O nascimento do participante é inválido na data do empréstimo");
        }
        validarMedia(participante.notaAvaliacaoComodante());
        validarMedia(participante.notaAvaliacaoComodatario());
        return participante;
    }

    private static void validarMedia(double media) {
        if (!Double.isFinite(media) || media < 0 || media > 5) {
            throw new IllegalArgumentException("A média das avaliações deve estar entre zero e cinco");
        }
    }
}
