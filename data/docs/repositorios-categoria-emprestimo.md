# Repositórios de Categoria e Empréstimo

Os contratos pertencem ao domínio principal, nos pacotes `jogo` e `emprestimo`.
Os dados dos participantes usam `usuario.UsuarioInfo`. O pacote `domain.AI`
não participa desta implementação.

Os dois repositórios reutilizam `FirestoreMapRepository` para CRUD, paginação e
captura síncrona dos dados enviados às gravações assíncronas. `create` rejeita um
ID existente; `save` cria ou substitui integralmente o documento; `delete` também
aceita um ID ausente. Os métodos retornam `CompletableFuture`, e falhas de banco
são propagadas pelo futuro. Validações locais podem lançar exceção imediatamente.
O cliente Firestore injetado pertence ao chamador.

## Categoria

`CategoriaRepository` é implementado por `CategoriaFirestoreMapRepository`.
A coleção continua sendo `categoriasJogo`, e o ID do documento é a tag gerada pelo
domínio. Os campos são `nome` e `descricao`; a descrição pode ser nula ou vazia.
O nome é único, com as reservas mantidas atomicamente pela infraestrutura existente.

`buscarPorNome(nome)` e `listarPorNome(nome, tamanhoPagina, aposTag)` validam o nome
e normalizam os espaços, incluindo as extremidades, com a mesma regra da entidade.
A correspondência usa o nome completo e diferencia maiúsculas de minúsculas.
`getPage` oferece a listagem geral por tag.

Os repositórios de Usuário, tanto Map quanto POJO, usam `usuarios`. O apontamento
anterior deles para `categoriasJogo` foi corrigido. Nenhum dado existente foi movido
ou excluído; bases que tenham registros misturados precisam de análise específica
antes de qualquer migração.

## Empréstimo e cópias históricas

`EmprestimoRepository` é implementado por `EmprestimoFirestoreMapRepository`.
Cada empréstimo ocupa um documento de `emprestimos`, cujo ID é seu UUID textual.
O documento contém os seguintes campos:

| Campo | Conteúdo |
| --- | --- |
| `proprietario` e `tomador` | Mapas com os sete campos de `UsuarioInfo`: ID textual, nome, email, telefone, nascimento e as duas médias de avaliação. |
| `dataEmprestimo` | Início como `Timestamp`. |
| `dataPrevistaDevolucao` | Vencimento comum aos itens, como `Timestamp`. |
| `itens` | Lista de mapas com `id`, `exemplar` e `dataEfetivaDevolucao` opcional. |

O mapa de `exemplar` contém `id`, `idJogo`, `nomeJogo`, `editoraJogo`,
`estadoConservacao`, `observacao`, `idProprietario`, `nomeProprietario` e
`emailProprietario`. Os dados são copiados de `ExemplarInfo`, e o estado é salvo
pelo nome do enum. Observação e data efetiva de devolução admitem `null`.
O nascimento é armazenado à meia-noite UTC, e todos os outros instantes são UTC.
O [Firestore limita a precisão das datas a microssegundos](https://firebase.google.com/docs/firestore/manage-data/data-types).
Prazos tão curtos que início e vencimento se tornariam iguais nessa precisão são
rejeitados antes da gravação, evitando salvar um documento impossível de reconstituir.

A leitura usa somente o documento do empréstimo: não exige documentos de usuários,
exemplares, jogos ou categorias. Alterações e exclusões posteriores nesses cadastros
não alteram o histórico. Campos obrigatórios ausentes, tipos incorretos e estados
inválidos produzem erro com o ID do documento e a causa original.

Os participantes devem ser distintos. O registro exige pelo menos um item, todos
do mesmo proprietário, sem repetição de ID do item ou do exemplar. O vencimento
deve ser posterior ao início. Cada devolução pode ocorrer no início ou depois,
inclusive após o vencimento. A lista retornada e os itens são imutáveis.

As fábricas novas são `ItemEmprestimo.criar`, `ItemEmprestimo.reconstituir`,
`Emprestimo.criar` e `Emprestimo.reconstituir`. O empréstimo oferece `adicionarItem`,
`removerItem` e `registrarDevolucao`; a última operação pode corrigir uma data já
registrada e substitui o item sem modificar versões anteriormente consultadas.

```java
var item = ItemEmprestimo.criar(exemplar);
var emprestimo = Emprestimo.criar(
        proprietarioInfo, tomadorInfo, inicio, vencimento, List.of(item));
EmprestimoRepository repositorio = new EmprestimoFirestoreMapRepository(firestore);

repositorio.create(emprestimo).thenCompose(salvo -> {
    salvo.registrarDevolucao(item.getId(), instanteRecebimento);
    return repositorio.save(salvo);
});
```

O prazo é fornecido pelo chamador. Não há cálculo automático de sete dias,
solicitação/aceite, controle de disponibilidade entre empréstimos, notificações,
avaliações nem tratamento de danos nesta entrega. A existência dos cadastros de
origem é responsabilidade dos casos de uso; o repositório mantém as cópias recebidas.
O nascimento é validado em relação à data do empréstimo, para que o envelhecimento
dos participantes não torne inválida uma cópia histórica anteriormente válida.

## Consultas paginadas

`listarPorProprietario(id, tamanhoPagina, aposId)` filtra por `proprietario.id`;
`listarPorTomador(id, tamanhoPagina, aposId)` filtra por `tomador.id`. Os IDs dos
participantes e o cursor são `UUID`, e o resultado é `Page<Emprestimo, UUID>`.
As sobrecargas sem cursor iniciam a consulta; as que recebem somente o participante
usam o tamanho padrão de 50 registros. A ordenação é pelo ID do empréstimo.

Reutilize o cursor somente com o mesmo filtro. O tamanho permitido é de 1 a 200.
Um cursor filtrado cujo documento foi excluído causa erro; a consulta não reinicia
silenciosamente. `getAll` carrega a coleção inteira e `getPage` oferece paginação
geral. As demais regras seguem o [guia de paginação](pagination.md).

## Testes

É necessário um JDK 25 em `JAVA_HOME` e um emulador Firestore em `127.0.0.1:8185`.
O emulador pode ser iniciado com o JAR oficial baixado pelo Firebase CLI:

```powershell
& "$env:JAVA_HOME\bin\java.exe" -jar $caminhoDoJarDoEmulador --host 127.0.0.1 --port 8185
```

Em outro terminal, na raiz do projeto:

```powershell
$env:FIRESTORE_EMULATOR_HOST = '127.0.0.1:8185'
.\gradlew.bat :domain:test --tests '*EmprestimoValidationTest' :data:test --tests '*CategoriaFirestoreMapRepositoryTest' --tests '*EmprestimoFirestoreMapRepositoryTest'
```

Para a regressão completa, execute `.\gradlew.bat :domain:test :data:test --continue`.
As integrações usam projetos `demo-jogai-*` isolados, sem credenciais reais.
Sem `FIRESTORE_EMULATOR_HOST`, esses testes são explicitamente ignorados.

### Verificação realizada em 11/11/2026

Ambiente: Temurin JDK 25.0.5, emulador oficial Firestore 1.22.0 e projetos isolados.

| Conjunto | Resultado |
| --- | --- |
| Domínio de Empréstimo | 15 testes aprovados |
| Integração de Categoria | 22 testes aprovados |
| Integração de Empréstimo | 16 testes aprovados |
| Toda a camada de dados | 144 testes aprovados, nenhum ignorado |
| Todo o domínio | 1.443 testes aprovados, nenhuma falha e nenhum ignorado |

A linha de base foi executada em uma cópia independente do commit `c68dca5`:
9 falhas no domínio e 37 na camada de dados. A implementação e a correção complementar
das validações eliminaram todas as falhas encontradas nessas suítes. O histórico detalhado
está no [relatório de validação do domínio](../../domain/docs/validacao-das-entidades.md).
