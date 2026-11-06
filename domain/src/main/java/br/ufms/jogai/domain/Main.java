package br.ufms.jogai.domain;

import br.ufms.jogai.domain.jogo.*;
import br.ufms.jogai.domain.usuario.Usuario;

import java.time.LocalDate;
import java.time.Year;
import java.util.Set;
import java.util.UUID;

public class Main {

    static void main() {

        Usuario u = Usuario.create(
                "Kleber Kruger",
                "kleberkruger",
                "kleberkruger@gmail.com",
                "67996122809",
                LocalDate.of(1988, 12, 8)
        );

//        var j1 = Jogo.create(
//                "Detetive",
//                "Estrella",
//                Year.of(2005),
//                new NumeroJogadores(3, 6),
//                new FaixaEtaria(12),
//                new DuracaoPartida(15, 20),
//                Set.of(CategoriaJogo.create("estrategia", "Estratégia", "")),
//                ""
//        );

        var j2 = Jogo.builder()
                .nomeEditoraLancamento("Banco Imobiliário", "Estrella", 1990)
                .numeroJogadores(1, 5)
                .faixaEtaria(3, 6)
                .duracaoPartida(15, 30)
                .categorias(
                        CategoriaJogo.create("Estrategia", ""),
                        CategoriaJogo.create("Ação", "")
                )
                .create();

        var j3 = Jogo.builder()
                .nomeEditoraLancamento("Banco Imobiliário", "Estrella", 1990)
                .numeroJogadores(1, 5)
                .faixaEtaria(3, 6)
                .duracaoPartida(15, 30)
                .categorias(
                        CategoriaJogo.create("Estrategia", ""),
                        CategoriaJogo.create("Ação", "")
                )
                .reconstitute(UUID.randomUUID(), true);

        var j4 = Jogo.create()
                .nomeEditoraLancamento("Banco Imobiliário", "Estrella", 1990)
                .numeroJogadores(1, 5)
//                .faixaEtaria(3, 6)
                .duracaoPartida(15, 30)
                .categorias(
                        CategoriaJogo.create("Estrategia", ""),
                        CategoriaJogo.create("Ação", "")
                )
                .build();

        var j5 = Jogo.reconstitute(UUID.randomUUID(), true)
                .nomeEditoraLancamento("Banco Imobiliário", "Estrella", 1990)
                .numeroJogadores(1, 5)
                .faixaEtaria(3, 6)
                .duracaoPartida(15, 30)
                .categorias(
                        CategoriaJogo.create("Estrategia", "Jogos de estratégia"),
                        CategoriaJogo.create("Ação", "Jogos de ação")
                )
                .build();
    }
}
