package br.ufms.jogai.domain.usuario;

import br.ufms.jogai.domain.shared.Entidade;
import br.ufms.jogai.domain.util.Validar;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;
import java.util.UUID;

/**
 * Representa um usuário cadastrado, seus dados pessoais, estado e avaliações.
 */
public class Usuario extends Entidade<UUID> {

    private String nome;
    private String usuario;
    private String email;
    private String telefone;
    private LocalDate dataNascimento;
    private final AvaliacaoMediaUsuario avaliacao;
    private StatusUsuario status;
    private final Instant dataCadastro;

    /**
     * Constrói a entidade após validar e atribuir os seus dados.
     */
    private Usuario(
            UUID id,
            String nome,
            String usuario,
            String email,
            String telefone,
            LocalDate dataNascimento,
            AvaliacaoMediaUsuario avaliacao,
            StatusUsuario status,
            Instant dataCadastro
    ) {
        super(Validar.uuid(id));

        setNome(nome);
        setUsuario(usuario);
        setEmail(email);
        setTelefone(telefone);
        setDataNascimento(dataNascimento);

        this.avaliacao = Objects.requireNonNull(avaliacao, "A avaliação do usuário não pode ser nula");
        this.status = Objects.requireNonNull(status, "O status do usuário não pode ser nulo");
        this.dataCadastro = Validar.dataCadastro(dataCadastro);
    }

    /**
     * @return nome completo
     */
    public String getNome() {
        return nome;
    }

    /**
     * Define o nome após validá-lo.
     *
     * @param nome nome completo
     */
    public void setNome(String nome) {
        this.nome = Validar.nomePessoa(nome, true);
    }

    /**
     * @return nome de usuário
     */
    public String getUsuario() {
        return usuario;
    }

    /**
     * Define o usuário após validá-lo.
     */
    public void setUsuario(String usuario) {
        this.usuario = Validar.usuario(usuario);
    }

    /**
     * @return endereço de e-mail do usuário
     */
    public String getEmail() {
        return email;
    }

    /**
     * Define o e-mail após validá-lo.
     *
     * @param email email
     * @throws IllegalArgumentException caso o email seja nulo, vazio ou inválido
     */
    public void setEmail(String email) {
        this.email = Validar.email(email);
    }

    /**
     * @return telefone do usuário
     */
    public String getTelefone() {
        return telefone;
    }

    /**
     * Define o telefone após validá-lo.
     *
     * @param telefone telefone
     */
    public void setTelefone(String telefone) {
        this.telefone = Validar.telefone(telefone, true);
    }

    /**
     * @return data de nascimento do usuário
     */
    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    /**
     * Define a data de nascimento após validá-la.
     *
     * @param dataNascimento data de nascimento
     */
    public void setDataNascimento(LocalDate dataNascimento) {
        this.dataNascimento = Validar.dataNascimento(dataNascimento, true);
    }

    /**
     * Calcula a idade completa na data atual.
     */
    public static int idade(LocalDate dataNascimento) {
        Objects.requireNonNull(dataNascimento, "A data de nascimento não pode ser nula");
        return Period.between(dataNascimento, LocalDate.now()).getYears();
    }

    /**
     * Verifica se a pessoa atingiu a idade mínima informada.
     */
    public static boolean idadeIgualOuMaiorQue(LocalDate dataNascimento, int idade) {
        return idade(dataNascimento) >= idade;
    }

    /**
     * @return idade atual do usuário em anos completos
     */
    public int getIdade() {
        return idade(dataNascimento);
    }

    /**
     * @return {@code true} se o usuário tiver pelo menos 18 anos
     */
    public boolean isMaiorIdade() {
        return idadeIgualOuMaiorQue(dataNascimento, 18);
    }

    /**
     * @return instante em que o usuário foi cadastrado
     */
    public Instant getDataCadastro() {
        return dataCadastro;
    }

    /**
     * @return estado atual do usuário
     */
    public StatusUsuario getStatus() {
        return status;
    }

    /**
     * Registra uma avaliação recebida pelo usuário como comodante.
     *
     * @param nota nota da avaliação
     */
    public void avaliarComoComodante(int nota) {
        avaliacao.avaliarComoComodante(nota);
    }

    /**
     * @return número de avaliações recebidas como comodante
     */
    public long getQuantidadeAvaliacoesComodante() {
        return avaliacao.getQuantidadeAvaliacoesComodante();
    }

    /**
     * @return média das avaliações recebidas como comodante
     */
    public double getNotaAvaliacaoComodante() {
        return avaliacao.getMediaAvaliacaoComodante();
    }

    /**
     * Registra uma avaliação recebida pelo usuário como comodatário.
     *
     * @param nota nota da avaliação
     */
    public void avaliarComoComodatario(int nota) {
        avaliacao.avaliarComoComodatario(nota);
    }

    /**
     * @return número de avaliações recebidas como comodatário
     */
    public long getQuantidadeAvaliacoesComodatario() {
        return avaliacao.getQuantidadeAvaliacoesComodatario();
    }

    /**
     * @return média das avaliações recebidas como comodatário
     */
    public double getNotaAvaliacaoComodatario() {
        return avaliacao.getMediaAvaliacaoComodatario();
    }

    /**
     * Altera o estado do usuário para ativo.
     */
    public void ativar() {
        this.status = StatusUsuario.ATIVO;
    }

    /**
     * Altera o estado do usuário para inativo.
     */
    public void inativar() {
        this.status = StatusUsuario.INATIVO;
    }

    /**
     * Altera o estado do usuário para bloqueado.
     */
    public void bloquear() {
        this.status = StatusUsuario.BLOQUEADO;
    }

    /**
     * Cria um usuário com identificador e data de cadastro gerados automaticamente,
     * estado ativo e avaliações iniciais vazias.
     *
     * @param nome           nome completo
     * @param usuario        nome de usuário
     * @param email          endereço de e-mail
     * @param telefone       telefone de contato
     * @param dataNascimento data de nascimento
     * @return usuário recém-criado
     */
    public static Usuario create(
            String nome,
            String usuario,
            String email,
            String telefone,
            LocalDate dataNascimento
    ) {
        return new Usuario(
                UUID.randomUUID(),
                nome,
                usuario,
                email,
                telefone,
                dataNascimento,
                AvaliacaoMediaUsuario.create(),
                StatusUsuario.ATIVO,
                Instant.now()
        );
    }

    /**
     * Reconstrói um usuário a partir dos dados existentes, por exemplo, carregados
     * de uma fonte de persistência.
     *
     * @param id             identificador do usuário
     * @param nome           nome completo
     * @param usuario        nome de usuário
     * @param email          endereço de e-mail
     * @param telefone       telefone de contato
     * @param dataNascimento data de nascimento
     * @param avaliacao      estatísticas de avaliação
     * @param dataCadastro   instante do cadastro
     * @param status         estado atual
     * @return usuário reconstruído
     */
    public static Usuario reconstitute(
            UUID id,
            String nome,
            String usuario,
            String email,
            String telefone,
            LocalDate dataNascimento,
            AvaliacaoMediaUsuario avaliacao,
            StatusUsuario status,
            Instant dataCadastro
    ) {
        return new Usuario(
                id,
                nome,
                usuario,
                email,
                telefone,
                dataNascimento,
                avaliacao,
                status,
                dataCadastro
        );
    }
}
