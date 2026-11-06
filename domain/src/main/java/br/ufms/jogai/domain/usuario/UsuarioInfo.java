package br.ufms.jogai.domain.usuario;

import br.ufms.jogai.domain.util.Validar;

import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;

/**
 * Projeção imutável com os dados de usuário usados para consulta e apresentação.
 */
public record UsuarioInfo(
        UUID id,
        String nome,
        String email,
        String telefone,
        LocalDate dataNascimento,
        double notaAvaliacaoComodante,
        double notaAvaliacaoComodatario
) {
    /**
     * Cria a projeção a partir dos dados atuais de um usuário.
     */
    public UsuarioInfo(Usuario usuario) {
        this(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getTelefone(),
                usuario.getDataNascimento(),
                usuario.getNotaAvaliacaoComodante(),
                usuario.getNotaAvaliacaoComodatario()
        );
    }

    /**
     * @return idade calculada a partir da data de nascimento
     */
    public int idade() {
        return Usuario.idade(dataNascimento);
    }

    /**
     * @return {@code true} se o usuário tiver pelo menos 18 anos
     */
    public boolean isMaiorIdade() {
        return Usuario.idadeIgualOuMaiorQue(dataNascimento, 18);
    }
}
