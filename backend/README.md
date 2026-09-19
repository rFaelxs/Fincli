# FinCLI Backend

API REST do FinCLI. Spring Boot 3.3 · Java 21 · SQL Server · Flyway.

Em produção, o mesmo JAR serve o front Angular de [`../frontend/`](../frontend/): um artefato só,
sem CORS ([ADR 0002](../docs/adr/0002-empacotamento-monolitico.md)). As decisões de arquitetura
estão em [`../docs/adr/`](../docs/adr/) e o comportamento de cada tela em
[`../docs/specs/`](../docs/specs/).

## Estrutura

```
src/main/java/com/rfaelxs/web/
├── api/          # controllers REST, DTOs e o tradutor de erros para problem+json
├── domain/       # entidades JPA
├── repository/   # consultas, com as agregações feitas no banco
├── service/      # regras de negócio; resumo/ tem os números de Gestão e Relatório
├── security/     # sessão em cookie, CSRF e carga do usuário pelo CPF
└── config/       # relógio injetável e o fallback de SPA
```

O `WebConfig` devolve o `index.html` para as rotas do Angular (`/gestao`, `/transacoes/**`...).
A lista é explícita: um padrão genérico passaria na frente dos arquivos estáticos. `/hoje` e
`/dashboard`, nomes antigos da tela principal, redirecionam para `/gestao`.

## Pré-requisitos

- Java 21
- SQL Server 2019+ acessível (instância local ou via `docker compose up -d` na raiz do repo)
- Para empacotar o front: nada além disso. O profile `front` baixa o próprio Node para
  `target/`.

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

O script carrega o `.env.local`, encerra uma instância anterior na porta 8080, compila backend e
front num JAR só e sobe a aplicação.

**Qualquer sistema, na mão:**

```bash
export FINCLI_DB_USER=fincli_app
export FINCLI_DB_PASSWORD='<sua-senha>'
export FINCLI_COOKIE_SECURE=false   # apenas em dev, sobre http://localhost

mvn package -Pfront -DskipTests
java -jar target/fincli-backend-1.0.0.jar
```

Em ambos os casos a aplicação sobe em `http://localhost:8080` — abra no navegador.
Pare com `Ctrl+C`. Sem `-Pfront`, o JAR sobe só com a API.

Para trabalhar no front com recarga automática, deixe o backend no ar e rode `npm start` em
`frontend/`: o `ng serve` abre em `http://localhost:4200` e repassa `/api` para cá.

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

## Contrato da API

Sessão por cookie `HttpOnly` com CSRF habilitado ([ADR 0006](../docs/adr/0006-sessao-em-cookie.md)).
O front chama `GET /api/csrf` para receber o cookie `XSRF-TOKEN` e o reenvia no header
`X-XSRF-TOKEN` em toda mutação. Tudo em `/api/**` exige sessão, exceto cadastro, login e csrf.

| Método | Rota | Respostas |
|---|---|---|
| `POST` | `/api/cadastro` | 201 · 400 CPF/senha inválidos · 409 CPF já existe |
| `POST` | `/api/login` | 200 · 401 |
| `POST` | `/api/logout` | 204 |
| `GET` | `/api/me` | 200 · 401 |
| `GET` | `/api/csrf` | 204 (emite o cookie) |
| `GET` | `/api/hoje` | 200 — números da tela Gestão, sempre do mês corrente |
| `GET` | `/api/relatorio?mes=yyyy-MM` | 200 |
| `POST` | `/api/comando` | 200 · 400 · 422 texto que não vira lançamento |
| `GET` | `/api/previstas?mes=yyyy-MM` | 200 |
| `POST` | `/api/previstas` | 201 · 400 |
| `DELETE` | `/api/previstas/{id}` | 204 · 404 |
| `GET` | `/api/saldo` | 200 — `{ "saldoDisponivel": número }` |
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
mvn test              # regras de negócio e contrato HTTP, sem banco
mvn checkstyle:check  # estilo (Google Java Style)
./smoke-test.sh       # 22 checks contra a aplicação rodando, ponta a ponta
```

O `smoke-test.sh` exercita o fluxo real: cadastro, login, alocação seguida de transação
(o cenário que corrompia dados na versão CLI), regras de saldo, isolamento entre usuários,
extrato e logout.

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
- **A gramática da barra de comando existe em duas cópias:** `ComandoParser.java` decide e
  grava; `frontend/.../gramatica.ts` só desenha o preview. Os testes das duas se espelham.

## Pendências

- Recuperação de senha ([ADR 0007](../docs/adr/0007-cpf-e-senha.md)). A coluna `email` já existe
  e é opcional, mas não há fluxo. **Esta é a decisão que ainda bloqueia o cadastro em produção.**
- Testes de repositório com Testcontainers ([ADR 0008](../docs/adr/0008-persistencia.md)).
- Bloqueio por tentativas repetidas de login.
