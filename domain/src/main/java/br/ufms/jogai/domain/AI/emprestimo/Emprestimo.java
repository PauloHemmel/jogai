package br.ufms.jogai.domain.AI.emprestimo;

import br.ufms.jogai.domain.AI.jogo.Exemplar;
import br.ufms.jogai.domain.AI.jogo.ExemplarId;
import br.ufms.jogai.domain.AI.jogo.StatusExemplar;
import br.ufms.jogai.domain.AI.reserva.Reserva;
import br.ufms.jogai.domain.AI.reserva.ReservaId;
import br.ufms.jogai.domain.AI.reserva.StatusReserva;
import br.ufms.jogai.domain.AI.shared.Entidade;
import br.ufms.jogai.domain.AI.shared.Texto;
import br.ufms.jogai.domain.AI.usuario.Usuario;
import br.ufms.jogai.domain.AI.usuario.UsuarioId;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Aggregate root responsável pelas regras e pelo histórico dos itens emprestados.
 */
public final class Emprestimo extends Entidade<EmprestimoId> {

    public static final Duration PRAZO_PADRAO = Duration.ofDays(7);
    public static final int LIMITE_EXEMPLARES_POR_USUARIO = 3;

    private final UsuarioId usuarioId;
    private final Instant dataEmprestimo;
    private final Instant dataPrevistaDevolucao;
    private final Instant dataDevolucao;
    private final StatusEmprestimo status;
    private final String observacoes;
    private final List<ItemEmprestimo> itens;

    private Emprestimo(EmprestimoId id, UsuarioId usuarioId,
                      Instant dataEmprestimo, Instant dataPrevistaDevolucao, Instant dataDevolucao,
                      StatusEmprestimo status, String observacoes, List<ItemEmprestimo> itens) {
        super(id);
        this.usuarioId = Objects.requireNonNull(usuarioId, "usuarioId não pode ser nulo");
        this.dataEmprestimo = Objects.requireNonNull(dataEmprestimo, "dataEmprestimo não pode ser nula");
        this.dataPrevistaDevolucao = Objects.requireNonNull(dataPrevistaDevolucao,
                "dataPrevistaDevolucao não pode ser nula");
        if (!dataPrevistaDevolucao.isAfter(dataEmprestimo)) {
            throw new IllegalArgumentException("a devolução prevista deve ser posterior ao empréstimo");
        }
        this.status = Objects.requireNonNull(status, "status não pode ser nulo");
        if ((status == StatusEmprestimo.FINALIZADO) == (dataDevolucao == null)) {
            throw new IllegalArgumentException("somente empréstimos finalizados possuem data de devolução");
        }
        if (dataDevolucao != null && dataDevolucao.isBefore(dataEmprestimo)) {
            throw new IllegalArgumentException("data de devolução não pode anteceder o empréstimo");
        }
        this.dataDevolucao = dataDevolucao;
        this.observacoes = Texto.opcional(observacoes).orElse(null);
        Objects.requireNonNull(itens, "itens não pode ser nulo");
        this.itens = List.copyOf(itens);
        validarItens();
    }

    public static Emprestimo reconstitute(EmprestimoId id, UsuarioId usuarioId,
                                          Instant dataEmprestimo, Instant dataPrevistaDevolucao,
                                          Instant dataDevolucao, StatusEmprestimo status,
                                          String observacoes, List<ItemEmprestimo> itens) {
        return new Emprestimo(id, usuarioId, dataEmprestimo, dataPrevistaDevolucao,
                dataDevolucao, status, observacoes, itens);
    }

    /**
     * Cria um empréstimo validando regras que dependem do usuário e de outros aggregates.
     */
    public static Emprestimo realizar(EmprestimoId id, Usuario usuario, List<Emprestimo> emprestimosAtivos,
                                      List<Exemplar> exemplares, Instant instante, Duration prazo) {
        return criar(id, usuario, emprestimosAtivos, exemplares, Set.of(), null, instante, prazo);
    }

    /**
     * Cria um empréstimo que retira o exemplar reservado ao próprio usuário.
     */
    public static Emprestimo realizarPorReserva(EmprestimoId id, Usuario usuario,
                                                List<Emprestimo> emprestimosAtivos,
                                                List<Exemplar> exemplares, Reserva reserva,
                                                Instant instante, Duration prazo) {
        Objects.requireNonNull(reserva, "reserva não pode ser nula");
        Objects.requireNonNull(usuario, "usuario não pode ser nulo");
        Objects.requireNonNull(exemplares, "exemplares não pode ser nulo");
        if (reserva.status() != StatusReserva.ATENDIDA || !reserva.usuarioId().equals(usuario.id())) {
            throw new IllegalStateException("a reserva atendida deve pertencer ao usuário do empréstimo");
        }
        ExemplarId exemplarReservado = reserva.exemplarAtendidoId()
                .orElseThrow(() -> new IllegalStateException("reserva atendida sem exemplar associado"));
        if (exemplares.stream().noneMatch(exemplar -> exemplar.id().equals(exemplarReservado))) {
            throw new IllegalArgumentException("inclua no empréstimo o exemplar vinculado à reserva");
        }
        return criar(id, usuario, emprestimosAtivos, exemplares, Set.of(exemplarReservado), reserva.id(), instante, prazo);
    }

    private static Emprestimo criar(EmprestimoId id, Usuario usuario, List<Emprestimo> emprestimosAtivos,
                                    List<Exemplar> exemplares, Set<ExemplarId> idsReservadosAutorizados,
                                    ReservaId reservaAutorizada,
                                    Instant instante, Duration prazo) {
        Objects.requireNonNull(usuario, "usuario não pode ser nulo");
        Objects.requireNonNull(emprestimosAtivos, "emprestimosAtivos não pode ser nulo");
        Objects.requireNonNull(exemplares, "exemplares não pode ser nulo");
        Objects.requireNonNull(instante, "instante não pode ser nulo");
        Objects.requireNonNull(prazo, "prazo não pode ser nulo");
        if (!usuario.podeRealizarEmprestimo()) {
            throw new IllegalStateException("somente usuários ativos podem realizar empréstimos");
        }
        if (prazo.isZero() || prazo.isNegative()) throw new IllegalArgumentException("prazo deve ser positivo");
        if (exemplares.isEmpty()) throw new IllegalArgumentException("informe ao menos um exemplar");

        long exemplaresEmAberto = emprestimosAtivos.stream()
                .filter(e -> e.usuarioId.equals(usuario.id()))
                .filter(e -> e.estaEmAberto())
                .mapToLong(Emprestimo::quantidadeItensEmAberto).sum();
        boolean possuiAtraso = emprestimosAtivos.stream()
                .filter(e -> e.usuarioId.equals(usuario.id()))
                .anyMatch(e -> e.estaAtrasadoEm(instante));
        if (possuiAtraso) throw new IllegalStateException("usuário possui empréstimo atrasado");
        if (exemplaresEmAberto + exemplares.size() > LIMITE_EXEMPLARES_POR_USUARIO) {
            throw new IllegalStateException("limite de três exemplares emprestados por usuário excedido");
        }

        Set<ExemplarId> idsSolicitados = new HashSet<>();
        Set<ExemplarId> idsEmprestados = new HashSet<>();
        emprestimosAtivos.stream().filter(Emprestimo::estaEmAberto)
                .flatMap(e -> e.itens.stream()).filter(item -> !item.devolvido())
                .forEach(item -> idsEmprestados.add(item.exemplarId()));
        for (Exemplar exemplar : exemplares) {
            boolean disponivel = exemplar.status() == StatusExemplar.DISPONIVEL;
            boolean reservadoAoUsuario = exemplar.status() == StatusExemplar.RESERVADO
                    && idsReservadosAutorizados.contains(exemplar.id())
                    && exemplar.reservaId().filter(reservaAutorizada::equals).isPresent();
            if (!disponivel && !reservadoAoUsuario) {
                throw new IllegalStateException("somente exemplares disponíveis podem ser emprestados");
            }
            if (!idsSolicitados.add(exemplar.id()) || idsEmprestados.contains(exemplar.id())) {
                throw new IllegalStateException("exemplar já consta de um empréstimo em aberto");
            }
        }
        List<ItemEmprestimo> itens = exemplares.stream()
                .map(exemplar -> new ItemEmprestimo(ItemEmprestimoId.novo(), exemplar.id(), null)).toList();
        return new Emprestimo(id, usuario.id(), instante, instante.plus(prazo), null,
                StatusEmprestimo.ATIVO, null, itens);
    }

    public static Emprestimo realizar(EmprestimoId id, Usuario usuario, List<Emprestimo> emprestimosAtivos,
                                      List<Exemplar> exemplares, Instant instante) {
        return realizar(id, usuario, emprestimosAtivos, exemplares, instante, PRAZO_PADRAO);
    }

    private void validarItens() {
        if (itens.isEmpty()) throw new IllegalArgumentException("um empréstimo deve possuir ao menos um item");
        Set<ExemplarId> ids = new HashSet<>();
        for (ItemEmprestimo item : itens) {
            Objects.requireNonNull(item, "item não pode ser nulo");
            if (!ids.add(item.exemplarId())) throw new IllegalArgumentException("exemplar duplicado no empréstimo");
            item.dataDevolucao().ifPresent(data -> {
                if (data.isBefore(dataEmprestimo))
                    throw new IllegalArgumentException("devolução anterior ao empréstimo");
                if (dataDevolucao != null && data.isAfter(dataDevolucao)) {
                    throw new IllegalArgumentException("item devolvido após a finalização do empréstimo");
                }
            });
        }
        if (status == StatusEmprestimo.FINALIZADO && itens.stream().anyMatch(item -> !item.devolvido())) {
            throw new IllegalArgumentException("todos os itens devem ser devolvidos para finalizar o empréstimo");
        }
        if (status != StatusEmprestimo.FINALIZADO && itens.stream().allMatch(ItemEmprestimo::devolvido)) {
            throw new IllegalArgumentException("um empréstimo com todos os itens devolvidos deve ser finalizado");
        }
        if (status == StatusEmprestimo.FINALIZADO) {
            Instant ultimaDevolucao = itens.stream().flatMap(item -> item.dataDevolucao().stream())
                    .max(Instant::compareTo).orElseThrow();
            if (!ultimaDevolucao.equals(dataDevolucao)) {
                throw new IllegalArgumentException("a data de finalização deve corresponder à última devolução");
            }
        }
        if (status == StatusEmprestimo.CANCELADO && itens.stream().anyMatch(ItemEmprestimo::devolvido)) {
            throw new IllegalArgumentException("empréstimo com item devolvido não pode ser cancelado");
        }
    }

    public UsuarioId usuarioId() {
        return usuarioId;
    }

    public Instant dataEmprestimo() {
        return dataEmprestimo;
    }

    public Instant dataPrevistaDevolucao() {
        return dataPrevistaDevolucao;
    }

    public Optional<Instant> dataDevolucao() {
        return Optional.ofNullable(dataDevolucao);
    }

    public StatusEmprestimo status() {
        return status;
    }

    public Optional<String> observacoes() {
        return Optional.ofNullable(observacoes);
    }

    public List<ItemEmprestimo> itens() {
        return itens;
    }

    public boolean estaEmAberto() {
        return status == StatusEmprestimo.ATIVO || status == StatusEmprestimo.ATRASADO;
    }

    public boolean estaAtrasadoEm(Instant instante) {
        Objects.requireNonNull(instante, "instante não pode ser nulo");
        return estaEmAberto() && instante.isAfter(dataPrevistaDevolucao);
    }

    public long quantidadeItensEmAberto() {
        return itens.stream().filter(item -> !item.devolvido()).count();
    }

    public Emprestimo atualizarAtraso(Instant instante) {
        if (status == StatusEmprestimo.ATIVO && estaAtrasadoEm(instante)) {
            return copiar(StatusEmprestimo.ATRASADO, null, itens);
        }
        return this;
    }

    public Emprestimo devolver(ExemplarId exemplarId, Instant instante) {
        if (!estaEmAberto()) throw new IllegalStateException("somente empréstimos em aberto aceitam devolução");
        Objects.requireNonNull(exemplarId, "exemplarId não pode ser nulo");
        Objects.requireNonNull(instante, "instante não pode ser nulo");
        if (instante.isBefore(dataEmprestimo)) throw new IllegalArgumentException("devolução anterior ao empréstimo");
        List<ItemEmprestimo> itensAtualizados = new ArrayList<>(itens.size());
        boolean encontrou = false;
        for (ItemEmprestimo item : itens) {
            if (item.exemplarId().equals(exemplarId)) {
                if (item.devolvido()) throw new IllegalStateException("o exemplar já foi devolvido");
                itensAtualizados.add(item.devolver(instante));
                encontrou = true;
            } else {
                itensAtualizados.add(item);
            }
        }
        if (!encontrou) throw new IllegalArgumentException("exemplar não pertence a este empréstimo");
        boolean finalizado = itensAtualizados.stream().allMatch(ItemEmprestimo::devolvido);
        return copiar(finalizado ? StatusEmprestimo.FINALIZADO : status,
                finalizado ? instante : null, itensAtualizados);
    }

    public Emprestimo cancelar() {
        if (status != StatusEmprestimo.ATIVO)
            throw new IllegalStateException("somente empréstimos ativos podem ser cancelados");
        if (itens.stream().anyMatch(ItemEmprestimo::devolvido)) {
            throw new IllegalStateException("empréstimo com devolução parcial não pode ser cancelado");
        }
        return copiar(StatusEmprestimo.CANCELADO, null, itens);
    }

    private Emprestimo copiar(StatusEmprestimo novoStatus, Instant devolucao, List<ItemEmprestimo> novosItens) {
        return new Emprestimo(id(), usuarioId, dataEmprestimo, dataPrevistaDevolucao,
                devolucao, novoStatus, observacoes, novosItens);
    }
}
