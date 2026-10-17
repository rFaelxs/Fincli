# Spec — Extrato

Transações e movimentações de reserva na mesma lista cronológica, agrupadas por dia.

**Cobre:** RF-10 (id do mapa das specs, **proposto**). RN-EXT-01 (**proposto**): a lista junta as duas origens e ordena da mais recente para a mais antiga.

## Telas e rotas

| Rota Angular | Tela | Acesso |
|---|---|---|
| `/extrato` | Extrato | Autenticada |

## API

| Método e rota | Uso ou corpo | Respostas |
|---|---|---|
| `GET /api/extrato` | Histórico inteiro do usuário, sem filtro e sem paginação | 200 · 401 |

Cada item traz `origem` (`TRANSACAO` ou `RESERVA`), `data`, `valor`, `tipo`, `categoria` e `descricao`. Em transação, `tipo` é `ENTRADA` ou `SAIDA` e `descricao` é o texto livre. Em movimentação, `tipo` é `ALOCACAO` ou `SAQUE`, `categoria` é "Reserva" e `descricao` é o nome da reserva no momento do movimento.

A rota mora hoje no `DashboardController`, que faz a junção e a ordenação. A reescrita move a rota para um `ExtratoController`, com a lógica num `ExtratoService`, sem mudar o corpo da resposta.

## Comportamento

1. A tela carrega com uma chamada a `GET /api/extrato` e exibe os itens na ordem recebida. No mesmo dia, as transações vêm antes das movimentações de reserva.
2. O título é "Extrato" e o subtítulo, "transações e movimentações de reserva · cronológico".
3. Os itens são agrupados por dia, com um cabeçalho por data no formato "16 de set. de 2026".
4. Um novo cabeçalho aparece sempre que a data muda em relação ao item anterior.
5. Transação de entrada mostra o símbolo "↑"; de saída, "↓"; movimentação de reserva mostra "◎". Os símbolos são decorativos para leitor de tela.
6. Cada item mostra a descrição como título e, embaixo, o rótulo do tipo e a categoria separados por " · ".
7. Os rótulos de tipo são "Entrada", "Saída", "Alocação" e "Saque".
8. Item sem descrição mostra a categoria no lugar do título.
9. Transação mostra o valor com `+` em entrada e `−` em saída, em BRL.
10. Movimentação de reserva mostra o valor sem sinal: o dinheiro mudou de lugar, não entrou nem saiu.
11. Sem nenhum item, a tela mostra "Nada por aqui ainda.".
12. Erro no carregamento aparece num toast com o `detail` do servidor.
13. A lista não tem filtro, busca nem paginação.

## Critérios de aceite

- [ ] CA-EXT-01: Dada uma transação em `2026-09-16` e uma movimentação em `2026-09-18`, quando a tela carrega, então a movimentação aparece antes da transação.
- [ ] CA-EXT-02: Dadas duas transações em `2026-09-16`, quando a tela pinta, então elas ficam sob um único cabeçalho "16 de set. de 2026".
- [ ] CA-EXT-03: Dada uma entrada de 450,00 com descrição "Freela", quando o item pinta, então mostra "↑", "Freela", "Entrada · Trabalho" e "+R$ 450,00".
- [ ] CA-EXT-04: Dada uma saída de 120,00, quando o item pinta, então mostra "↓" e "−R$ 120,00".
- [ ] CA-EXT-05: Dada uma alocação de 300,00 na reserva "Viagem", quando o item pinta, então mostra "◎", "Viagem", "Alocação · Reserva" e "R$ 300,00" sem sinal.
- [ ] CA-EXT-06: Dado um saque de 300,00, quando o item pinta, então o rótulo é "Saque · Reserva" e o valor aparece sem sinal.
- [ ] CA-EXT-07: Dada uma transação com descrição nula e categoria "Alimentação", quando o item pinta, então o título mostra "Alimentação".
- [ ] CA-EXT-08: Dado um usuário sem nenhum lançamento, quando a tela carrega, então mostra "Nada por aqui ainda.".
- [ ] CA-EXT-09: Dado que `GET /api/extrato` responde 500, quando a tela carrega, então um toast mostra o `detail` e a lista fica vazia.
- [ ] CA-EXT-10: Dado um segundo usuário com lançamentos, quando `GET /api/extrato` é chamado pelo primeiro, então nenhum item do segundo aparece.
- [ ] CA-EXT-11: Dada uma reserva renomeada depois de uma alocação, quando o extrato é aberto, então o item mostra o nome gravado no momento do movimento.
- [ ] CA-EXT-12: Dado um aporte desfeito pela barra de comando, quando o extrato é aberto, então a alocação e o saque aparecem como dois itens.
- [ ] CA-EXT-13: Dadas uma transação e uma alocação, ambas em `2026-09-16`, quando `GET /api/extrato` responde, então a transação vem antes da alocação.

## Estrutura Angular

```
features/extrato/
├── extrato.ts                      # linhasDoExtrato(): cabeçalho do dia, símbolo, rótulo e sinal
├── pages/
│   ├── extrato.page.ts             # rota /extrato
│   └── extrato.page.css
└── data/
    └── extrato.service.ts          # GET /api/extrato
```

Os itens ficam direto no `.feed`, sem componente em volta de cada um: o CSS depende de `.feed-day:first-child` e `.item:last-child` numa lista plana. A lógica de apresentação fica numa função pura, testada à parte. Componentes standalone; estado em Signals; sem NgRx.


## Observação sobre o estado atual

- O endpoint devolve o histórico inteiro, sem filtro nem paginação: a resposta cresce sem limite com o tempo de uso.
- A ordenação usa só a data. O desempate vem de a ordenação ser estável: no mesmo dia, as transações ficam antes das movimentações, cada grupo na ordem do repositório. Não é um critério declarado, e mudar a junção muda a ordem.
- O cabeçalho de data é montado com `new Date(iso + 'T00:00:00')`; no Angular a data ISO é tratada como texto, sem conversão.

## Propostas

- Aceitar `?mes=` no endpoint e paginar, antes que o histórico torne a tela lenta.
