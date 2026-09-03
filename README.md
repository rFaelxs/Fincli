# FinCLI

Gestão financeira pessoal em Java. Registre receitas, despesas e reservas, acompanhe seu saldo e veja a meta Selic em tempo real — **no terminal ou no navegador**.

O projeto tem duas aplicações que compartilham as mesmas regras de negócio:

| | Aplicação | Rodar | Dados em |
|---|---|---|---|
| 🖥️ | **CLI** — interface de terminal | `java -jar target/fincli-1.0.0.jar` | JSON local (`data/`) |
| 🌐 | **Web** — API REST + interface web | `backend\run-dev.ps1` → http://localhost:8080 | SQL Server |

> As duas mantêm bases separadas: uma conta criada no CLI não existe na Web, e vice-versa.

## 🎯 Problema

Jovens adultos perdem o controle financeiro por falta de um registro simples e rápido. O FinCLI resolve isso com uma interface leve, sem distrações, que permite registrar e acompanhar as finanças em segundos.

## 👥 Público-alvo

Jovens adultos brasileiros (18–30 anos) que buscam acompanhar suas finanças de forma prática.

## ✨ Funcionalidades

- **Autenticação**: cadastro e login por CPF; dados isolados por usuário
- **Transações**: adicionar, listar, editar e remover receitas/despesas por categoria
- **Reservas financeiras**: criar metas, alocar e sacar saldo, com Reserva de Emergência protegida
- **Dashboard**: saldo disponível, totais do mês, progresso da Reserva de Emergência e **meta Selic vigente (% a.a.)** via API do Banco Central
- **Extrato**: histórico cronológico de transações e movimentações de reservas

Exclusivo da versão Web:

- Senha com hash **BCrypt** e validação de CPF pelos dígitos verificadores
- Valores em `BigDecimal` / `DECIMAL(19,2)` — sem erro de ponto flutuante em moeda
- Busca e filtros nas transações; proporção **essenciais × supérfluas** no dashboard

## 🌐 Integração com API pública

A meta Selic é obtida em tempo real da API pública do Banco Central do Brasil (série SGS 432 — meta Selic definida pelo Copom, em % ao ano):

```
GET https://api.bcb.gov.br/dados/serie/bcdata.sgs.432/dados/ultimos/1?formato=json
```

> A série SGS 11 é a Selic **efetiva diária** (ex.: `0.051660` = 0,0517% ao dia) e não deve ser exibida como "taxa Selic atual". O raciocínio completo está em [`docs/spec-selic.md`](docs/spec-selic.md).

Em caso de falha de rede, o dashboard exibe "Indisponível" sem interromper a aplicação. Na versão Web o valor fica em cache de aplicação (TTL de 24h), com fallback para o último valor conhecido.

---

## 🌐 Executando a versão Web

**Pré-requisitos:** Java 21+, Maven 3.x e um **SQL Server** acessível (instância local ou `docker compose up -d` na raiz).

**1. Prepare o banco** (uma vez só):

```sql
CREATE DATABASE fincli;
GO
CREATE LOGIN fincli_app WITH PASSWORD = '<sua-senha>', DEFAULT_DATABASE = fincli;
GO
USE fincli;
CREATE USER fincli_app FOR LOGIN fincli_app;
ALTER ROLE db_owner ADD MEMBER fincli_app;   -- o Flyway cria e altera o schema
GO
```

O schema em si é criado pelo Flyway no primeiro start — não rode DDL à mão.

**2. Crie `backend\.env.local`** (já ignorado pelo git):

```
FINCLI_DB_USER=fincli_app
FINCLI_DB_PASSWORD=<sua-senha>
FINCLI_COOKIE_SECURE=false
```

**3. Suba:**

```powershell
cd backend
.\run-dev.ps1
```

Abra **http://localhost:8080**. Não há front separado para rodar — as páginas são servidas pelo próprio backend.

> **Não use `mvn spring-boot:run`.** Se o caminho do repositório tiver acento (como `Área de Trabalho`), o processo filho criado pelo plugin recebe o classpath corrompido e falha com `ClassNotFoundException`. Use `run-dev.ps1` ou `java -jar`.

Detalhes de configuração, contrato completo da API e estrutura do front: [`backend/README.md`](backend/README.md).

## 🖥️ Executando o CLI

```bash
mvn clean package -DskipTests
java -jar target/fincli-1.0.0.jar
```

Ou baixe o JAR pronto em [Releases](https://github.com/rFaelxs/fincli/releases/latest).

### Menu principal

```
[1] Dashboard
[2] Transações  →  Adicionar / Listar / Editar / Remover / Resumo por categoria
[3] Reservas    →  Criar / Listar / Alocar saldo / Sacar / Meta emergência / Excluir
[4] Extrato
[0] Sair
```

### Formatos de data aceitos

- `dd/MM/yyyy` (ex: 17/05/2026)
- `yyyy-MM-dd` (ex: 2026-05-17)
- `yyyy/MM/dd` (ex: 2026/05/17)

---

## 🧪 Testes

```bash
mvn test                    # CLI — 16 testes (Mockito + integração com a API do BCB)
mvn checkstyle:check        # Estilo (Google Java Style)

cd backend
mvn test                    # Web — 25 testes de regra de negócio, sem banco
./smoke-test.sh             # Web — 22 checks ponta a ponta, com a API no ar
```

O `smoke-test.sh` exercita o fluxo real contra o banco: cadastro, login, alocação seguida de transação, regras de saldo, isolamento entre usuários e logout. É idempotente — gera CPFs válidos aleatórios a cada execução.

## 📁 Estrutura do projeto

```
.
├── src/                      # 🖥️ CLI (Java puro + Gson)
│   ├── main/java/com/rfaelxs/
│   │   ├── Main.java · command/ · config/
│   │   ├── model/            # Transacao, Reserva, MovimentacaoReserva, DadosUsuario, User
│   │   ├── service/          # TransacaoService, ReservaService, DashboardService, UserService
│   │   └── repository/       # UsuarioRepository, SelicRepository
│   └── test/java/com/rfaelxs/
│
├── backend/                  # 🌐 Web (Spring Boot 3 + SQL Server)
│   ├── src/main/java/com/rfaelxs/web/
│   │   ├── domain/ · repository/ · service/ · api/ · security/ · config/
│   └── src/main/resources/
│       ├── db/migration/     # Flyway (V1__schema_inicial.sql)
│       └── static/           # front: uma pasta por recurso
│           ├── assets/       # base.css, api.js, shell.js (compartilhados)
│           ├── login/ · cadastro/ · dashboard/ · extrato/
│           ├── transacoes/   # index · create · update
│           └── reservas/     # index · create · update · movimentar
│
├── docs/
│   ├── spec-selic.md         # decisão da série SGS 432 e alternativas descartadas
│   └── escopo-web.md         # escopo da migração, decisões e pendências
│
├── data/                     # dados do CLI (JSON por usuário, fora do git)
└── docker-compose.yml        # SQL Server, para quem não tem instância local
```

## ⚠️ Limitação conhecida

**Não há recuperação de senha na versão Web.** O login é por CPF e não existe canal para provar identidade e redefinir a senha — quem esquecer perde o acesso à conta. A coluna `email` já existe no schema, opcional, reservada para esse fluxo. É a decisão em aberto que impede o uso em produção; o raciocínio e as alternativas estão em [`docs/escopo-web.md`](docs/escopo-web.md) §1.1.

## ⚙️ CI/CD

O pipeline roda em todo push/PR para `master`, executando testes e checkstyle com JDK 21.

> **Cobertura atual:** o workflow builda apenas o projeto raiz (CLI). Os 25 testes do `backend/` ainda não rodam na CI — precisam de um step próprio.

## 📌 Versão

1.0.0

## 👤 Autor

Rafael Siqueira — [@rFaelxs](https://github.com/rFaelxs)

## 🔗 Repositório

<https://github.com/rfaelxs/fincli>
