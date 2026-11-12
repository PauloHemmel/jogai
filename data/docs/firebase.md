# Firebase e repositórios

## Padrão do professor

As entidades com modelo completo no domínio usam `FirestoreMapRepository`.
O repositório concreto escolhe a coleção, declara os campos únicos no construtor
e implementa os dois sentidos do mapeamento. CRUD, transações de unicidade e
paginação são herdados da classe base.

O exemplo atual de referência é
[`UsuarioFirestoreMapRepository`](https://github.com/kleberkruger/jogai/blob/main/data/src/main/java/br/ufms/jogai/data/repository/UsuarioFirestoreMapRepository.java).
Na versão atual da base, o método de leitura se chama
`documentToEntity(DocumentSnapshot)`, correspondente ao `mapToEntity` da mensagem
original do professor. A coleção de usuários neste projeto é `usuarios`.

| Entidade | Implementação | Coleção | Campos únicos, além do ID |
| --- | --- | --- | --- |
| Usuário | `UsuarioFirestoreMapRepository` | `usuarios` | `email`, `telefone` |
| Categoria | `CategoriaFirestoreMapRepository` | `categoriasJogo` | `nome` |
| Jogo | `JogoFirestoreMapRepository` | `jogos` | `nome` |
| Exemplar | `ExemplarFirestoreMapRepository` | `exemplares` | Nenhum |
| Empréstimo | `EmprestimoFirestoreMapRepository` | `emprestimos` | Nenhum |

Exemplares distintos podem pertencer ao mesmo jogo e proprietário. Empréstimos
distintos também podem ter os mesmos participantes. Por isso, seus dados
compartilhados não são declarados como únicos. As reservas de valores únicos
ficam em `_uniqueValues_`, seguindo o protocolo transacional da base do professor.

As coleções recebem documentos pelas operações `create` e `save` dos repositórios.
Não é necessário cadastrar cada coleção manualmente no console.

## Projeto existente e credencial local

O projeto usado nesta máquina é `jogai-paulo`. `FirebaseConfig` obtém o ID da
conta de serviço, sem fixar o projeto no código. Para selecionar explicitamente
outro projeto, use `GOOGLE_CLOUD_PROJECT` ou `GCLOUD_PROJECT`.

A chave desta máquina está em:

```text
config/firebase/serviceAccountKey.json
```

O arquivo é local, ignorado pelo Git e externo às pastas de código e de recursos.
Não é empacotado no JAR. Em outra máquina, coloque a credencial do projeto nesse
caminho ou informe o caminho do seu próprio arquivo:

```powershell
$env:GOOGLE_APPLICATION_CREDENTIALS = 'C:\credenciais\serviceAccountKey.json'
$env:GOOGLE_CLOUD_PROJECT = 'jogai-paulo'
```

Um caminho explícito em `GOOGLE_APPLICATION_CREDENTIALS` tem prioridade sobre o
arquivo local. Se ele não existir, a inicialização falha com uma mensagem de
configuração, sem trocar silenciosamente de conta. Sem nenhuma dessas opções,
a inicialização tenta Application Default Credentials. Nesse caso, informe o
projeto em `GOOGLE_CLOUD_PROJECT` quando a credencial não trouxer um ID de projeto.
Consulte também a [configuração oficial do Firebase Admin SDK](https://firebase.google.com/docs/admin/setup).

Execute pela raiz do repositório:

```powershell
.\gradlew.bat :data:verificarFirebase
```

O comando imprime o ID do projeto e realiza uma consulta limitada a um documento,
projetando apenas o ID. Não imprime os dados dos usuários e não cria, altera ou
exclui documentos. Executar `br.ufms.jogai.data.Main` no IDE faz a mesma verificação.

Esse comando também confirma o acesso a um Firestore vazio. A existência do
projeto Firebase, por si só, não substitui a criação e habilitação do banco Cloud
Firestore no console. O teste desta máquina confirmou acesso ao banco do projeto
`jogai-paulo`.

## Jogos e exemplares

`JogoFirestoreMapRepository` implementa `JogoRepository`, incluindo busca exata
por nome e listagens paginadas por categoria, editora e jogos ativos. Os campos
opcionais continuam nulos quando não informados. Durações são gravadas como
strings ISO-8601, preservando segundos e nanossegundos, inclusive quando somente
um dos limites foi informado.

O documento do jogo contém `categorias`, uma lista de mapas com `tag`, `nome` e
`descricao`. O campo `categoriaTags` contém as tags usadas no filtro por categoria.
As informações incorporadas permitem reconstruir o jogo sem consultas extras.

`ExemplarFirestoreMapRepository` implementa `ExemplarRepository`, incluindo
listagens paginadas por jogo, proprietário e estado de conservação. Seu documento
contém `jogo`, `proprietario`, `estadoConservacao`, `observacao` e `cadastradoEm`.
Jogo e proprietário são mapas com os dados de suas projeções `JogoInfo` e
`UsuarioInfo`. A data de cadastro usa o timestamp do Firestore.

Os dados relacionados incorporados representam o momento da gravação; alterações
posteriores em outros documentos não os atualizam automaticamente. Reconstruir
um exemplar não depende da existência de documentos separados de jogo ou usuário.
O mesmo princípio preserva o histórico dos participantes e itens de empréstimos.

## Testes

Os testes unitários podem ser executados sem Firebase:

```powershell
.\gradlew.bat test
```

Os testes de persistência usam exclusivamente projetos `demo-jogai-*` no emulador.
Com um emulador Firestore disponível em `127.0.0.1:8185`, execute:

```powershell
$env:FIRESTORE_EMULATOR_HOST = '127.0.0.1:8185'
.\gradlew.bat test
Remove-Item Env:\FIRESTORE_EMULATOR_HOST
```

Com `FIRESTORE_EMULATOR_HOST`, a configuração compartilhada também usa o emulador
sem carregar credenciais reais. Sem essa variável, os testes de persistência são
explicitamente ignorados. A verificação do projeto real é uma tarefa separada.

Os testes de Jogos e Exemplares cobrem criação, reconstrução dos dados,
atualização, exclusão, consultas paginadas, duplicidade de nomes, liberação de
reservas, valores opcionais e independência dos documentos relacionados.

## Estado dos demais tipos do domínio

`ItemEmprestimo` é persistido na lista `itens` do documento de empréstimo.
`AvaliacaoMediaUsuario` é persistida nos campos agregados de avaliação do usuário.
Esses tipos não precisam de coleções independentes no modelo atual.

`Devolucao` tem apenas um construtor protegido e campos privados, sem métodos
de criação, leitura ou regras de negócio. `DevolucaoDocument.toEntity()` ainda
retorna `null`. `Avaliacao`, `AvaliacaoProprietario` e `AvaliacaoTomador` também
estão sem dados definidos. Esses esboços precisam ter o domínio concluído para
receberem repositórios funcionais. Os exemplos em `domain.AI` não são usados pelos
repositórios da aplicação.
