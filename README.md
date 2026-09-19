# FinCLI

Gestão financeira pessoal na web. Registre entradas e saídas escrevendo o que aconteceu — `mercado 120 ontem` —, acompanhe quanto dá para gastar até o fim do mês e guarde dinheiro em reservas com meta.

## 🎯 Problema

Jovens adultos perdem o controle financeiro por falta de um registro simples e rápido. O FinCLI resolve isso com uma interface leve, sem distrações, que permite registrar e acompanhar as finanças em segundos.

## 👥 Público-alvo

Jovens adultos brasileiros (18–30 anos) que buscam acompanhar as próprias finanças de forma prática.

## ✨ Funcionalidades

- **Gestão**: quanto dá para gastar até o fim do mês, já descontadas as contas que ainda vencem e o que foi guardado
- **Barra de comando**: texto livre vira lançamento, com preview enquanto se digita e "desfazer"
- **Relatório**: composição do mês, sobra dos últimos 6 meses, ritmo das metas e projeção de fechamento
- **Transações**: lista com filtro por mês, tipo e texto; criação e edição
- **Reservas**: metas de poupança, alocação e saque, com a Reserva de Emergência protegida
- **Extrato**: transações e movimentações de reserva numa lista cronológica
- **Conta**: cadastro e login por CPF e senha, com sessão em cookie `HttpOnly`

## 📁 Estrutura

```
fincli/
├── backend/            # Spring Boot 3, Java 21, SQL Server, Flyway
├── frontend/           # Angular 22 (SPA), empacotado no JAR do backend
├── docs/
│   ├── specs/          # comportamento de cada tela, com critérios de aceite
│   └── adr/            # decisões de arquitetura
├── .github/workflows/  # CI com um job por projeto
└── docker-compose.yml  # SQL Server de desenvolvimento
```

## 🚀 Como executar

Pré-requisitos: **Java 21**, **Maven 3.9** e **Docker** (ou um SQL Server 2019+ acessível). Para desenvolver o front, **Node 24.15+**.

```bash
docker compose up -d        # SQL Server de desenvolvimento
```

Configure as credenciais e suba a aplicação seguindo o [backend/README.md](backend/README.md). O JAR serve API e front juntos em `http://localhost:8080`.

Para trabalhar no front com recarga automática, deixe o backend no ar e rode, em `frontend/`:

```bash
npm ci
npm start                   # http://localhost:4200, com /api repassado ao backend
```

## 🧪 Testes

```bash
cd backend
mvn test                    # regras de negócio e contrato HTTP, sem banco
mvn checkstyle:check        # estilo (Google Java Style)
./smoke-test.sh             # ponta a ponta, contra a aplicação rodando

cd ../frontend
npm test -- --watch=false   # formatação, gramática da barra de comando, serviços e telas
```

## 📚 Documentação

- [Specs](docs/specs/) — o que cada tela faz, com critérios de aceite
- [ADRs](docs/adr/) — por que o projeto é como é

## 👤 Autor

Rafael Siqueira — [@rFaelxs](https://github.com/rFaelxs)

## 🔗 Repositório

<https://github.com/rfaelxs/fincli>
