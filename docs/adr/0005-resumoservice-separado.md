# ADR 0005 — ResumoService separado por tela

- **Status:** aceita
- **Data:** 2026-09-18

## Contexto

O `ResumoService` calcula os números de duas telas, Gestão e Relatório. Com 428 linhas, ele concentra as duas, sete records aninhados e um enum.

As duas telas compartilham uma única peça: os totais de um mês — entradas, saídas, essenciais, supérfluas, aportes e sobra —, somados no banco. O resto é de uma tela só: o número livre e os últimos lançamentos são da Gestão; o histórico, as categorias, o ritmo das reservas e a projeção são do Relatório.

## Decisão

Separar em três classes, num pacote `service/resumo/`:

- `CalculadoraDeTotais`, com os totais por mês;
- `GestaoService`, com o que `GET /api/hoje` devolve;
- `RelatorioService`, com o que `GET /api/relatorio` devolve.

Os records aninhados passam para arquivos próprios no mesmo pacote.

## Consequências

- Nenhuma fórmula muda. Os testes de RN-01 a RN-08 são a rede: rodam antes e depois, com as mesmas asserções.
- O `ResumoServiceTest` se divide em `GestaoServiceTest` e `RelatorioServiceTest`.
- O `ResumoController` continua servindo as duas rotas, agora com um serviço para cada.
