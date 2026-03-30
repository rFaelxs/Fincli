# FinCLI
CLI de gerenciamento de finanças pessoais feito em Java. Registre, liste e analise seus gastos por categoria direto no terminal.

## 🎯 Problema
Jovens adultos perdem o controle financeiro por falta de um registro simples e rápido dos gastos do dia a dia. O FinCLI resolve isso com uma interface de linha de comando leve, sem distrações, que permite registrar receitas e despesas em segundos.

## 👥 Público-alvo
Jovens adultos brasileiros (18–30 anos) que querem acompanhar suas finanças pessoais de forma prática, especialmente estudantes e profissionais iniciantes que já têm familiaridade com o terminal.

## Funcionalidades
- Adicionar gastos com valor, categoria, descrição e data
- Listar todos os gastos registrados
- Remover um gasto pelo ID
- Resumo de gastos agrupados por categoria
- Persistência em JSON local (`gasto.json`)

## Pré-requisitos
- Java 21+
- Maven 3.x

## Instalação e execução
```bash
# Clone o repositório
git clone https://github.com/rfaelxs/fincli.git
cd fincli

# Compile e gere o JAR
mvn clean package

# Execute
java -cp target/fincli-1.0.0.jar com.rfaelxs.Main
```

## Menu
```
[1] Adicionar gasto
[2] Listar gastos
[3] Remover gasto
[4] Resumo por categoria
[0] Sair
```

### Formatos de data aceitos
- `yyyy-MM-dd` (ex: 2025-01-15)
- `yyyy/MM/dd` (ex: 2025/01/15)
- `dd/MM/yyyy` (ex: 15/01/2025)

## Estrutura do projeto
```
src/
├── main/java/com/rfaelxs/
│   ├── Main.java
│   ├── model/         # Gasto.java
│   ├── service/       # GastoService.java
│   ├── repository/    # GastoRepository.java
│   └── comand/        # CommandHandler.java
└── test/java/com/rfaelxs/
    └── AppTest.java
```

## Comandos úteis
```bash
mvn test               # Rodar testes
mvn checkstyle:check   # Verificar estilo de código
```

## CI
O pipeline roda automaticamente em todo push/PR para `master`, executando testes e checkstyle com JDK 21.

## 📌 Versão
1.0.0

## 👤 Autor
Rafael Siqueira — [@rFaelxs](https://github.com/rFaelxs)

## 🔗 Repositório
https://github.com/rfaelxs/fincli
