# FinCLI

CLI de gestão financeira pessoal em Java — com API REST, banco de dados em nuvem (Supabase) e deploy contínuo no Render.

## Integrantes

| Nome | Matrícula |
|------|-----------|
| Rafael Siqueira | — |

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Build | Maven |
| API REST | Javalin 6 |
| Banco de dados | PostgreSQL via Supabase |
| Serialização | Gson |
| Testes | JUnit 5 + Mockito |
| Estilo de código | Checkstyle (Google Java Style) |
| CI/CD | GitHub Actions + Render |
| API externa | BCB Selic API |

## Funcionalidades

- **Autenticação**: cadastro e login por CPF; dados isolados por usuário (UUID)
- **Transações**: adicionar, listar, editar e remover receitas/despesas por categoria
- **Reservas financeiras**: criar metas, alocar e sacar saldo, com Reserva de Emergência protegida
- **Dashboard**: saldo disponível, totais do mês, progresso da Reserva de Emergência e taxa Selic atual via API do Banco Central
- **Extrato**: histórico cronológico de transações e movimentações de reservas

## Configuração do banco de dados (Supabase)

1. Crie um projeto em [supabase.com](https://supabase.com)
2. Abra o **SQL Editor** e execute o conteúdo de [`schema.sql`](schema.sql)
3. Copie a **Connection String** JDBC em `Settings > Database > Connection string`

## Como executar localmente

### Modo CLI (persistência em JSON local, sem banco)

```bash
mvn exec:java -Dexec.mainClass="com.rfaelxs.Main"
```

### Modo API REST (requer Supabase configurado)

```bash
export DATABASE_URL="jdbc:postgresql://db.<projeto>.supabase.co:5432/postgres?user=postgres&password=<senha>&sslmode=require"
export API_MODE=true
mvn exec:java -Dexec.mainClass="com.rfaelxs.Main"
# API disponível em http://localhost:8080
```

### Via JAR

```bash
mvn clean package -DskipTests
java -jar target/fincli-1.0.0.jar           # modo CLI
java -jar target/fincli-1.0.0.jar --api     # modo API
```

## Endpoints da API

| Método | Rota | Descrição |
|--------|------|-----------|
| GET | `/health` | Health check |
| POST | `/api/auth/login` | `{"cpf":"..."}` |
| POST | `/api/auth/cadastro` | `{"nome":"...","cpf":"..."}` |
| GET | `/api/transacoes/{userId}` | Listar transações |
| POST | `/api/transacoes/{userId}` | Adicionar transação |
| PUT | `/api/transacoes/{userId}/{id}` | Editar transação |
| DELETE | `/api/transacoes/{userId}/{id}` | Remover transação |
| GET | `/api/reservas/{userId}` | Listar reservas |
| POST | `/api/reservas/{userId}` | Criar reserva |
| POST | `/api/reservas/{userId}/{id}/alocar` | `{"valor":X,"saldoDisponivel":Y}` |
| POST | `/api/reservas/{userId}/{id}/sacar` | `{"valor":X}` |
| DELETE | `/api/reservas/{userId}/{id}` | Excluir reserva |
| GET | `/api/dashboard/{userId}` | Dashboard financeiro |

**Payload de transação:**
```json
{
  "valor": 1500.00,
  "categoria": "Salário",
  "descricao": "Salário maio",
  "data": "2026-05-01",
  "tipo": "ENTRADA",
  "essencial": false
}
```

## Testes

```bash
mvn test                # unit tests (Mockito) + integração (API BCB)
mvn checkstyle:check    # Google Java Style
```

## Estrutura do projeto

```
src/
├── main/java/com/rfaelxs/
│   ├── Main.java
│   ├── api/           # ApiServer (Javalin)
│   ├── config/        # GsonConfig, DatabaseConfig
│   ├── model/         # Transacao, Reserva, MovimentacaoReserva, DadosUsuario, User
│   ├── service/       # TransacaoService, ReservaService, DashboardService, UserService
│   ├── repository/    # IUsuarioRepository, UsuarioRepository (JSON), UsuarioRepositoryDb (PG)
│   └── command/       # CommandHandler
└── test/java/com/rfaelxs/
    ├── service/       # TransacaoServiceTest, ReservaServiceTest, DashboardServiceTest
    └── repository/    # SelicRepositoryIntegrationTest
schema.sql              # DDL para criação das tabelas no Supabase
render.yaml             # Configuração de deploy automático no Render
```

## Deploy (Render)

1. Conecte o repositório ao Render — o `render.yaml` configura o serviço automaticamente
2. Adicione a variável `DATABASE_URL` no painel do Render (`Environment > Add Environment Variable`)
3. Cada push na `master` dispara o pipeline CI/CD e atualiza o deploy

## CI/CD

Pipeline via GitHub Actions (`.github/workflows/ci.yml`):
1. `mvn test` — falha se algum teste quebrar
2. `mvn checkstyle:check` — falha se o estilo estiver fora do padrão

## Fluxo de colaboração

```
Issue → branch feature/nome → desenvolvimento
  → PR aberto → Actions roda testes
      → revisão → merge na master
          → deploy atualizado automaticamente
```

## Autor

Rafael Siqueira — [@rFaelxs](https://github.com/rFaelxs)
