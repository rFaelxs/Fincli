# Escopo — FinCLI Web

Documento de **escopo**, não a spec. Cada seção abaixo corresponde a uma seção da spec que
você vai escrever: descreve o que precisa estar lá, o que já está decidido pelo código
existente e quais perguntas você precisa responder. Onde há recomendação, ela está marcada
como **→ Recomendo**; onde a decisão ainda é sua, está marcada como **◻ Decidir**.

## Decisões travadas

| Tema | Decisão | Consequência principal |
|---|---|---|
| Interface | **REST + MPA por recurso** (revisada — era SPA React) | Páginas HTML/CSS/JS por recurso em `static/`, servidas pelo backend; um artefato só, sem CORS em produção |
| Login | **CPF + senha** | Responsabilidade LGPD é assumida (§1.5); **falta caminho de recuperação de senha** (§1.1) |
| Dados existentes | **Começar do zero** | Sem importador; `data/*.json` é descartado |
| Banco | **SQL Server** | Flyway + `mssql-jdbc`; tipos conforme §2.3 |
| Backend | **Spring Boot 3 / Java 21** | Mantém a versão já usada pelo projeto |

---

## 0. Ponto de partida — o que existe hoje

Inventário honesto do que sobrevive à migração:

| Camada | Arquivos | Destino na web |
|---|---|---|
| Modelo de domínio | `model/` (User, Transacao, Reserva, MovimentacaoReserva, enums) | **Reaproveita** — vira entidade/DTO com ajustes (§1.4) |
| Regras de negócio | `TransacaoService`, `ReservaService`, `DashboardService` | **Reaproveita as regras, reescreve a estrutura** (§1.2) |
| Integração externa | `SelicRepository` | **Reaproveita quase intacto** — só ganha cache compartilhado |
| Persistência | `UsuarioRepository` (JSON por arquivo) | **Substitui** (§1.3) |
| Autenticação | `UserService` | **Substitui** — não há senha (§1.1) |
| Interface | `CommandHandler`, laço do `Main` | **Descarta** — é 100% terminal |
| Testes | `*ServiceTest`, `SelicRepositoryIntegrationTest` | **Reaproveita** — são testes de regra, não de UI |

O ponto importante: a lógica financeira (cálculo de saldo, alocação/saque, proteção da
Reserva de Emergência, totais mensais) está em services e já tem cobertura de teste. Ela é o
ativo do projeto. O que não sobrevive é a camada de I/O — em ambas as pontas.

---

## 1. Bloqueadores — resolver antes de escrever a primeira linha de web

Esta é a seção mais importante da spec. São problemas que o modelo CLI tolera e o modelo web
não. Escrever a spec sem endereçá-los produz um plano que não se sustenta.

### 1.1 Não existe senha — **bloqueador crítico**

```java
public User login(String cpf) {
  return repository.buscarPorCpf(cpf);   // UserService.java:55
}
```

Conhecer o CPF **é** a autenticação. Num binário rodando na sua própria máquina, isso é uma
conveniência com raio de exposição zero. Publicado na web, é bypass total de autenticação:
CPF não é segredo — é semi-público, aparece em cadastro de faculdade, nota fiscal e vazamento
de base. Qualquer pessoa com o CPF de alguém entra na conta e vê a vida financeira inteira.

**Decidido: CPF + senha.** A spec precisa definir:
- Senha com hash **BCrypt** (nunca SHA/MD5, nunca texto puro), política mínima de tamanho.
- Bloqueio por tentativas repetidas e o que a mensagem de erro revela — "CPF ou senha
  inválidos", nunca "CPF não encontrado", que entrega enumeração de cadastro.
- Validação de CPF (dígitos verificadores) no cadastro, que hoje não existe.

> **Problema aberto criado por esta decisão: não há recuperação de senha.**
> Com CPF como único identificador, não existe canal para provar identidade e redefinir
> senha — não há e-mail nem telefone no modelo. Usuário que esquecer a senha perde a conta e
> todo o histórico financeiro, sem recurso. As saídas possíveis:
>
> 1. **E-mail opcional, só para recuperação** — login continua por CPF, e-mail é um campo a
>    mais no cadastro usado exclusivamente para redefinir senha. **→ Recomendo.** Custa um
>    campo e resolve o problema inteiro.
> 2. **Reset manual pelo administrador** — funciona numa entrega acadêmica com poucos
>    usuários; não é resposta para uso real.
> 3. **Assumir a perda** — documentar que senha esquecida = conta perdida. Só é aceitável se
>    a spec disser isso explicitamente.
>
> ◻ **Decidir antes de modelar a tabela de usuário**, porque muda o schema.

### 1.2 Serviços guardam estado mutável carregado no construtor — **bloqueador + bug atual**

`TransacaoService` e `ReservaService` recebem o `idUsuario` no construtor e carregam **cada
um a sua própria cópia** de `DadosUsuario`:

```java
this.dados = repository.carregarDados(idUsuario);   // em ambos os services
```

Como cada mutação regrava o documento inteiro, as duas cópias sobrescrevem uma à outra.
**Isto já é um bug de perda de dados no CLI atual.** Reproduzi contra o código de hoje:

```
apos alocar 500, em memoria (rs): 500.0
apos alocar 500, no disco       : 500.0
apos nova transacao, no disco   : 0.0        ← alocação apagada
movimentacoes de reserva no disco: 0         ← e o registro do extrato também
```

Sequência: alocar R$ 500 na Reserva de Emergência e em seguida registrar **qualquer**
transação. O `TransacaoService` grava a cópia dele, cuja lista de reservas ainda é a de
antes da alocação. O dinheiro volta ao saldo disponível e a movimentação some do extrato —
silenciosamente, sem erro.

Para a web isso é duplamente bloqueante: além do bug, serviços com estado por usuário não
podem ser beans compartilhados. A spec precisa definir a passagem para serviços **stateless**,
que recebem o usuário por parâmetro e leem/escrevem por operação.

> Este bug existe hoje, em produção, independente da web. Vale corrigir em commit próprio
> antes da migração — assim a correção fica testável contra a suíte atual.

### 1.3 Persistência sem controle de concorrência

`data/{uuid}.json` reescrito por inteiro a cada operação, sem lock, sem transação, sem
versão. Um processo, um usuário, uma sessão: funciona. Na web, duas abas abertas já produzem
lost update — e não há como detectar que aconteceu.

Some-se: sem índice (busca de perfil é varredura linear de `perfis.json`), sem escrita
atômica (queda no meio do `FileWriter` deixa JSON truncado e corrompe a conta) e sem
migração de schema.

**Decidido: banco relacional (SQL Server).** O modelo já é naturalmente tabular
(usuário 1—N transações, 1—N reservas, 1—N movimentações) e as regras de saldo pedem
transação ACID.

### 1.4 Dinheiro em `double`

`valorTransacao`, `metaValor`, `saldoAtual` e todos os cálculos usam `double`. Erro de ponto
flutuante binário acumula em soma de moeda — `0.1 + 0.2 != 0.3`. Com poucos lançamentos
locais o desvio não aparece; num histórico longo, aparece no centavo.

**→ Recomendo** migrar para `BigDecimal` (escala 2, `RoundingMode.HALF_UP`) na mesma
oportunidade em que o modelo vira entidade. Fazer depois custa outra migração de dados.

### 1.5 CPF é dado pessoal — LGPD

Hoje o CPF fica em claro em `data/perfis.json`, na máquina do próprio usuário. Publicado,
passa a ser base de dados pessoais sob responsabilidade sua.

Como a decisão foi **manter o CPF como login** (§1.1), essa responsabilidade é assumida e a
spec precisa endereçá-la explicitamente:

- Base legal para o tratamento e política de privacidade acessível no cadastro.
- CPF **nunca** em log, mensagem de erro ou URL — nem em `GET /usuarios?cpf=...`.
- Cifra em repouso (SQL Server: *Always Encrypted* ou TDE) e canal sempre em HTTPS.
- Fluxo de exclusão de conta que apague de fato os dados, não só marque como inativo.
- Índice **único** em CPF, com a colisão tratada como erro de negócio no cadastro.

---

## 2. Decisões de arquitetura a registrar na spec

### 2.1 Stack do backend
**→ Recomendo Spring Boot 3 + Java 21** — já é a versão do projeto, e traz num pacote só o
que os bloqueadores acima exigem: Spring Security (§1.1), Spring Data JPA + transações
(§1.3), validação de entrada, e configuração por ambiente.
◻ Se houver restrição de disciplina/entrega que obrigue outra coisa, registre na spec.

### 2.2 Forma da interface
**Decidido (revisado): REST + MPA por recurso.** A decisão original era SPA React; foi
revisada durante a implementação para páginas HTML/CSS/JS organizadas por recurso
(`index`/`create`/`update`/`movimentar`, cada pasta com seu CSS e JS), servidas de
`resources/static` pelo próprio backend. Consequências: um artefato só, sem CORS em
produção, sem build de front — e o contrato de API (§4) continua sendo a fronteira, o que
mantém aberta a porta para um SPA no futuro sem tocar o backend.

O texto original da decisão SPA segue abaixo como registro do que precisaria mudar caso
ela seja retomada:

- **Estrutura do repositório:** monorepo com `backend/` e `frontend/`, ou dois repositórios?
- **CORS:** quais origens são aceitas em dev e em produção.
- **Formato de erro padronizado** (ex.: RFC 7807 `application/problem+json`) — o SPA precisa
  de um corpo de erro previsível para exibir mensagem ao usuário.
- **Build de entrega:** o SPA é servido pelo próprio Spring (`static/`, artefato único) ou
  publicado separado? A primeira opção elimina CORS em produção e mantém um só deploy.
  **→ Recomendo** essa.

Custo assumido com esta escolha: dois builds, dois conjuntos de dependências e o risco de
autenticação em SPA (§2.4) — que é onde a maioria dos projetos erra.

### 2.3 Banco de dados
**Decidido: SQL Server**, com **Flyway** versionando o schema. Pontos que a spec precisa
registrar por serem específicos deste banco:

| Conceito | No SQL Server |
|---|---|
| Driver | `com.microsoft.sqlserver:mssql-jdbc` |
| Dialeto JPA | `SQLServerDialect` |
| UUID (`idUsuario`) | `UNIQUEIDENTIFIER` — **não** use `NEWID()` como PK clusterizada; prefira `NEWSEQUENTIALID()` ou chave numérica + UUID público |
| Dinheiro (§1.4) | `DECIMAL(19,2)` — nunca `FLOAT`/`REAL`, nunca o tipo `MONEY` |
| Data (`LocalDate`) | `DATE` |
| Ambiente de dev | Docker `mcr.microsoft.com/mssql/server` ou SQL Server Express |

- ◻ **Decidir:** banco dos testes. Rodar Testcontainers com SQL Server real é fiel mas
  pesado; H2 em modo compatibilidade é rápido mas diverge do banco de produção.
  **→ Recomendo** Testcontainers para os testes de repositório e nada de banco para os
  testes de serviço, que já funcionam com mock.

### 2.4 Sessão e autenticação
Com SPA, esta decisão fica mais delicada e a spec precisa ser explícita.
**→ Recomendo sessão via cookie** `HttpOnly` + `Secure` + `SameSite=Lax`, mesmo com SPA:
guardar token em `localStorage` é o padrão mais comum e também o mais explorado, porque
qualquer XSS no front-end lê o token. Cookie `HttpOnly` não é legível por JavaScript.
Defina também: CSRF habilitado (obrigatório com cookie), timeout de sessão, HTTPS
obrigatório, e o comportamento do SPA ao receber `401` (redirecionar para login).

### 2.5 Dados existentes
**Decidido: começar do zero.** Sem importador; `data/*.json` é descartado e o banco nasce
vazio. Isso elimina o problema de "usuário sem senha" — todo cadastro passa pelo fluxo novo.

Consequências a registrar na spec:
- O `data/` local e o `.gitignore` correspondente deixam de ter função no produto web.
- Se em algum momento houver usuário real no CLI, essa decisão precisa ser revisitada
  **antes** do deploy, não depois.

### 2.6 Deploy
◻ Onde roda, como sobem as variáveis de ambiente (credenciais de banco **nunca** no
repositório), e o que acontece com o JAR do CLI — continua sendo mantido em paralelo ou é
aposentado? Essa resposta muda o README e a página de Releases.

---

## 3. Escopo funcional da v1

Preencha a coluna de decisão. O objetivo é ter uma v1 pequena que já substitua o CLI, não
uma v1 que faça tudo.

| Funcionalidade | Existe no CLI | v1 web? |
|---|---|---|
| Cadastro e login (agora com senha) | parcial | ◻ |
| Dashboard: saldo, entradas/saídas do mês, progresso da reserva, meta Selic | sim | ◻ |
| CRUD de transações | sim | ◻ |
| Criar reserva, alocar, sacar, excluir | sim | ◻ |
| Reserva de Emergência protegida contra exclusão | sim | ◻ |
| Extrato cronológico | sim | ◻ |
| Gráficos (evolução mensal, gasto por categoria) | não | ◻ |
| Filtro de transações por período/categoria | não | ◻ |
| Exportar CSV | não | ◻ |
| Rendimento estimado da reserva (ver `spec-selic.md` §5) | não | ◻ |
| Recuperação de senha | não | ◻ — depende de §1.1 |

O campo `essencial` de `Transacao` é preenchido hoje e **nunca usado** em nenhuma
visualização. A web é a chance de dar utilidade a ele (ex.: proporção essencial × supérfluo
no mês) ou removê-lo.

---

## 4. Contrato de API

Com a decisão por SPA, **esta é a seção mais importante da spec**: o contrato é a única
fronteira entre backend e front-end, e os dois times/etapas trabalham contra ele. Liste cada
rota com método, entrada, saída e erros. Esboço a completar:

```
POST   /api/cadastro      nome, cpf, senha          → 201 | 409 já existe
POST   /api/login         cpf, senha                → 200 + sessão | 401
POST   /api/logout                                  → 204
GET    /api/me                                      → usuário da sessão | 401
GET    /api/dashboard                               → saldo, totais do mês, reservas, selic
GET    /api/transacoes    ?mes=&categoria=          → lista
POST   /api/transacoes    valor, categoria, ...     → 201 | 400 valor inválido
PUT    /api/transacoes/{id}                         → 200 | 404 | 403 (dono ≠ sessão)
DELETE /api/transacoes/{id}                         → 204 | 404 | 403
GET    /api/reservas                                → lista
POST   /api/reservas      nome, meta                → 201
POST   /api/reservas/{id}/alocar  valor             → 200 | 400 saldo insuficiente
POST   /api/reservas/{id}/sacar   valor             → 200 | 400 saldo da reserva insuficiente
DELETE /api/reservas/{id}                           → 204 | 409 é a Reserva de Emergência
GET    /api/extrato                                 → transações + movimentações, ordenadas
```

`GET /api/me` não existia no esboço anterior e passa a ser necessário: o SPA precisa saber,
ao carregar, se há sessão válida e de quem ela é — informação que numa página renderizada no
servidor viria de graça.

Ponto que a spec **precisa** cravar: toda rota com `{id}` valida que o recurso pertence ao
usuário da sessão. No CLI isso era garantido pelo processo; na web, é a falha de autorização
mais comum que existe (IDOR). O `403` na tabela acima não é decorativo.

As `ValorInvalidoException` e `IllegalStateException` já lançadas pelos services viram
respostas HTTP — defina o mapeamento numa `@ControllerAdvice` e a forma do corpo de erro.

---

## 5. Requisitos não-funcionais

- **Testes:** manter os 15 atuais verdes durante toda a migração — são a rede de segurança
  que prova que a regra de negócio não mudou de comportamento. Somar testes de camada web
  (`@WebMvcTest`) e de autorização (usuário A não acessa recurso de B).
- **Selic:** cache hoje é por sessão; na web vira cache compartilhado da aplicação com TTL
  (ver `spec-selic.md` §5, item 3). Uma chamada por sessão de usuário não escala e é
  desnecessária — a meta muda a cada ~45 dias.
- **Falha do BCB:** comportamento atual (exibir "Indisponível", não quebrar) deve ser
  preservado. A chamada não pode bloquear o render do dashboard.
- ◻ Logs, tratamento de erro 500, e o que **nunca** pode aparecer em log (CPF, senha, valores).

---

## 6. Riscos e fora de escopo

Declare explicitamente o que **não** será feito, para a v1 não inchar. Candidatos naturais a
"fora de escopo": app mobile, integração bancária/Open Finance, multiusuário na mesma conta,
importação de OFX/extrato bancário, notificações.

Riscos a registrar: os cinco bloqueadores da §1 são todos trabalho **anterior** à primeira
tela; subestimá-los é o principal risco de cronograma da entrega.

---

## 7. Checklist de prontidão da spec

A spec está pronta quando responde, sem ambiguidade:

- [x] Como alguém prova que é quem diz ser? — CPF + senha BCrypt (§1.1)
- [x] O que acontece com as contas que já existem em `data/`? — descartadas (§2.5)
- [ ] **Como um usuário recupera a senha esquecida?** (§1.1 — aberto, muda o schema)
- [ ] Onde os dados ficam e o que acontece com dois acessos simultâneos? (§1.2, §1.3)
- [ ] Dinheiro é representado como o quê? (§1.4)
- [ ] Quais telas existem na v1 e quais ficam para depois? (§3)
- [ ] Como o sistema impede que um usuário leia o recurso de outro? (§4)
- [ ] Qual é o modelo de tabelas, com os tipos do SQL Server? (§2.3)
- [ ] Onde o SPA é servido e quais origens o CORS aceita? (§2.2)
- [ ] O que explicitamente não será feito? (§6)
