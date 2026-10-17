# Spec — Reservas

Metas de poupança: criar, acompanhar o progresso, alocar saldo disponível, sacar de volta, ajustar a meta e excluir.

**Cobre:** RF-06, RF-07, RF-08, RF-09 (ids do mapa das specs, **proposto**). RN-09, o saldo disponível. RN-RES-01 a RN-RES-04 (**proposto**): Reserva de Emergência protegida, alocação limitada ao saldo disponível, saque limitado ao saldo da reserva, exclusão só com saldo zerado.

## Telas e rotas

| Rota Angular | Tela | Acesso |
|---|---|---|
| `/reservas` | Lista de reservas | Autenticada |
| `/reservas/nova` | Nova reserva | Autenticada |
| `/reservas/:id/meta` | Editar meta | Autenticada |
| `/reservas/:id/alocar` | Alocar na reserva | Autenticada |
| `/reservas/:id/sacar` | Sacar da reserva | Autenticada |

## API

| Método e rota | Uso ou corpo | Respostas |
|---|---|---|
| `GET /api/reservas` | Lista, com a de emergência primeiro e as demais por nome | 200 · 401 |
| `GET /api/reservas/{id}` | Carrega a reserva para meta, alocação e saque | 200 · 404 |
| `POST /api/reservas` | `{ nome, metaValor }` | 201 · 400 |
| `POST /api/reservas/{id}/alocar` | `{ valor }` | 200 · 400 · 404 |
| `POST /api/reservas/{id}/sacar` | `{ valor }` | 200 · 400 · 404 |
| `PUT /api/reservas/{id}/meta` | `{ valor }` | 200 · 400 · 404 |
| `DELETE /api/reservas/{id}` | Exclui a reserva | 204 · 404 · 409 |
| `GET /api/saldo` **(novo)** | `{ "saldoDisponivel": número }` para a tela de alocar | 200 · 401 |

Cada reserva traz `id`, `nome`, `metaValor`, `saldoAtual`, `progresso` e `emergencia`. `progresso` vem do servidor com uma casa decimal e é 0,0 quando não há meta.

`GET /api/saldo` substitui a leitura de `saldoDisponivel` que a tela de alocar faz hoje em `GET /api/dashboard`, e reaproveita o mesmo cálculo: entradas menos saídas menos o total já alocado em reservas.

## Comportamento

### Lista

1. A tela carrega com `GET /api/reservas`. A Reserva de Emergência vem primeiro; as demais, por nome.
2. O subtítulo mostra "R$ 1.500,00 guardados no total", com a soma de `saldoAtual` de todas as reservas.
3. Cada card mostra o nome, o progresso em percentual com uma casa, e "R$ 1.200,00 / R$ 10.000,00" com saldo e meta.
4. O medidor do card é limitado a 100%, mesmo quando o progresso passa disso.
5. A Reserva de Emergência mostra o chip "protegida" no lugar do botão de excluir.
6. Cada card tem as ações "Alocar", "Sacar" e "Meta", que levam às rotas correspondentes.
7. O botão "+ Nova reserva" leva a `/reservas/nova`.
8. Excluir pede confirmação com "Excluir esta reserva?" e só então chama `DELETE`.
9. Exclusão bem-sucedida mostra o toast "Reserva excluída." e recarrega a lista.
10. Erro em qualquer operação da lista aparece num toast com o `detail` do servidor.
11. Sem reservas além da de emergência, a lista mostra apenas o card dela.

### Criação

12. A tela de criação tem o título "Nova reserva" e o subtítulo "Uma meta para guardar dinheiro com propósito.".
13. Os campos são "Nome" (obrigatório, até 120 caracteres, exemplo "Ex.: Viagem") e "Meta (R$)" (obrigatória, passo 0,01, mínimo 0).
14. Os botões são "Cancelar", que volta para `/reservas`, e "Criar".
15. Sucesso substitui a rota por `/reservas`.
16. Erro aparece num bloco fixo no topo do formulário, com o formulário ainda preenchido.

### Meta

17. A tela de meta tem o título "Editar meta" e o subtítulo "Carregando…" enquanto busca a reserva; depois, "Defina o novo valor-alvo.".
18. O resumo mostra "Reserva:" com o nome e "Guardado hoje:" com o saldo atual em BRL.
19. O campo "Nova meta (R$)" nasce preenchido com a meta vigente.
20. O botão diz "Salvar meta"; sucesso substitui a rota por `/reservas`.
21. Falha na carga mantém o formulário escondido e mostra a mensagem do servidor no lugar do subtítulo.

### Alocar e sacar

22. Em `/reservas/:id/alocar`, o título é "Alocar na reserva", o subtítulo "Move dinheiro do saldo disponível para a reserva." e o botão "Alocar".
23. Em `/reservas/:id/sacar`, o título é "Sacar da reserva", o subtítulo "Devolve dinheiro da reserva ao saldo disponível." e o botão "Sacar".
24. As duas telas mostram "Reserva:" e "Guardado hoje:" no resumo.
25. Só a tela de alocar mostra também "Saldo disponível:", lido de `GET /api/saldo`, para orientar antes do erro.
26. O campo "Valor (R$)" é obrigatório, tem passo 0,01 e mínimo 0,01.
27. Sucesso substitui a rota por `/reservas`; erro fica no bloco do topo do formulário.
28. Rota sem id substitui a rota por `/reservas`.

### Regras do servidor

29. Meta negativa na criação responde 400 com "A meta da reserva não pode ser negativa."; na edição, "A meta não pode ser negativa.".
30. Nome em branco responde 400 com `campos.nome` igual a "Informe o nome da reserva."; acima de 120 caracteres, "O nome deve ter no máximo 120 caracteres.".
31. Meta ausente responde 400 com `campos.metaValor` igual a "Informe a meta."; valor ausente em alocar, sacar ou meta, com `campos.valor` igual a "Informe o valor.".
32. Alocação de valor zero ou negativo responde 400 com "O valor de alocação deve ser positivo."; saque, com "O valor do saque deve ser positivo.".
33. Alocação acima do saldo disponível responde 400 com "Saldo insuficiente para esta alocação.".
34. Saque acima do saldo da reserva responde 400 com "Saldo da reserva insuficiente para este saque.".
35. Excluir a Reserva de Emergência responde 409 com "A Reserva de Emergência não pode ser excluída.".
36. Excluir reserva com saldo responde 409 com "A reserva ainda tem saldo. Saque o valor antes de excluí-la.".
37. Reserva inexistente, ou de outro usuário, responde 404 com "Reserva não encontrada.".
38. Alocação, saque e exclusão travam a linha do usuário, e o saldo disponível é lido dentro da transação, depois do lock. Assim, duas alocações simultâneas em reservas diferentes também não passam juntas do disponível (ADR 0008).
39. Alocação e saque gravam uma movimentação, que aparece no extrato e na lista de lançamentos da Gestão. Excluir a reserva não apaga as movimentações dela.

## Critérios de aceite

- [x] CA-RES-01: Dadas as reservas "Viagem", "Carro" e a de emergência, quando `/reservas` carrega, então a de emergência é a primeira e "Carro" vem antes de "Viagem".
- [x] CA-RES-02: Dadas duas reservas com saldos 1.200,00 e 300,00, quando a tela carrega, então o subtítulo mostra "R$ 1.500,00 guardados no total".
- [x] CA-RES-03: Dada uma reserva com saldo 1.250,00 e meta 10.000,00, quando o card pinta, então mostra "12,5%" e "R$ 1.250,00 / R$ 10.000,00".
- [x] CA-RES-04: Dada uma reserva com saldo 12.000,00 e meta 10.000,00, quando o card pinta, então o medidor fica em 100% e o percentual mostra "120,0%".
- [x] CA-RES-05: Dada uma reserva com meta 0,00, quando `GET /api/reservas` responde, então `progresso` é 0,0.
- [x] CA-RES-06: Dada a Reserva de Emergência, quando o card pinta, então mostra o chip "protegida" e nenhum botão de excluir.
- [x] CA-RES-07: Dado o clique em excluir, quando o usuário cancela a confirmação, então nenhuma chamada é feita.
- [x] CA-RES-08: Dada uma reserva com saldo 0,00, quando a exclusão é confirmada, então o servidor responde 204, o toast "Reserva excluída." aparece e a lista recarrega.
- [x] CA-RES-09: Dada uma reserva com saldo 300,00, quando a exclusão é confirmada, então o servidor responde 409 e o toast mostra "A reserva ainda tem saldo. Saque o valor antes de excluí-la.".
- [x] CA-RES-10: Dada a Reserva de Emergência, quando `DELETE /api/reservas/{id}` é chamado, então a resposta é 409 com "A Reserva de Emergência não pode ser excluída.".
- [x] CA-RES-11: Dado o nome "Viagem" e a meta 8.000,00, quando o envio conclui, então a reserva é criada e a aplicação volta para `/reservas`.
- [x] CA-RES-12: Dada a meta −1, quando o envio é feito, então o servidor responde 400 com "A meta da reserva não pode ser negativa." e a mensagem aparece no topo do formulário.
- [x] CA-RES-13: Dado o nome em branco, quando o envio é feito, então a resposta traz `campos.nome` igual a "Informe o nome da reserva.".
- [x] CA-RES-14: Dada a rota `/reservas/:id/meta` de uma reserva com meta 5.000,00, quando a carga termina, então o campo nasce com 5000,00 e o subtítulo vira "Defina o novo valor-alvo.".
- [x] CA-RES-15: Dada a nova meta 12.000,00, quando "Salvar meta" conclui, então `PUT /api/reservas/{id}/meta` é chamado e a aplicação volta para `/reservas`.
- [x] CA-RES-16: Dada a rota `/reservas/:id/alocar`, quando a tela carrega, então `GET /api/saldo` é chamado e "Saldo disponível:" aparece no resumo.
- [x] CA-RES-17: Dada a rota `/reservas/:id/sacar`, quando a tela carrega, então "Saldo disponível:" não aparece e `GET /api/saldo` não é chamado.
- [x] CA-RES-18: Dado um saldo disponível de 200,00, quando o usuário aloca 300,00, então o servidor responde 400 com "Saldo insuficiente para esta alocação." e a rota continua em `/reservas/:id/alocar`.
- [x] CA-RES-19: Dado um saldo disponível de 500,00, quando o usuário aloca 300,00, então o saldo da reserva sobe 300,00 e a aplicação volta para `/reservas`.
- [x] CA-RES-20: Dada uma reserva com saldo 100,00, quando o usuário saca 150,00, então o servidor responde 400 com "Saldo da reserva insuficiente para este saque.".
- [x] CA-RES-21: Dado o valor 0, quando o usuário tenta alocar, então o servidor responde 400 com "O valor de alocação deve ser positivo.".
- [x] CA-RES-22: Dada uma alocação de 300,00 concluída, quando o extrato é aberto, então existe um item de origem `RESERVA`, tipo `ALOCACAO`, com valor 300,00.
- [x] CA-RES-23: Dado o id de uma reserva de outro usuário, quando `GET /api/reservas/{id}` é chamado, então a resposta é 404 com "Reserva não encontrada.".
- [x] CA-RES-24: Dadas entradas de 5.000,00, saídas de 2.000,00 e 1.000,00 já alocados, quando `GET /api/saldo` responde, então `saldoDisponivel` é 2.000,00.
- [x] CA-RES-25: Dadas duas alocações simultâneas de 300,00 com saldo disponível de 500,00, quando as duas rodam, então só uma conclui e a outra responde 400.

## Estrutura Angular

```
features/reservas/
├── pages/
│   ├── lista-reservas.page.ts      # rota /reservas
│   ├── nova-reserva.page.ts        # rota /reservas/nova
│   ├── editar-meta.page.ts         # rota /reservas/:id/meta
│   └── movimentar.page.ts          # rotas /reservas/:id/alocar e /reservas/:id/sacar
├── components/
│   ├── card-reserva.component.ts   # nome, progresso, medidor e ações
│   └── resumo-reserva.component.ts # nome, guardado hoje e saldo disponível
└── data/
    ├── reservas.service.ts         # GET, POST, PUT e DELETE de /api/reservas
    └── saldo.service.ts            # GET /api/saldo
```

A tela de movimentar é uma só, parametrizada pela rota, como já acontece hoje. Reaproveita `shared/ui` (card, chip, medidor, toast), `shared/format` (moeda, pct) e `core/api`. Componentes standalone com formulários reativos; estado em Signals; sem NgRx.

## Observação sobre o estado atual

- A tela de alocar lê o saldo disponível de `GET /api/dashboard`, que carrega totais do mês, progresso da emergência e a meta Selic só para usar um número. `GET /api/saldo` existe para encerrar isso.
- O saldo disponível mostrado na tela é uma leitura anterior ao envio: o servidor relê o valor dentro da transação, com a reserva travada, e é essa leitura que decide.
- A lista não tem estado vazio próprio, porque todo usuário nasce com a Reserva de Emergência.
- A exclusão usa a confirmação nativa do navegador, sem o padrão visual do resto do produto.

## Propostas

- Trocar a confirmação nativa por um diálogo do próprio produto, alinhado à faixa de "desfazer" já usada na Gestão.
