package br.ufms.jogai.domain.emprestimo;

import br.ufms.jogai.domain.jogo.Exemplar;
import br.ufms.jogai.domain.shared.Entidade;
import br.ufms.jogai.domain.usuario.UsuarioInfo;
import br.ufms.jogai.domain.util.Validar;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.UUID;

public class Emprestimo extends Entidade<UUID> {

    private UsuarioInfo proprietario;
    private UsuarioInfo tomador;
    private LocalDateTime dataEmprestimo;
    private LocalDate dataDevolucao;

    private Collection<ItemEmprestimo> itens;

    protected Emprestimo(UUID uuid) {
        super(Validar.uuid(uuid));
    }

    public void adicionarItem(Exemplar exemplar) {

    }
}
