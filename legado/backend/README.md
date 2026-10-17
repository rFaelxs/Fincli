# FinCLI Backend

API REST + front web do FinCLI. Spring Boot 3.3 · Java 21 · SQL Server · Flyway.

Implementa as decisões travadas em [`../docs/escopo-web.md`](../docs/escopo-web.md).

## Estrutura do front

Páginas estáticas em `src/main/resources/static`, uma pasta por recurso, com CSS e JS
próprios; o comum fica em `assets/`:

```
static/
├── index.html            # raiz: redireciona p/ dashboard (com sessão) ou login
├── assets/
│   ├── css/base.css      # tokens, reset e componentes compartilhados
│   └── js/
│       ├── api.js        # fetch + CSRF, formatação, tema, guarda de sessão
│       └── shell.js      # sidebar das páginas autenticadas
├── login/                # index.html · login.css · login.js
├── cadastro/             # index.html · cadastro.css · cadastro.js
├── dashboard/            # index.html · dashboard.css · dashboard.js
├── transacoes/           # index (busca/filtros) · create · update + transacoes.css
├── reservas/             # index · create · update (meta) · movimentar (alocar/sacar)
└── extrato/              # index.html · extrato.css · extrato.js
```

Convenção por recurso: `index` lista (com busca/filtros onde faz sentido), `create` cria,
`update` edita, `movimentar` cobre alocar/sacar. Navegação é por links reais entre páginas;
cada página autenticada valida a sessão via `App.guardar()` e monta a sidebar via
`App.montarShell()`. O `WebConfig` faz o forward de `/transacoes/` → `/transacoes/index.html`
(o Spring só resolve `index.html` na raiz).

## Pré-requisitos

- Java 21
- SQL Server 2019+ acessível (instância local ou via `docker compose up -d` na raiz do repo)

## Configuração do banco

Credenciais **não** ficam no repositório. Crie o banco e o login da aplicação uma vez:

```sql
CREATE DATABASE fincli;
GO
CREATE LOGIN fincli_app WITH PASSWORD = '<sua-senha>', DEFAULT_DATABASE = fincli;
GO
USE fincli;
CREATE USER fincli_app FOR LOGIN fincli_app;
ALTER ROLE db_owner ADD MEMBER fincli_app;   -- o Flyway cria e altera objetos de schema
GO
```

> Autenticação SQL exige modo misto. Verifique com
> `SELECT SERVERPROPERTY('IsIntegratedSecurityOnly')` — `0` significa que já está habilitado.

O schema em si é criado pelo Flyway no primeiro start; não rode DDL à mão.

## Executando

**Windows (recomendado).** Crie `backend\.env.local` — já ignorado pelo git — com:

```
FINCLI_DB_USER=fincli_app
FINCLI_DB_PASSWORD=<senha do login fincli_app>
FINCLI_COOKIE_SECURE=false
```

Depois, na pasta `backend\`:

```powershell
.\run-dev.ps1
```

O script carrega o `.env.local`, compila se o JAR não existir e sobe a aplicação.

**Qualquer sistema, na mão:**

```bash
export FINCLI_DB_USER=fincli_app
export FINCLI_DB_PASSWORD='<sua-senha>'
export FINCLI_COOKIE_SECURE=false   # apenas em dev, sobre http://localhost

mvn package -DskipTests
java -jar target/fincli-backend-1.0.0.jar
```

Em ambos os casos a aplicação sobe em `http://localhost:8080` — abra no navegador.
Pare com `Ctrl+C`.

> **Use `java -jar`, não `mvn spring-boot:run`.** O caminho deste repositório contém um
> acento (`Área de Trabalho`), e o processo filho que o plugin cria recebe o classpath com a
> codificação corrompida — a falha aparece como
> `ClassNotFoundException: com.rfaelxs.web.FinCliApplication`.

### Variáveis de ambiente

| Variável | Padrão | Função |
|---|---|---|
| `FINCLI_DB_URL` | `jdbc:sqlserver://localhost:1433;databaseName=fincli;…` | Conexão JDBC |
| `FINCLI_DB_USER` | — (obrigatória) | Login da aplicação |
| `FINCLI_DB_PASSWORD` | — (obrigatória) | Senha do login |
| `FINCLI_PORT` | `8080` | Porta HTTP |
| `FINCLI_COOKIE_SECURE` | `true` | `false` só em dev sobre HTTP |
| `FINCLI_CORS_ORIGENS` | `http://localhost:5173` | Origens aceitas (SPA) |

## Contrato da API

Sessão por cookie `HttpOnly` com CSRF habilitado. O SPA deve chamar `GET /api/csrf` uma vez
para receber o cookie `XSRF-TOKEN` e reenviá-lo no header `X-XSRF-TOKEN` em toda mutação.

| Método | Rota | Respostas |
|---|---|---|
| `POST` | `/api/cadastro` | 201 · 400 CPF/senha inválidos · 409 CPF já existe |
| `POST` | `/api/login` | 200 · 401 |
| `POST` | `/api/logout` | 204 |
| `GET` | `/api/me` | 200 · 401 |
| `GET` | `/api/csrf` | 204 (emite o cookie) |
| `GET` | `/api/dashboard?mes=yyyy-MM` | 200 |
| `GET` | `/api/transacoes` | 200 |
| `GET` | `/api/transacoes/{id}` | 200 · 404 |
| `POST` | `/api/transacoes` | 201 · 400 |
| `PUT` | `/api/transacoes/{id}` | 200 · 400 · 404 |
| `DELETE` | `/api/transacoes/{id}` | 204 · 404 |
| `GET` | `/api/reservas` | 200 |
| `GET` | `/api/reservas/{id}` | 200 · 404 |
| `POST` | `/api/reservas` | 201 · 400 |
| `POST` | `/api/reservas/{id}/alocar` | 200 · 400 saldo insuficiente · 404 |
| `POST` | `/api/reservas/{id}/sacar` | 200 · 400 saldo da reserva insuficiente · 404 |
| `PUT` | `/api/reservas/{id}/meta` | 200 · 400 · 404 |
| `DELETE` | `/api/reservas/{id}` | 204 · 409 emergência ou com saldo · 404 |
| `GET` | `/api/extrato` | 200 |

Erros seguem `application/problem+json` (RFC 7807). Erros de validação trazem um objeto
`campos` com a mensagem por campo.

Recurso de outro usuário responde **404**, nunca 403: distinguir "não existe" de "não é seu"
revelaria a existência de recursos alheios.

## Testes

```bash
mvn test              # 25 testes de regra de negócio, sem banco
./smoke-test.sh       # 22 checks contra a API rodando, ponta a ponta
```

O `smoke-test.sh` exercita o fluxo real: cadastro, login, alocação seguida de transação
(o cenário que corrompia dados na versão CLI), regras de saldo, isolamento entre usuários
e logout.

## Decisões que valem conhecer

- **Serviços sem estado.** Recebem o usuário por parâmetro e vão ao banco a cada operação.
  A versão CLI guardava `DadosUsuario` em campo, e duas cópias divergentes causavam perda
  silenciosa de dados.
- **`BigDecimal` com escala 2** em todo valor monetário; `DECIMAL(19,2)` no banco.
  Nunca `double`, nunca o tipo `MONEY` do SQL Server.
- **`BIGINT IDENTITY` como PK clusterizada** + `UNIQUEIDENTIFIER public_id` exposto na API.
  Um GUID aleatório como chave clusterizada fragmenta as páginas do índice.
- **Sessão em cookie, não token em `localStorage`.** Qualquer XSS no front-end leria o token;
  um cookie `HttpOnly` não é legível por JavaScript.
- **Reserva travada com `PESSIMISTIC_WRITE`** durante alocação e saque, e o saldo disponível
  é lido dentro da transação, depois do lock.
- **Meta Selic da série SGS 432**, com cache de aplicação (TTL 24h) e fallback para o último
  valor conhecido. Ver [`../docs/spec-selic.md`](../docs/spec-selic.md).

## Pendências

- Recuperação de senha (`escopo-web.md` §1.1). A coluna `email` já existe e é opcional, mas
  não há fluxo. **Esta é a decisão que ainda bloqueia o cadastro em produção.**
- Testes de repositório com Testcontainers (`escopo-web.md` §2.3).
- Bloqueio por tentativas repetidas de login.
