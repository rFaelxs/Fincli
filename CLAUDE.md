# FinCLI — Contexto do Projeto

CLI de gestão financeira pessoal em Java, com dois modos de operação: **CLI** (persistência JSON local) e **API REST** (PostgreSQL via Supabase + deploy no Render).

Leia este arquivo antes de qualquer tarefa neste repositório.

---

## Stack técnica

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

> Spring Boot está fora do escopo atual.

---

## Arquitetura

Padrão **Command + Repository + Service** em camadas:

```
src/main/java/com/rfaelxs/
├── Main.java
├── api/           ← ApiServer (Javalin) — modo API REST
├── command/       ← CommandHandler — modo CLI
├── config/        ← GsonConfig, DatabaseConfig
├── exception/     ← ValorInvalidoException
├── model/         ← Transacao, Reserva, MovimentacaoReserva, DadosUsuario, User
├── repository/    ← IUsuarioRepository, UsuarioRepository (JSON), UsuarioRepositoryDb (PG), SelicRepository
└── service/       ← TransacaoService, ReservaService, DashboardService, UserService
```

### Dois modos de execução

- **CLI** (padrão): persiste em JSON local em `~/.fincli/`
- **API REST**: ativa com `API_MODE=true` + `DATABASE_URL` configurada; usa PostgreSQL

O `Main.java` decide o modo pelo env var `API_MODE`.

### Modelo de transação

```java
// Modelo unificado — não há Gasto e Receita separados
public class Transacao {
    private String id;           // UUID
    private String userId;       // UUID do dono
    private TipoTransacao tipo;  // ENTRADA | SAIDA
    private String descricao;
    private BigDecimal valor;
    private LocalDate data;
    private String categoria;
    private boolean essencial;
}
```

---

## Requisitos funcionais

| RF | Descrição | Status |
|---|---|---|
| RF01 | Adicionar transação (gasto ou receita) | ✅ |
| RF02 | Listar transações com filtros (tipo, período, categoria) | ✅ |
| RF03 | Resumo financeiro (saldo, total gastos, total receitas) | ✅ |
| RF04 | Suporte a múltiplos usuários via UUID | ✅ |
| RF05 | Reservas financeiras: metas, alocar, sacar, Reserva de Emergência protegida | ✅ |
| RF06 | Integração com API BCB para taxa Selic atual | ✅ |
| RF07 | Persistência em JSON local via Gson | ✅ |
| RF08 | Exportação de relatório (texto formatado) | 🔄 em andamento |

---

## Convenções de código

### Nomenclatura
- Classes: PascalCase
- Métodos e variáveis: camelCase
- Constantes: UPPER_SNAKE_CASE
- Pacotes: lowercase sem separador

### Estilo obrigatório (Checkstyle — Google Java Style)
```java
// Chaves na mesma linha
if (condition) {
    doSomething();
}

// Imports organizados: java.* → libs → projeto
import java.util.List;
import com.google.gson.Gson;
import com.rfaelxs.model.Transacao;

// Sem wildcard imports — ERRADO: import java.util.*;
```

### BigDecimal para dinheiro — sempre
```java
// CERTO
BigDecimal valor = new BigDecimal("150.00");
BigDecimal total = valor.add(outro).setScale(2, RoundingMode.HALF_UP);

// ERRADO — perda de precisão
double valor = 150.0;
```

---

## Persistência

### Modo CLI (JSON local)
```
~/.fincli/
  users.json
  transactions.json
  emergency-fund.json
```
- Nunca acessar JSON diretamente fora das classes Repository
- Nunca serializar `LocalDate` diretamente — usar adapter Gson registrado (`GsonConfig`)
- IDs sempre gerados com `UUID.randomUUID().toString()`

### Modo API REST (PostgreSQL)
- Schema em `schema.sql` — executar no SQL Editor do Supabase para criar as tabelas
- `UsuarioRepositoryDb` substitui `UsuarioRepository` quando `DATABASE_URL` está configurada
- `DatabaseConfig` gerencia a conexão JDBC

---

## Integração BCB Selic API

Endpoint: `https://api.bcb.gov.br/dados/serie/bcdata.sgs.11/dados/ultimos/1?formato=json`

- `SelicRepository` faz a chamada HTTP; `DashboardService` consome o resultado
- Cache em memória por sessão — não chamar a cada operação
- Tratar falha de rede com fallback — não bloquear o fluxo principal

---

## Testes

### O que testar
- `TransacaoService`: toda lógica de negócio com teste unitário
- `ReservaService`: cenários de alocação, saque e Reserva de Emergência
- `DashboardService`: cálculo de saldo, totais e progresso
- `SelicRepository`: mock do HTTP client + fallback

### Estrutura de teste (padrão AAA)
```java
@Test
@DisplayName("deve calcular saldo corretamente com receitas e gastos mistos")
void shouldCalculateBalanceCorrectly() {
    // given
    // when
    // then
}
```
- Um assert por teste quando possível
- Nomes de teste descritivos em inglês
- `@BeforeEach` para setup de repositório em memória

---

## Comandos úteis

### Executar
```bash
# Modo CLI (JSON local)
mvn exec:java -Dexec.mainClass="com.rfaelxs.Main"

# Modo API REST
export DATABASE_URL="jdbc:postgresql://..."
export API_MODE=true
mvn exec:java -Dexec.mainClass="com.rfaelxs.Main"

# Via JAR
java -jar target/fincli-1.0.0.jar           # CLI
java -jar target/fincli-1.0.0.jar --api     # API
```

### Testes e qualidade
```bash
mvn test                                    # todos os testes
mvn test -Dtest=TransacaoServiceTest        # classe específica
mvn checkstyle:check                        # verifica estilo (deve passar antes do commit)
mvn checkstyle:checkstyle                   # relatório sem falhar o build
```

### Build
```bash
mvn clean package                           # build completo (limpa, compila, testa, empacota)
mvn clean package -DskipTests              # JAR sem rodar testes
mvn clean                                   # limpa artefatos
```

### Git
```bash
git checkout -b feature/nome-da-funcionalidade
gh pr checks                               # status do pipeline (requer GitHub CLI)
grep -r "TODO\|FIXME\|System.out.println" --include="*.java" src/
```

---

## CI/CD

### GitHub Actions (ci.yml)
1. `mvn test` — falha se algum teste quebrar
2. `mvn checkstyle:check` — falha se o estilo estiver fora do padrão

**Nunca fazer merge com pipeline vermelho.**

### Deploy no Render
- `render.yaml` configura o serviço automaticamente
- Adicionar `DATABASE_URL` em `Environment > Add Environment Variable` no painel do Render
- Cada push na `master` dispara o pipeline e atualiza o deploy

---

## O que evitar

- **Nunca** usar `double` ou `float` para valores monetários
- **Nunca** acessar arquivos JSON fora das classes Repository
- **Nunca** colocar lógica de negócio no `CommandHandler` (ele só despacha)
- **Não** adicionar dependências sem justificativa — manter o projeto lean
- **Não** antecipar Spring Boot — arquitetura atual deve ficar limpa para migração futura

---

## Próximos passos

- Completar RF08 (exportação de relatório) → `vault/specs/RF08-exportacao-relatorio.md`
- Adicionar categoria obrigatória na criação de transação
- Implementar filtro por categoria na listagem
- Melhorar UX do CLI (menus numerados, confirmações)

---

## Vault Obsidian (Spec-Driven Development)

Localização: `../vault/`

O vault é a fonte de verdade para specs e decisões. Fluxo obrigatório:

```
1. Ler/criar spec em vault/specs/RFxx-nome.md
2. Alinhar com Claude antes de implementar
3. Implementar seguindo a spec
4. Atualizar spec com resultado e decisões tomadas
```

| Pasta | Conteúdo |
|---|---|
| `vault/specs/` | Uma spec por requisito funcional |
| `vault/decisions/` | ADRs — decisões arquiteturais com contexto |
| `vault/backlog/` | Itens planejados e status geral |
| `vault/sessions/` | Template e notas de cada sessão de trabalho |
| `vault/index.md` | Dashboard do projeto |

**Antes de qualquer implementação:** ler a spec correspondente em `vault/specs/`.
