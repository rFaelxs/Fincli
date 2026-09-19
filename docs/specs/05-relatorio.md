# Spec — Relatório

Para onde foi o dinheiro do mês: a composição do fluxo, a sobra dos últimos seis meses e três perguntas respondidas com números.

**Cobre:** RF-14, RF-15, RF-16 (ids do mapa das specs, **proposto**). RN-01 a RN-08 (**proposto**), as mesmas fórmulas do resumo mensal, mais RN-REL-01 a RN-REL-03 (**proposto**): histórico de seis meses, ritmo das reservas em janela de três meses e projeção de fechamento.

## Telas e rotas

| Rota Angular | Tela | Acesso |
|---|---|---|
| `/relatorio` | Relatório | Autenticada |

## API

| Método e rota | Uso ou corpo | Respostas |
|---|---|---|
| `GET /api/relatorio?mes=yyyy-MM` | Números do mês escolhido; sem o parâmetro, o mês corrente | 200 · 401 |

A resposta traz `mes`, `entradas`, `saidas`, `essenciais`, `superfluas`, `aportes`, `sobra`, `historico`, `topCategorias`, `reservas` e `projecao`.

Regras que o servidor aplica e o front só exibe:

- `historico`: seis meses, do mais antigo ao mais recente, incluindo os sem movimento, com `sobra` zerada.
- `topCategorias`: até quatro categorias de saída do mês, da maior para a menor.
- `reservas[].aporteMedio`: alocações menos saques dos últimos três meses, divididos por 3.
- `reservas[].previsao`: mês em que a meta fecha no ritmo atual, em `yyyy-MM`; nulo quando não há meta ou o aporte médio não é positivo. Reserva que já bateu a meta recebe o próprio mês de referência.
- `reservas[].noRitmo`: falso para reserva sem meta ou sem aporte na janela.
- `projecao.jaSaiu` é o total de saídas do mês; `projecao.aindaVence` é a soma das contas ainda por vencer; `projecao.sobraPrevista` é `sobra − aindaVence`.

Em mês já fechado nada mais vence: `aindaVence` é 0,00 e `sobraPrevista` coincide com `sobra`.

## Comportamento

1. A tela carrega com uma chamada a `GET /api/relatorio` para o mês selecionado e não recalcula os valores recebidos.
2. O seletor de mês oferece os 12 últimos meses, do mais recente para o mais antigo, por extenso: "setembro de 2026". O rótulo acessível é "Mês do relatório".
3. Trocar o mês refaz a chamada e repinta a tela inteira.
4. O título fixo é "Para onde foi o dinheiro", e o rótulo acima dele mostra "RELATÓRIO · SETEMBRO DE 2026".
5. A barra de fluxo tem até quatro segmentos, proporcionais ao valor: "essenciais", "supérfluas", "reservas" e "sobra". Segmento de valor zero ou negativo não aparece.
6. "reservas" usa `aportes` e "sobra" usa `sobra`, ambos limitados a zero por baixo.
7. Segmento com menos de 12% da largura fica sem rótulo visível; o nome e o valor continuam no `title` e nos totais abaixo.
8. A barra é decorativa para leitor de tela, e o texto alternativo repete os pares nome e valor.
9. Sem nada lançado no mês, a barra vira um bloco único com "nada lançado neste mês".
10. Abaixo da barra ficam cinco totais: "ENTROU" com `+`, "ESSENCIAIS" e "SUPÉRFLUAS" com `−`, "GUARDEI" e "SOBRA" com `−` só quando negativos.
11. O gráfico "Sobra dos últimos 6 meses" desenha uma coluna por mês, com altura proporcional ao maior valor absoluto do período e mínimo de 2% para que o mês continue visível.
12. Mês com sobra negativa recebe o tratamento negativo e o rótulo ganha `−`.
13. O mês de referência da tela recebe destaque entre as colunas.
14. Os rótulos das colunas usam a forma curta: 3210 vira "3,2k"; abaixo de mil, o número inteiro.
15. Sob o gráfico aparecem as abreviações dos meses: "abr", "mai", "jun".
16. A seção "Perguntas do mês" traz três acordeões. O primeiro nasce aberto; os outros dois, fechados.
17. Cada acordeão expõe `aria-expanded` e `aria-controls`, e a seta alterna entre "▴" e "▾".
18. Ao abrir ou fechar, o foco continua no botão acionado.
19. A primeira pergunta é "O que puxou meu mês pra baixo?", com o destaque "R$ 800,00 em supérfluas".
20. Havendo saídas, o texto da primeira pergunta é "É 32% de tudo que saiu no mês. As categorias abaixo são as que mais pesaram.", com o percentual arredondado ao inteiro.
21. Sem saídas no mês, o texto é "Nada saiu neste mês, então não há o que explicar.".
22. As linhas da primeira pergunta são as categorias de `topCategorias`, com barra proporcional à maior delas.
23. A segunda pergunta é "Estou no ritmo das minhas metas?", com o destaque "2 de 3 no ritmo", contando só reservas com meta maior que zero.
24. Sem nenhuma reserva com meta, o destaque é "nenhuma meta ainda" e o texto "Defina uma meta em Reservas para acompanhar o ritmo por aqui.".
25. Havendo metas, o texto é "A previsão usa o aporte médio dos últimos 3 meses. Reserva parada não recebe previsão — sem aporte não há como estimar quando ela fecha.".
26. Cada linha da segunda pergunta mostra o nome da reserva, a barra no percentual de progresso limitado a 100, e "meta em set/27", "sem ritmo" ou "sem meta".
27. A terceira pergunta é "Como fecho o mês se seguir assim?", com o destaque "+R$ 500,00 livres" ou "−R$ 200,00 no vermelho".
28. O texto da terceira pergunta é "Projeção com o que já saiu e as contas que ainda vencem (R$ 210,00).", com o valor de `aindaVence`.
29. As linhas da terceira pergunta são "Já saiu", "Ainda vence" e "Sobra prevista", com barras proporcionais às entradas do mês.
30. Toda barra é limitada entre 0% e 100%.
31. Enquanto a chamada está em voo, a tela mantém o conteúdo anterior; erro mostra o `detail` num toast e não limpa a tela.

## Critérios de aceite

- [ ] CA-REL-01: Dado o mês `2026-09` no seletor, quando a tela carrega, então `GET /api/relatorio?mes=2026-09` é chamado e o rótulo mostra "RELATÓRIO · SETEMBRO DE 2026".
- [ ] CA-REL-02: Dado o seletor aberto, quando a lista pinta, então traz 12 opções, a primeira sendo o mês corrente por extenso.
- [ ] CA-REL-03: Dada a troca do mês para `2026-08`, quando a resposta chega, então a barra, o gráfico e as três perguntas são repintados com os dados de agosto.
- [ ] CA-REL-04: Dado `essenciais` 1.200,00, `superfluas` 800,00, `aportes` 300,00 e `sobra` 500,00, quando a barra pinta, então ela tem quatro segmentos com essas proporções.
- [ ] CA-REL-05: Dado `aportes` igual a −200,00, quando a barra pinta, então o segmento "reservas" não aparece.
- [ ] CA-REL-06: Dado um segmento com 5% da largura, quando a barra pinta, então ele fica sem rótulo visível e mantém o nome e o valor no `title`.
- [ ] CA-REL-07: Dados todos os totais em 0,00, quando a barra pinta, então mostra "nada lançado neste mês".
- [ ] CA-REL-08: Dado `sobra` igual a −150,00, quando os totais pintam, então "SOBRA" mostra "−R$ 150,00".
- [ ] CA-REL-09: Dado `entradas` igual a 5.000,00, quando os totais pintam, então "ENTROU" mostra "+R$ 5.000,00".
- [ ] CA-REL-10: Dado um histórico com sobras 400, −200, 0, 900, 100 e 300, quando o gráfico pinta, então a coluna de 900 é a mais alta e a de −200 recebe o tratamento negativo.
- [ ] CA-REL-11: Dado um mês do histórico com sobra 0,00, quando o gráfico pinta, então a coluna aparece com a altura mínima, não sumindo do eixo.
- [ ] CA-REL-12: Dada uma sobra de 3.210,00, quando o rótulo da coluna pinta, então mostra "3,2k".
- [ ] CA-REL-13: Dado um usuário sem movimento em nenhum dos seis meses, quando o gráfico pinta, então as seis colunas aparecem com rótulo "0".
- [ ] CA-REL-14: Dado o carregamento da tela, quando as perguntas pintam, então só a primeira está com `aria-expanded="true"`.
- [ ] CA-REL-15: Dado o clique no título da segunda pergunta, quando ela abre, então o foco permanece no botão dela.
- [ ] CA-REL-16: Dado `superfluas` 800,00 e `saidas` 2.500,00, quando a primeira pergunta pinta, então o destaque é "R$ 800,00 em supérfluas" e o texto começa com "É 32%".
- [ ] CA-REL-17: Dado `saidas` igual a 0,00, quando a primeira pergunta pinta, então o texto é "Nada saiu neste mês, então não há o que explicar.".
- [ ] CA-REL-18: Dadas cinco categorias de saída no mês, quando `GET /api/relatorio` responde, então `topCategorias` traz as quatro maiores.
- [ ] CA-REL-19: Dadas três reservas com meta e duas no ritmo, quando a segunda pergunta pinta, então o destaque é "2 de 3 no ritmo".
- [ ] CA-REL-20: Dado que nenhuma reserva tem meta, quando a segunda pergunta pinta, então o destaque é "nenhuma meta ainda".
- [ ] CA-REL-21: Dada uma reserva com meta 10.000,00, saldo 4.000,00 e aporte médio 500,00, quando o mês de referência é `2026-09`, então `previsao` é `2027-09` e a linha mostra "meta em set/27".
- [ ] CA-REL-22: Dada uma reserva com meta 5.000,00 e aporte médio 0,00, quando a segunda pergunta pinta, então a linha mostra "sem ritmo" e `noRitmo` é falso.
- [ ] CA-REL-23: Dada uma reserva sem meta, quando a segunda pergunta pinta, então a linha mostra "sem meta" e ela fica fora da contagem "N de M".
- [ ] CA-REL-24: Dada uma reserva com saldo maior ou igual à meta, quando o mês de referência é `2026-09`, então `previsao` é `2026-09` e `noRitmo` é verdadeiro.
- [ ] CA-REL-25: Dado `sobra` 710,00 e `aindaVence` 210,00, quando a terceira pergunta pinta, então o destaque é "+R$ 500,00 livres" e o texto termina com "(R$ 210,00).".
- [ ] CA-REL-26: Dada `sobraPrevista` igual a −200,00, quando a terceira pergunta pinta, então o destaque é "−R$ 200,00 no vermelho".
- [ ] CA-REL-27: Dado o mês `2026-07`, já fechado, quando `GET /api/relatorio` responde, então `aindaVence` é 0,00 e `sobraPrevista` iguala `sobra`.
- [ ] CA-REL-28: Dado que `GET /api/relatorio` responde 500, quando a tela tenta carregar, então um toast mostra o `detail` e o conteúdo anterior continua visível.

## Estrutura Angular

```
features/relatorio/
├── pages/
│   └── relatorio.page.ts               # rota /relatorio; signal do mês e da resposta
├── components/
│   ├── seletor-mes.component.ts        # 12 meses por extenso
│   ├── barra-fluxo.component.ts        # segmentos, rótulo condicional e texto alternativo
│   ├── totais-do-mes.component.ts
│   ├── historico-sobra.component.ts    # colunas proporcionais ao maior valor absoluto
│   └── pergunta.component.ts           # acordeão com linhas de barra
└── data/
    └── relatorio.service.ts            # GET /api/relatorio
```

Reaproveita `shared/ui` (card, medidor, estado vazio), `shared/format` (moeda, moedaAbs, moedaCurta, pct, mesPorExtenso, mesCurto, mesAno) e `core/api`. Componentes standalone; estado em Signals; sem NgRx.

## Observação sobre o estado atual

- O seletor mostra só os 12 últimos meses: não há como abrir um mês mais antigo, mesmo havendo dados.
- O estado de aberto dos três acordeões vive fora da tela: trocar de mês, ou sair e voltar ao Relatório, mantém cada pergunta como o usuário deixou.
- O percentual de supérfluas sobre as saídas é calculado na tela, a partir de dois números que a API devolve.

## Propostas

- Devolver o percentual de supérfluas no corpo da resposta, para que a tela não faça conta nenhuma.
