# Spec — Gestão

A tela de abertura: quanto dá para gastar até o fim do mês, os três números do mês, os últimos lançamentos e as contas que ainda vencem.

**Cobre:** RF-11, RF-12, RF-13 (ids do mapa das specs, **proposto**). RN-01 a RN-08 (**proposto**), as fórmulas do resumo mensal, que ficam no servidor.

A tela se chamava "Hoje" e passa a se chamar "Gestão". O endpoint continua `GET /api/hoje`, e "hoje" como data não muda de nome em lugar nenhum.

## Telas e rotas

| Rota Angular | Tela | Acesso |
|---|---|---|
| `/gestao` | Gestão | Autenticada |

## API

| Método e rota | Uso ou corpo | Respostas |
|---|---|---|
| `GET /api/hoje` | Números do mês corrente; não aceita parâmetro de mês | 200 · 401 |
| `POST /api/previstas` | `{ nome, valor, diaVencimento, mesReferencia }`; usado só pelo "desfazer" de uma remoção | 201 · 400 |
| `DELETE /api/previstas/{id}` | Remove uma conta prevista | 204 · 404 |

`GET /api/hoje` devolve `hoje`, `mes`, `fimDoMes`, `diasRestantes`, `livre`, `porDia`, `entradas`, `saidas`, `essenciais`, `superfluas`, `aportes`, `sobra`, `previstasTotal`, `ultimosLancamentos` e `previstas`. Datas em `yyyy-MM-dd`, mês em `yyyy-MM`, valores com duas casas.

As fórmulas ficam no servidor e o front só exibe:

```
essenciais = soma das saídas marcadas essencial
superfluas = saidas − essenciais
aportes    = alocações − saques do mês
sobra      = entradas − saidas − aportes
previstas  = soma das contas ainda por vencer no mês
livre      = sobra − previstas
porDia     = livre / máx(diasRestantes, 1)
```

`diasRestantes` é o último dia do mês menos o dia de hoje, e vale 0 no último dia.

## Comportamento

1. A tela carrega com uma única chamada a `GET /api/hoje` e não recalcula nenhum dos valores recebidos.
2. Enquanto a chamada está em voo, o número grande e os três KPIs mostram "R$ 0,00" com o tratamento de esqueleto.
3. Com `livre` positivo, o rótulo do topo é "DÁ PRA GASTAR · ATÉ" seguido da data de `fimDoMes` por extenso em maiúsculas, por exemplo "DÁ PRA GASTAR · ATÉ 30 DE SETEMBRO".
4. O número grande mostra `livre` sem sinal, em BRL.
5. A pílula ao lado mostra `porDia` e os dias restantes: "R$ 84,50 por dia · 12 dias", com "1 dia" no singular.
6. Com `livre` positivo, o texto de apoio é "Já descontei as contas que ainda vencem no mês e o aporte das reservas. É dinheiro que você pode gastar sem estragar setembro.", com o nome do mês corrente no fim.
7. Com `livre` negativo, o rótulo vira "NO VERMELHO · ATÉ 30 DE SETEMBRO", a pílula mostra "R$ 300,00 no vermelho" e o número grande ganha o tratamento negativo.
8. Com `livre` negativo, o texto de apoio é "As contas que ainda vencem e o que já saiu passam do que entrou em setembro. Cortar aqui é escolher o que não vai acontecer.".
9. Com `entradas`, `saidas` e `aportes` todos iguais a zero, o rótulo vira "SEM LANÇAMENTOS AINDA", o número grande mostra "R$ 0,00", a pílula mostra "setembro está em branco", o texto vira "Lance o primeiro gasto na barra abaixo — é só escrever o que aconteceu." e a barra de comando recebe destaque.
10. Os três KPIs são "ENTROU", "SAIU" e "GUARDEI". "ENTROU" mostra `+` antes do valor, "SAIU" mostra `−`, e "GUARDEI" só mostra `−` quando `aportes` é negativo.
11. O medidor de "ENTROU" fica cheio quando há entradas e vazio quando não há. Os de "SAIU" e "GUARDEI" mostram a proporção sobre as entradas, limitada a 100%.
12. A seção "Últimos lançamentos" lista até 6 itens do mês, do mais recente para o mais antigo, com o link "ver o mês inteiro →" para `/relatorio`.
13. Cada linha mostra o dia no formato `16/09`, a descrição, a categoria, o chip "essencial" quando a transação é essencial e o valor.
14. Transação de entrada mostra `+` e de saída mostra `−`. Movimentação de reserva não leva sinal e ganha o chip "guardado" ou "sacado".
15. Cada valor carrega um rótulo só para leitor de tela: "entrada de", "saída de", "guardado" ou "sacado".
16. Sem lançamentos no mês, a lista mostra "Nenhum lançamento neste mês ainda.".
17. A seção de contas leva o título "Ainda vence em setembro", com o nome do mês de referência.
18. Cada conta prevista mostra o nome, o valor em BRL, "vence 22/09" e um botão de remover com rótulo acessível "Remover Energia".
19. Sem contas a vencer, a seção mostra "Nenhuma conta prevista para o resto do mês. Escreva `prever energia 210 dia 22` na barra para adicionar uma.".
20. Remover uma conta chama `DELETE /api/previstas/{id}`, mostra a faixa "Removido Energia" com a ação "desfazer" e recarrega a tela.
21. O "desfazer" de uma remoção recria a conta com `POST /api/previstas`, repetindo nome, valor e dia; `mesReferencia` vai nulo quando a conta era recorrente e com o mês da tela quando era avulsa.
22. Erro em qualquer operação aparece na faixa, em tratamento de erro, com o botão "tentar de novo", que recarrega a tela.
23. A tela tem um link de pulo para a barra de comando como primeiro elemento focável.
24. Uma conta com vencimento no dia 31 aparece no último dia dos meses mais curtos: 28 de fevereiro em ano comum.
25. A conta que vence hoje ainda conta como a vencer e continua descontada de `livre`.

## Critérios de aceite

- [ ] CA-GESTAO-01: Dado `livre` igual a 1.014,00 e `fimDoMes` igual a `2026-09-30`, quando a tela carrega, então o rótulo é "DÁ PRA GASTAR · ATÉ 30 DE SETEMBRO" e o número grande mostra "R$ 1.014,00".
- [ ] CA-GESTAO-02: Dado `porDia` igual a 84,50 e `diasRestantes` igual a 12, quando a tela carrega, então a pílula mostra "R$ 84,50 por dia · 12 dias".
- [ ] CA-GESTAO-03: Dado `diasRestantes` igual a 1, quando a tela carrega, então a pílula termina com "1 dia".
- [ ] CA-GESTAO-04: Dado `livre` igual a −300,00, quando a tela carrega, então o rótulo começa com "NO VERMELHO ·", a pílula mostra "R$ 300,00 no vermelho" e o número grande mostra "R$ 300,00" sem sinal.
- [ ] CA-GESTAO-05: Dado `entradas`, `saidas` e `aportes` iguais a 0,00, quando a tela carrega, então o rótulo é "SEM LANÇAMENTOS AINDA" e a pílula mostra "setembro está em branco".
- [ ] CA-GESTAO-06: Dado `entradas` 5.000,00 e `saidas` 2.500,00, quando a tela carrega, então "ENTROU" mostra "+R$ 5.000,00", "SAIU" mostra "−R$ 2.500,00" e o medidor de "SAIU" fica em 50%.
- [ ] CA-GESTAO-07: Dado `aportes` igual a −200,00, quando a tela carrega, então "GUARDEI" mostra "−R$ 200,00".
- [ ] CA-GESTAO-08: Dado `entradas` igual a 0,00 e `saidas` igual a 800,00, quando a tela carrega, então o medidor de "SAIU" fica em 0%.
- [ ] CA-GESTAO-09: Dado um lançamento de saída essencial de 120,00 em `2026-09-16` com descrição "Mercado", quando a lista pinta, então a linha mostra "16/09", "Mercado", o chip "essencial" e "−R$ 120,00".
- [ ] CA-GESTAO-10: Dado um lançamento de tipo `APORTE` de 300,00, quando a lista pinta, então o valor aparece sem sinal, com o chip "guardado" e o rótulo oculto "guardado".
- [ ] CA-GESTAO-11: Dado `ultimosLancamentos` vazio, quando a lista pinta, então mostra "Nenhum lançamento neste mês ainda.".
- [ ] CA-GESTAO-12: Dado um mês com 9 lançamentos, quando `GET /api/hoje` responde, então `ultimosLancamentos` traz 6 itens e a lista mostra 6 linhas.
- [ ] CA-GESTAO-13: Dada uma conta "Energia" de 210,00 que vence em `2026-09-22`, quando a seção pinta, então mostra "Energia", "R$ 210,00" e "vence 22/09".
- [ ] CA-GESTAO-14: Dado `previstas` vazio, quando a seção pinta, então o texto orienta a escrever `prever energia 210 dia 22` na barra.
- [ ] CA-GESTAO-15: Dado o clique em remover na conta "Energia", quando `DELETE /api/previstas/{id}` responde 204, então a faixa mostra "Removido Energia" com a ação "desfazer" e a tela recarrega.
- [ ] CA-GESTAO-16: Dada uma conta recorrente recém-removida, quando o usuário clica em "desfazer", então `POST /api/previstas` é chamado com `mesReferencia` nulo.
- [ ] CA-GESTAO-17: Dada uma conta avulsa de `2026-09` recém-removida, quando o usuário clica em "desfazer", então `POST /api/previstas` é chamado com `mesReferencia` igual a `2026-09`.
- [ ] CA-GESTAO-18: Dado que `GET /api/hoje` responde 500, quando a tela carrega, então a faixa de erro aparece com o botão "tentar de novo".
- [ ] CA-GESTAO-19: Dado o botão "tentar de novo", quando o usuário o aciona, então a tela refaz `GET /api/hoje` e some com a faixa.
- [ ] CA-GESTAO-20: Dado o primeiro `Tab` na tela, quando o foco entra, então o link "Pular para a barra de comando" é o elemento focado.
- [ ] CA-GESTAO-21: Dado hoje igual a `2026-09-30`, quando `GET /api/hoje` responde, então `diasRestantes` é 0 e `porDia` iguala `livre`.
- [ ] CA-GESTAO-22: Dada uma conta recorrente com dia 31 e o mês `2026-02`, quando a tela carrega, então o vencimento mostrado é 28/02.

## Estrutura Angular

```
features/gestao/
├── pages/
│   └── gestao.page.ts               # rota /gestao; um signal com a resposta de /api/hoje
├── components/
│   ├── heroi-livre.component.ts     # rótulo, número grande, pílula e texto de apoio
│   ├── kpis-do-mes.component.ts     # ENTROU, SAIU, GUARDEI e os três medidores
│   ├── lista-lancamentos.component.ts
│   └── contas-previstas.component.ts
└── data/
    ├── gestao.service.ts            # GET /api/hoje
    └── previstas.service.ts         # POST e DELETE /api/previstas
```

Reaproveita `shared/ui` (card, chip, medidor, faixa de aviso, estado vazio), `shared/format` (moeda, moedaAbs, diaMes, diaPorExtenso, soMes) e `core/api`. A barra de comando entra aqui como componente, especificada em [04-barra-de-comando.md](04-barra-de-comando.md). Componentes standalone; estado em Signals; sem NgRx.

## Observação sobre o estado atual

- `GET /api/hoje` não aceita mês: a tela é sempre do mês corrente, por decisão de produto.
- O "desfazer" guarda o último lançamento apenas em memória; recarregar a página perde a ação.
- A tela chama `GET /api/hoje` inteiro depois de cada operação, inclusive para atualizar uma única conta prevista.

## Propostas

- Expor o "desfazer" também por atalho de teclado, já que a faixa some junto com a navegação.
