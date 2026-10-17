# FinCLI

Gestão financeira pessoal na web: registre entradas e saídas, guarde dinheiro em reservas com meta e veja, logo ao abrir, quanto dá para gastar até o fim do mês.

> O projeto está sendo reescrito. O **backend** novo está completo; o **front em Angular** está em construção. O comportamento de cada tela está especificado em [`docs/specs/`](docs/specs/) e as decisões de arquitetura em [`docs/adr/`](docs/adr/).

## 🎯 Problema

Jovens adultos perdem o controle financeiro por falta de um registro simples e rápido. O FinCLI quer que lançar um gasto leve segundos: na barra de comando, `mercado 120 ontem` já vira uma saída de R$ 120,00 em Alimentação, com a data de ontem.

## ✨ Funcionalidades

- **Gestão:** quanto dá para gastar até o fim do mês, já descontadas as contas que ainda vencem e o que foi guardado; entradas, saídas e aportes do mês; últimos lançamentos.
- **Barra de comando:** texto livre vira lançamento — saída, entrada, aporte numa reserva ou conta prevista (`prever energia 210 dia 22`).
- **Transações:** entradas e saídas por categoria, com marcação de essencial.
- **Reservas:** metas de poupança, alocação limitada ao saldo disponível, saque e uma Reserva de Emergência protegida.
- **Relatório:** para onde foi o dinheiro do mês, a sobra dos últimos seis meses, o ritmo de cada meta e a projeção de fechamento.
- **Extrato:** transações e movimentações de reserva numa linha do tempo só.

## 🏗️ Arquitetura

| Parte | Tecnologia |
|---|---|
| Backend | Java 21, Spring Boot 3.3, Spring Security, Spring Data JPA |
| Banco | SQL Server, com o schema versionado pelo Flyway |
| Front | Angular 22, standalone e zoneless, estado em Signals |
| Testes | JUnit 5, Mockito, MockMvc e Testcontainers com SQL Server real |

Escolhas que moldam o código:

- **Sessão em cookie `HttpOnly` com CSRF**, e não token em `localStorage` ([ADR 0006](docs/adr/0006-sessao-em-cookie.md)).
- **Login por CPF e senha** com BCrypt; o CPF nunca volta nas respostas da API ([ADR 0007](docs/adr/0007-cpf-e-senha.md), [ADR 0009](docs/adr/0009-cpf-fora-das-respostas.md)).
- **Dinheiro em `BigDecimal` e `DECIMAL(19,2)`**, somas feitas no banco e lock por usuário nas operações de reserva ([ADR 0008](docs/adr/0008-persistencia.md)).
- **O front não recalcula regra financeira:** formata o que a API devolve.

## 🚀 Executando o backend

**Pré-requisitos:** Java 21, Maven 3.9 e um SQL Server. Sem instância local, suba um com Docker:

```powershell
$env:MSSQL_SA_PASSWORD = '<senha forte>'
docker compose up -d
```

**1. Crie o banco** (uma vez só). O schema em si é criado pelo Flyway no primeiro start:

```sql
CREATE DATABASE fincli;
GO
CREATE LOGIN fincli_app WITH PASSWORD = '<sua-senha>', DEFAULT_DATABASE = fincli;
GO
USE fincli;
CREATE USER fincli_app FOR LOGIN fincli_app;
ALTER ROLE db_owner ADD MEMBER fincli_app;
GO
```

**2. Defina as credenciais e suba:**

```powershell
$env:FINCLI_DB_USER = 'fincli_app'
$env:FINCLI_DB_PASSWORD = '<sua-senha>'
$env:FINCLI_COOKIE_SECURE = 'false'   # só em desenvolvimento, sobre http://localhost

cd backend
mvn -B package -DskipTests
java -jar target/fincli-backend-1.0.0.jar
```

A API sobe em **http://localhost:8080**.

> **Não use `mvn spring-boot:run`.** Se o caminho do repositório tiver acento (como `Área de Trabalho`), o processo filho criado pelo plugin recebe o classpath corrompido e falha com `ClassNotFoundException`. Use o `java -jar`.

| Variável | Uso | Padrão |
|---|---|---|
| `FINCLI_DB_URL` | JDBC do SQL Server | `localhost:1433`, banco `fincli` |
| `FINCLI_DB_USER` / `FINCLI_DB_PASSWORD` | credenciais do banco | obrigatórias |
| `FINCLI_COOKIE_SECURE` | cookie de sessão só em HTTPS | `true` |
| `FINCLI_PORT` | porta HTTP | `8080` |

## 🧪 Testes

```powershell
cd backend
mvn test              # testes unitários e de API, sem banco — segundos
mvn verify            # + testes de integração contra um SQL Server num container
mvn checkstyle:check  # Google Java Style
```

Os testes de integração (`*IT`) precisam do Docker rodando. Eles aplicam as migrations num SQL Server 2022 real e conferem as consultas agregadas, o isolamento entre usuários e duas alocações simultâneas que não podem passar juntas do saldo disponível.

O CI roda `mvn verify` e o Checkstyle em todo push e pull request para `master`.

## 📁 Estrutura

```
.
├── backend/                     # API REST (Spring Boot)
│   └── src/main/java/com/rfaelxs/web/
│       ├── api/                 # controllers, DTOs e o tratamento de erros (problem+json)
│       ├── domain/              # entidades JPA
│       ├── repository/          # Spring Data, com as consultas agregadas
│       ├── service/             # regras de negócio
│       │   ├── comando/         # gramática da barra de comando
│       │   └── resumo/          # números da Gestão e do Relatório
│       ├── security/            # sessão, CSRF e login
│       └── config/              # relógio da aplicação, no fuso de Brasília
├── frontend/                    # Angular (em construção)
├── docs/
│   ├── specs/                   # comportamento de cada tela e contrato da API
│   ├── adr/                     # decisões de arquitetura
│   └── escopo-web.md            # escopo original da migração para a web
└── docker-compose.yml           # SQL Server para desenvolvimento
```

## ⚠️ Limitação conhecida

**Não há recuperação de senha.** O login é por CPF, e ainda não existe canal para provar identidade e redefinir a senha: quem esquecer perde o acesso. A coluna `email` já existe, opcional, reservada para esse fluxo ([ADR 0007](docs/adr/0007-cpf-e-senha.md)).

## 👤 Autor

Rafael Siqueira — [@rFaelxs](https://github.com/rFaelxs)
