# Spec — Transações

Lista, criação, edição e exclusão de entradas e saídas, com filtro por mês, tipo e texto.

**Cobre:** RF-04, RF-05 (ids do mapa das specs, **proposto**). RN-TRX-01 (**proposto**): valor de transação não pode ser negativo. RN-TRX-02 (**proposto**): transação de outro usuário responde 404, nunca 403.

## Telas e rotas

| Rota Angular | Tela | Acesso |
|---|---|---|
| `/transacoes` | Lista com filtros e total do período | Autenticada |
| `/transacoes/nova` | Nova transação | Autenticada |
| `/transacoes/:id/editar` | Editar transação | Autenticada |

## API

| Método e rota | Uso ou corpo | Respostas |
|---|---|---|
| `GET /api/transacoes` | Todas as transações do usuário, da mais recente para a mais antiga | 200 · 401 |
| `GET /api/transacoes/{id}` | Carrega a transação para edição | 200 · 404 |
| `POST /api/transacoes` | `{ valor, categoria, descricao, data, tipo, essencial }` | 201 · 400 |
| `PUT /api/transacoes/{id}` | Mesmo corpo do POST | 200 · 400 · 404 |
| `DELETE /api/transacoes/{id}` | Exclui a transação | 204 · 404 |

`data` em `yyyy-MM-dd`, `tipo` em `ENTRADA` ou `SAIDA`, `valor` com duas casas. O endpoint não aceita filtro: a lista vem inteira.

## Comportamento

### Lista

1. A tela carrega com `GET /api/transacoes` e aplica os filtros sobre a lista recebida.
2. O filtro de mês oferece os 6 últimos meses por extenso e começa no mês corrente. Não há opção "todos os meses".
3. O filtro de tipo oferece "Todos os tipos", "Entradas" e "Saídas".
4. O campo de busca casa com a descrição ou a categoria, ignorando maiúsculas, e filtra a cada tecla digitada.
5. Trocar qualquer filtro repinta a tabela. A lista é buscada ao abrir a tela e depois de cada exclusão.
6. O contador ao lado do título mostra "N lançamento(s)" com o total já filtrado.
7. As colunas são "Data", "Descrição", "Categoria", "Tipo", "Valor (R$)" e a coluna de ações.
8. A data aparece como `16/09`, sem o ano.
9. Descrição vazia aparece como um traço apagado.
10. O tipo aparece como chip "↑ entrada" ou "↓ saída"; saída essencial ganha também o chip "essencial".
11. O valor aparece com `+` em entrada e `−` em saída, com duas casas no formato pt-BR, sem o prefixo de moeda — a unidade está no cabeçalho da coluna.
12. O rodapé mostra "Total do período" com a soma das entradas menos as saídas do que está filtrado, com `+` ou `−`.
13. Sem resultado, a tabela mostra "Nenhuma transação encontrada." e o total vira um traço.
14. O botão "+ Nova transação" leva a `/transacoes/nova`.
15. Cada linha tem um link de editar, para `/transacoes/:id/editar`, e um botão de excluir.
16. Excluir pede confirmação com "Excluir esta transação?" e só então chama `DELETE`.
17. Exclusão bem-sucedida mostra o toast "Transação excluída." e recarrega a lista.
18. Erro em qualquer operação da lista aparece num toast com o `detail` do servidor.

### Criação

19. A tela de criação tem o título "Nova transação" e o subtítulo "Registre uma entrada ou saída.".
20. Os campos são "Descrição" (até 255 caracteres, exemplo "Ex.: Mercado do mês"), "Categoria" (obrigatória, até 80, exemplo "Ex.: Alimentação"), "Valor (R$)" (obrigatório, passo 0,01, mínimo 0), "Data" (obrigatória) e "Tipo".
21. "Tipo" traz "Saída" e "Entrada", com "Saída" pré-selecionada.
22. A caixa "Despesa essencial" nasce desmarcada.
23. A data nasce preenchida com a data local de hoje.
24. Descrição em branco é enviada como nula.
25. Os botões são "Cancelar", que volta para `/transacoes`, e "Salvar".
26. Sucesso substitui a rota por `/transacoes`, sem deixar o formulário no histórico.
27. Erro aparece num bloco fixo no topo do formulário, com o `detail` e as mensagens de campo, e o formulário continua preenchido.

### Edição

28. A tela de edição tem o título "Editar transação" e o subtítulo "Carregando…" enquanto busca a transação.
29. O formulário fica escondido até a carga terminar; então o subtítulo vira "Altere os campos e salve.".
30. Falha na carga mantém o formulário escondido e mostra a mensagem do servidor no lugar do subtítulo.
31. Rota sem id substitui a rota por `/transacoes`.
32. O botão de envio diz "Salvar alterações"; sucesso substitui a rota por `/transacoes`.

### Regras do servidor

33. Valor negativo responde 400 com "O valor da transação não pode ser negativo."; valor ausente, com `campos.valor` igual a "Informe o valor.".
34. Categoria em branco responde 400 com `campos.categoria` igual a "Informe a categoria."; acima de 80 caracteres, "A categoria deve ter no máximo 80 caracteres.".
35. Descrição acima de 255 caracteres responde 400 com "A descrição deve ter no máximo 255 caracteres.".
36. Data ausente responde 400 com "Informe a data."; tipo ausente, com "Informe o tipo (ENTRADA ou SAIDA).".
37. Transação inexistente, ou de outro usuário, responde 404 com "Transação não encontrada.".

## Critérios de aceite

- [x] CA-TRX-01: Dado um usuário com 12 transações em três meses, quando `/transacoes` carrega, então só as do mês corrente aparecem e o contador reflete esse número.
- [x] CA-TRX-02: Dado o filtro de tipo em "Entradas", quando a lista repinta, então nenhuma linha de saída aparece.
- [x] CA-TRX-03: Dada a busca "merc", quando a lista repinta, então aparecem as transações cuja descrição ou categoria contém "merc", em qualquer caixa.
- [x] CA-TRX-04: Dada uma saída de 120,00 em `2026-09-16` com descrição "Mercado" e categoria "Alimentação", quando a linha pinta, então mostra "16/09", "Mercado", "Alimentação", o chip "↓ saída" e "−120,00".
- [x] CA-TRX-05: Dada uma saída essencial, quando a linha pinta, então ela traz também o chip "essencial".
- [x] CA-TRX-06: Dada uma entrada essencial, quando a linha pinta, então o chip "essencial" não aparece.
- [x] CA-TRX-07: Dada uma transação com descrição nula, quando a linha pinta, então a célula mostra um traço apagado.
- [x] CA-TRX-08: Dadas entradas de 5.000,00 e saídas de 2.500,00 no filtro, quando o rodapé pinta, então o total mostra "+2.500,00".
- [x] CA-TRX-09: Dadas saídas maiores que as entradas no filtro, quando o rodapé pinta, então o total começa com "−".
- [x] CA-TRX-10: Dado um filtro sem resultado, quando a tabela pinta, então mostra "Nenhuma transação encontrada." e o total vira um traço.
- [x] CA-TRX-11: Dado o clique em excluir, quando o usuário cancela a confirmação, então nenhuma chamada é feita.
- [x] CA-TRX-12: Dado o clique em excluir confirmado, quando `DELETE` responde 204, então o toast "Transação excluída." aparece e a lista recarrega.
- [x] CA-TRX-13: Dada a tela de criação recém-aberta, quando ela monta, então a data traz o dia de hoje e o tipo está em "Saída".
- [x] CA-TRX-14: Dado o envio com descrição em branco, quando `POST /api/transacoes` é chamado, então `descricao` vai nula no corpo.
- [x] CA-TRX-15: Dado um envio válido, quando o servidor responde 201, então a aplicação substitui a rota por `/transacoes`.
- [x] CA-TRX-16: Dado um envio com valor −10, quando o servidor responde 400, então a mensagem "O valor da transação não pode ser negativo." aparece no topo do formulário e os campos continuam preenchidos.
- [x] CA-TRX-17: Dado um envio com categoria em branco, quando o servidor responde 400, então a mensagem exibida inclui "Informe a categoria.".
- [x] CA-TRX-18: Dada a rota `/transacoes/:id/editar` com um id válido, quando a carga termina, então o formulário aparece preenchido e o subtítulo vira "Altere os campos e salve.".
- [x] CA-TRX-19: Dada a rota de edição com um id inexistente, quando `GET /api/transacoes/{id}` responde 404, então o formulário continua escondido e "Transação não encontrada." aparece no lugar do subtítulo.
- [x] CA-TRX-20: Dado o id de uma transação de outro usuário, quando `GET /api/transacoes/{id}` é chamado, então a resposta é 404, nunca 403.
- [x] CA-TRX-21: Dada a edição de uma transação de 120,00 para 150,00, quando o envio conclui, então `PUT /api/transacoes/{id}` é chamado e a aplicação volta para `/transacoes`.
- [x] CA-TRX-22: Dado o clique em "Cancelar" na criação, quando a navegação acontece, então a aplicação vai para `/transacoes` sem chamar a API.

## Estrutura Angular

```
features/transacoes/
├── pages/
│   ├── lista-transacoes.page.ts     # rota /transacoes
│   ├── nova-transacao.page.ts       # rota /transacoes/nova
│   └── editar-transacao.page.ts     # rota /transacoes/:id/editar
├── components/
│   ├── filtros-transacoes.component.ts   # mês, tipo e busca
│   ├── tabela-transacoes.component.ts    # linhas, rodapé e ações
│   └── formulario-transacao.component.ts # usado por nova e editar
└── data/
    └── transacoes.service.ts        # GET, POST, PUT e DELETE de /api/transacoes
```

Reaproveita `shared/ui` (card, chip, estado vazio, toast), `shared/format` (moeda, diaMes, mesPorExtenso) e `core/api`. Componentes standalone com formulários reativos; estado em Signals; sem NgRx.

## Observação sobre o estado atual

- `GET /api/transacoes` devolve o histórico inteiro e os três filtros rodam no cliente. Com muitos lançamentos, a lista cresce sem paginação.
- O total do período é somado na tela, a partir dos valores já filtrados; é a única conta que o front faz com dinheiro.
- O filtro de mês não tem opção "todos", então não há como ver o histórico completo numa tela só.
- O front antigo preenchia a data com `valueAsDate = new Date()`, que usa a data UTC: depois das 21h no horário de Brasília, o campo nascia com o dia seguinte. O Angular usa a data local, como o item 23 descreve.
- A coluna de valor mostra o número sem "R$", diferente do resto do produto, porque a unidade está no cabeçalho.

## Propostas

- Aceitar `?mes=` e `?tipo=` em `GET /api/transacoes` e devolver o total do período junto, tirando do front a única soma de dinheiro que sobrou.
