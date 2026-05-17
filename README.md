# FinCLI

> **[⬇️ Download da última versão (JAR)](https://github.com/rFaelxs/fincli/releases/latest)**

CLI de gestão financeira pessoal em Java. Registre receitas, despesas e reservas financeiras, acompanhe seu saldo e visualize a taxa Selic em tempo real — tudo direto no terminal.

## 🎯 Problema

Jovens adultos perdem o controle financeiro por falta de um registro simples e rápido. O FinCLI resolve isso com uma interface leve, sem distrações, que permite registrar e acompanhar as finanças em segundos.

## 👥 Público-alvo

Jovens adultos brasileiros (18–30 anos) com familiaridade com o terminal que buscam acompanhar suas finanças de forma prática.

## ✨ Funcionalidades

- **Autenticação**: cadastro e login por CPF; dados isolados por usuário (UUID)
- **Transações**: adicionar, listar, editar e remover receitas/despesas por categoria
- **Reservas financeiras**: criar metas, alocar e sacar saldo, com Reserva de Emergência protegida
- **Dashboard**: saldo disponível, totais do mês, progresso da Reserva de Emergência e **taxa Selic atual** via API do Banco Central
- **Extrato**: histórico cronológico de transações e movimentações de reservas
- Persistência em JSON local por usuário (`data/{uuid}.json`)

## 🌐 Integração com API pública

A taxa Selic é obtida em tempo real da API pública do Banco Central do Brasil:

```
GET https://api.bcb.gov.br/dados/serie/bcdata.sgs.11/dados/ultimos/1?formato=json
```

Em caso de falha de rede, o dashboard exibe "Indisponível" sem interromper a aplicação.

## 🚀 Como executar

### Opção 1 — Download direto (sem compilar)

1. Acesse a página de [Releases](https://github.com/rFaelxs/fincli/releases/latest) e baixe `fincli-1.0.0.jar`
2. Execute:

```bash
java -jar fincli-1.0.0.jar
```

> Pré-requisito: **Java 21+** instalado. Verifique com `java -version`.

### Opção 2 — Compilar a partir do código-fonte

```bash
# Clone o repositório
git clone https://github.com/rfaelxs/fincli.git
cd fincli

# Compile e gere o fat JAR (com todas as dependências)
mvn clean package -DskipTests

# Execute
java -jar target/fincli-1.0.0.jar
```

> Pré-requisitos: **Java 21+** e **Maven 3.x**.

## 🗂️ Menu principal

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

## 🧪 Testes

```bash
mvn test               # Unit tests (Mockito) + Teste de integração (API BCB)
mvn checkstyle:check   # Verificar estilo (Google Java Style)
```

## 📁 Estrutura do projeto

```
src/
├── main/java/com/rfaelxs/
│   ├── Main.java
│   ├── config/        # GsonConfig (TypeAdapter de LocalDate)
│   ├── model/         # Transacao, Reserva, MovimentacaoReserva, DadosUsuario, User
│   ├── service/       # TransacaoService, ReservaService, DashboardService, UserService
│   ├── repository/    # UsuarioRepository, SelicRepository
│   └── command/       # CommandHandler
└── test/java/com/rfaelxs/
    ├── service/       # TransacaoServiceTest, ReservaServiceTest, DashboardServiceTest
    └── repository/    # SelicRepositoryIntegrationTest
data/
├── perfis.json        # Índice de usuários (login por CPF)
└── {uuid}.json        # Dados completos de cada usuário
```

## ⚙️ CI/CD

O pipeline roda automaticamente em todo push/PR para `master`, executando testes e checkstyle com JDK 21.

## 📌 Versão

1.0.0

## 👤 Autor

Rafael Siqueira — [@rFaelxs](https://github.com/rFaelxs)

## 🔗 Repositório

<https://github.com/rfaelxs/fincli>
