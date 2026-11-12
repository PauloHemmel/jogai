# Validação das entidades

Escopo: subclasses de `br.ufms.jogai.domain.shared.Entidade` e objetos de valor usados pelos seus atributos. O pacote `AI` foi desconsiderado.

## Executar

```sh
./gradlew :domain:test
```

Registro da etapa anterior de ampliação: **1.430 casos aprovados, nenhuma falha e nenhum ignorado**. A auditoria inicial tinha 139 casos, com 58 falhas; após as correções, a suíte tinha 153 casos. Foram adicionados outros 1.277 casos de fronteiras, entradas malformadas e integridade das referências. O teste de nascimento nulo foi atualizado para esperar `IllegalArgumentException` em lugar do `NullPointerException` anterior.

### Verificação dos repositórios em 11/11/2026

Uma cópia independente do commit `c68dca5`, anterior à implementação dos repositórios,
foi executada com JDK 25 e revelou **9 falhas em 1.430 testes de domínio**.
Na primeira verificação da implementação, foram executados **1.443 testes: 1.435
aprovados e 8 falhas preexistentes**.
A normalização de Categoria resolveu `JogoValidationTest.aceitaLimitesEOpcionais`.

A correção complementar resolveu as oito falhas: o ano de lançamento passou a
aceitar somente valores de 1 até o ano atual, a validação temporal passou a verificar
campos obrigatórios, e os dados de referência e o nome da categoria passaram a
produzir os erros de validação esperados. Dois testes de email foram alinhados ao
contrato de `IllegalArgumentException` do validador para entrada nula ou inválida.

**Resultado final: 1.443 testes de domínio aprovados, nenhuma falha e nenhum ignorado**,
incluindo os 15 testes de Empréstimo.

Na camada de dados, a cópia original apresentou **37 falhas em 116 testes**.
A versão atual passou nos **144 testes, sem falhas ou testes ignorados**, usando
o emulador Firestore. O resultado inclui 22 testes de Categoria e 16 de Empréstimo.
Os relatórios atuais estão em `domain/build/reports/tests/test/index.html` e
`data/build/reports/tests/test/index.html`.

Relatório: `domain/build/reports/tests/test/index.html`.

Na etapa anterior de correção também foi executado `./gradlew :domain:test :data:test`, com sucesso. No módulo de dados, três testes passaram e 91 foram ignorados pelas condições de ambiente da própria suíte; os testes de integração que exigem emulador Firestore não foram exercitados.

Os números contam invocações parametrizadas; cada caso pode ter várias asserções. A cobertura usa classes de equivalência, fronteiras, valores extremos e exemplos de entradas malformadas.

## Cobertura

| Entidade | Campos e caminhos exercitados |
| --- | --- |
| `Usuario` | Identidade, nome, email, telefone, nascimento, cadastro e status; criação, setters e reconstituição; preservação do valor após alteração rejeitada; normalização; fronteira da maioridade. |
| `Jogo` | Identidade, nome, editora, lançamento, jogadores, faixa etária, duração, categorias, descrição e ativo; builder, reconstituição, alterações, proteção da coleção e remoção da última categoria. |
| `CategoriaJogo` | Identidade textual, nome e descrição; criação, reconstituição e setters; limites de comprimento e valores opcionais. |
| `Exemplar` | Identidade, jogo, proprietário, conservação, observação e cadastro; criação, reconstituição, setters, todos os estados de conservação e integridade das referências. |
| `Emprestimo` | Criação e reconstituição, identidade, participantes distintos, prazo, propriedade dos exemplares, itens repetidos, proteção das coleções, cópias históricas e datas efetivas de devolução. |

`NumeroJogadores`, `FaixaEtaria` e `DuracaoPartida` são exercitados diretamente e pelo builder: negativos, zero onde há regra, limites invertidos, extremos de inteiro, valores opcionais, igualdade dos limites e duração negativa em nanossegundos.

## Casos adicionais

- Emails com arrobas repetidas ou semelhantes Unicode, domínio incompleto, rótulos e separadores inválidos, letras Unicode fora do contrato ASCII, emojis, controles inseridos na parte local/domínio e entradas muito longas. Casos válidos incluem subdomínios, rótulos punycode, tags, hífens e sufixos longos.
- Nomes com dígitos ASCII e Unicode, pontuação indevida, separadores repetidos/deslocados, marcas combinantes sem letra base, controles e comprimentos excessivos. Casos válidos incluem diferentes alfabetos, apóstrofos e acentos combinados.
- Todos os DDDs de dois dígitos: lista explícita de válidos testada nos três formatos suportados, com todos os prefixos de telefone fixo e celular; cada DDD inválido testado nos três formatos. Também há comprimentos incorretos, prefixos incorretos, código internacional inválido, ramais, dígitos não ASCII e formatação malformada.
- Datas nulas, futuras, anteriores ao limite, extremos dos tipos, dias bissextos e fronteiras inclusivas de nascimento. Datas de calendário inexistentes, como 31 de fevereiro, não podem ser representadas por `LocalDate`; a conversão de texto para datas está fora da API destas entidades.
- Caracteres de controle no início, meio e fim de nomes do catálogo/chaves, espaços Unicode isolados e extremos dos intervalos de jogadores, idades, durações e anos.
- As mesmas entradas inválidas são verificadas na criação, alteração e reconstituição de usuários e nas referências de jogo/proprietário recebidas por exemplares. Alterações rejeitadas devem preservar os atributos anteriores.

A ampliação alterou somente testes e esta documentação; não foi necessário modificar novamente as validações de produção. As listas de referência são explícitas e os geradores são determinísticos, sem derivar as expectativas das expressões regulares de produção.

## Correções

- **Nomes de pessoas:** cada parte exige letras; hífens e apóstrofos são permitidos entre letras. Suporte a alfabetos Unicode e normalização NFC para acentos combinados. Nomes completos continuam exigindo sobrenome.
- **Emails:** pontos não podem iniciar/finalizar a parte local nem aparecer consecutivamente. Rótulos de domínio não podem iniciar/finalizar com hífen; sufixos longos são aceitos. O limite de 50 caracteres existente foi preservado.
- **Telefones:** validação de DDD brasileiro e prefixos de assinantes, mantendo os formatos de entrada e a normalização para dígitos nacionais.
- **Nomes de catálogo e chaves de categoria:** rejeição de entradas vazias/invisíveis e caracteres de controle. Limites existentes de nomes preservados.
- **Nascimento:** comparação entre datas, incluindo o dia exato de 150 anos atrás. Valores nulos obrigatórios geram erro de validação e valores opcionais nulos são aceitos.
- **Cadastro:** usuário e exemplar rejeitam cadastro futuro, inclusive na reconstituição.
- **Categorias:** rejeição de elementos nulos na construção; adição em lote valida todas as entradas antes de alterar o conjunto.
- **Referências de exemplar:** identidade e campos obrigatórios dos snapshots de jogo e proprietário são validados ao construir o exemplar.
- **Validadores opcionais:** valores informados também passam pela expressão regular. As sobrecargas de ano de lançamento aceitam `null` quando opcionais. Conversores que retornam `null` não restauram a entrada original.

A classe `LacunasValidacaoTest` permanece ativa como regressão dos problemas encontrados, com a tag `auditoria` e sem exclusões no Gradle.

## Limitações do modelo

- `Emprestimo` implementa o registro e as operações sobre itens. Solicitação, aceite, disponibilidade, avaliações e ocorrências de devolução continuam fora desta entrega. A persistência e os exemplos estão documentados em [Categoria e Empréstimo](../../data/docs/repositorios-categoria-emprestimo.md).
- Descrições e observações permanecem texto livre opcional, com `trim`; não foram inventados limites novos.
- Não foram introduzidos tetos para jogadores, faixa etária ou duração, nem idade mínima para cadastro. Duração zero continua aceita pelo contrato atual.
- A validação não consulta existência ou posse de emails/telefones. O pacote `AI` não foi alterado.
