# ADR 0008 — Persistência: SQL Server, BigDecimal e serviços sem estado

- **Status:** aceita
- **Data:** 2026-09-18
- **Origem:** `escopo-web.md` §1.2, §1.3, §1.4 e §2.3

## Contexto

O CLI gravava cada usuário num arquivo JSON reescrito por inteiro a cada operação, sem lock, sem transação e sem versão. Duas abas abertas já perderiam dados, e uma queda no meio da escrita corromperia a conta.

Os serviços do CLI guardavam uma cópia do usuário em campo. Alocar R$ 500 numa reserva e registrar qualquer transação em seguida apagava a alocação em silêncio.

O dinheiro era `double`, que acumula erro de ponto flutuante em somas de moeda.

## Decisão

- **Banco:** SQL Server, com o schema versionado pelo Flyway. O Hibernate só valida que as entidades batem com o banco.
- **Chaves:** `BIGINT IDENTITY` como chave clusterizada e um `UNIQUEIDENTIFIER public_id` exposto na API. Um GUID aleatório como chave clusterizada fragmentaria o índice.
- **Dinheiro:** `BigDecimal` com escala 2 e arredondamento `HALF_UP` no código; `DECIMAL(19,2)` no banco. Nunca `double`, nunca o tipo `MONEY`.
- **Serviços sem estado:** recebem o usuário por parâmetro e vão ao banco a cada operação.
- **Concorrência:** alocação, saque e exclusão de reserva travam a **linha do usuário** com `PESSIMISTIC_WRITE`, e o saldo disponível é lido dentro da transação, depois do lock. Travar só a reserva, como o backend antigo fazia, não basta: o saldo disponível é do usuário, e duas alocações simultâneas em reservas diferentes leriam o mesmo saldo e passariam juntas.
- **Movimentações sobrevivem à reserva:** `movimentacao_reserva.reserva_id` é opcional, com `ON DELETE SET NULL`. Excluída a reserva, o movimento continua no extrato com o nome gravado.
- **Datas no fuso de Brasília:** "hoje" vem de um `Clock` fixo em `America/Sao_Paulo`, não do fuso do servidor.
- **Agregações no banco:** totais do mês e saldo disponível são somados por consulta, não carregando a lista inteira em memória.

## Consequências

- O desenvolvimento precisa de um SQL Server: `docker compose up -d` na raiz sobe um.
- Credenciais nunca ficam no repositório; vêm de variáveis de ambiente.
- Os testes de serviço rodam sem banco, com mock. Testes de repositório contra banco real (Testcontainers) continuam pendentes, e passam a ser a única cobertura do filtro de transações por mês.
