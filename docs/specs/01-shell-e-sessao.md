# Spec — Shell e sessão

A moldura de toda rota autenticada: abre a sessão, monta a barra lateral, mostra o total guardado e trata o 401.

**Cobre:** RF-03, RF-19, RF-20 (ids do mapa das specs; não há documento de requisitos no repositório, então valem como **proposto**). RN: nenhuma regra financeira é decidida aqui.

## Telas e rotas

| Rota Angular | Tela | Acesso |
|---|---|---|
| `/` | Decide entre `/gestao` e `/login` conforme a sessão | Pública |
| `/dashboard` | Redireciona para `/gestao` | Pública |
| `/gestao` | Gestão, dentro do shell | Autenticada |
| `/relatorio` | Relatório, dentro do shell | Autenticada |
| `/transacoes` e subrotas | Transações, dentro do shell | Autenticada |
| `/reservas` e subrotas | Reservas, dentro do shell | Autenticada |
| `/extrato` | Extrato, dentro do shell | Autenticada |

## API

| Método e rota | Uso ou corpo | Respostas |
|---|---|---|
| `GET /api/csrf` | Emite o cookie `XSRF-TOKEN`; chamado antes de qualquer mutação | 204, sem corpo |
| `GET /api/me` | Identidade da sessão; chamada silenciosa no bootstrap | 200 `{ id, nome, cpf }` · 401 |
| `POST /api/logout` | Encerra a sessão e apaga `JSESSIONID` | 204 |
| `GET /api/reservas` | Alimenta o card "GUARDADO" | 200, lista de reservas |

`cpf` volta mascarado no formato `***.***.**NN`, só com os três últimos dígitos.

## Comportamento

1. No bootstrap de rota autenticada, o shell chama `GET /api/csrf` e depois `GET /api/me` em modo silencioso. Sem sessão, substitui a rota por `/login` sem deixar histórico.
2. Toda mutação envia o valor do cookie `XSRF-TOKEN` no header `X-XSRF-TOKEN`.
3. Um 401 em chamada não silenciosa manda o usuário para `/login` e interrompe o fluxo com a mensagem "Sessão expirada.".
4. A barra lateral lista, nesta ordem: "Gestão", "Relatório", "Transações", "Reservas" e "Extrato". O item da rota atual recebe `aria-current="page"`.
5. A marca no topo da barra leva a `/gestao` e tem rótulo acessível "FinCLI, ir para Gestão".
6. O card "GUARDADO" mostra a soma de `saldoAtual` de todas as reservas, em BRL.
7. A legenda do card mostra "em N reservas" — "em 1 reserva" no singular — e acrescenta " · emergência X,X%" só quando a Reserva de Emergência tem `metaValor` maior que zero.
8. Sem nenhuma reserva, a legenda mostra "nenhuma reserva ainda".
9. Enquanto o card carrega, o valor mostra "R$ 0,00" com o tratamento de esqueleto.
10. Falha ao carregar o card não derruba a tela: o valor vira "—" e a legenda "não consegui carregar".
11. O rodapé mostra o avatar com até duas iniciais do nome em maiúsculas, o nome do usuário, o mês corrente por extenso e o botão "Sair".
12. "Sair" chama `POST /api/logout` e vai para `/login`; erro na chamada é ignorado e a navegação acontece do mesmo jeito.
13. As teclas `G` e `R` navegam para `/gestao` e `/relatorio`. São ignoradas com `Ctrl`, `Meta` ou `Alt`, quando o foco está em campo de texto, e quando a rota já é a de destino.
14. Erros da API são lidos do `application/problem+json`: a tela mostra `detail` e, havendo `campos`, as mensagens de campo concatenadas. Sem corpo legível, mostra "Erro " seguido do status.
15. O toast de aviso tem `role="status"` e some sozinho depois de 3,2 segundos.

## Critérios de aceite

- [ ] CA-SHELL-01: Dado um navegador sem cookie de sessão, quando o usuário abre `/gestao`, então a aplicação chama `GET /api/me`, recebe 401 e substitui a rota por `/login`.
- [ ] CA-SHELL-02: Dado um usuário com sessão, quando o shell monta, então `GET /api/csrf` é chamado antes de `GET /api/me`.
- [ ] CA-SHELL-03: Dado o cookie `XSRF-TOKEN` com valor `abc123`, quando a tela dispara `POST /api/logout`, então a requisição leva o header `X-XSRF-TOKEN: abc123`.
- [ ] CA-SHELL-04: Dado um usuário na rota `/transacoes`, quando a barra lateral é montada, então só o item "Transações" tem `aria-current="page"`.
- [ ] CA-SHELL-05: Dado que `GET /api/reservas` devolve duas reservas com `saldoAtual` 1.200,00 e 300,00, quando o card carrega, então mostra "R$ 1.500,00" e a legenda começa com "em 2 reservas".
- [ ] CA-SHELL-06: Dado que o usuário tem só a Reserva de Emergência com `metaValor` 0, quando o card carrega, então a legenda é "em 1 reserva", sem o trecho de percentual.
- [ ] CA-SHELL-07: Dado que a Reserva de Emergência tem `metaValor` 10.000,00 e `progresso` 12,5, quando o card carrega, então a legenda termina com " · emergência 12,5%".
- [ ] CA-SHELL-08: Dado que `GET /api/reservas` falha com 500, quando o card carrega, então o valor mostra "—", a legenda "não consegui carregar" e o restante da tela continua funcionando.
- [ ] CA-SHELL-09: Dado o usuário "Rafael Siqueira", quando o rodapé é montado, então o avatar mostra "RS".
- [ ] CA-SHELL-10: Dado um usuário na rota `/relatorio`, quando ele pressiona `G`, então a aplicação navega para `/gestao`.
- [ ] CA-SHELL-11: Dado o foco dentro da barra de comando, quando o usuário digita a letra `g`, então nenhuma navegação acontece.
- [ ] CA-SHELL-12: Dado um usuário na rota `/gestao`, quando ele pressiona `G`, então nenhuma navegação é disparada.
- [ ] CA-SHELL-13: Dado que `POST /api/logout` responde 500, quando o usuário clica em "Sair", então a aplicação vai para `/login` mesmo assim.
- [ ] CA-SHELL-14: Dado um usuário com CPF `12345678901`, quando `GET /api/me` responde, então a tela recebe `***.***.**01` e nunca o CPF completo.
- [ ] CA-SHELL-15: Dado um erro 400 com `detail` "Verifique os campos enviados." e `campos` `{ "valor": "Informe o valor." }`, quando a tela lê o erro, então a mensagem exibida contém as duas frases.
- [ ] CA-SHELL-16: Dado o navegador em `/dashboard`, quando a rota resolve, então a aplicação redireciona para `/gestao`.

## Estrutura Angular

```
core/
├── api/
│   ├── modelos.ts             # corpos da API; datas e meses como texto
│   └── problema.ts            # lê problem+json: detail + campos
├── auth/
│   ├── sessao.service.ts      # iniciar() = csrf + me silencioso; signal do usuário; sair()
│   └── guardas.ts             # autenticadoGuard e publicoGuard
└── http/
    ├── silencioso.ts          # HttpContextToken das chamadas em que 401 é resposta
    └── nao-autorizado.interceptor.ts   # 401 → /login, exceto em chamadas silenciosas

layout/shell/
├── shell.component.ts         # moldura, link de pulo e atalhos G e R
├── components/
│   ├── barra-lateral.component.ts
│   ├── card-guardado.component.ts
│   └── rodape-usuario.component.ts
└── data/
    └── guardado.service.ts    # GET /api/reservas para o card; falha em silêncio

shared/
├── ui/                        # toast, faixa de aviso, medidor, erro de formulário
└── format/                    # formatos.ts (funções) e pipes.ts
```

O XSRF usa o suporte nativo do `HttpClient`, com os nomes do backend. Cada serviço de feature chama o `HttpClient` direto; não há cliente HTTP próprio por cima dele. O card "GUARDADO" recarrega a cada navegação concluída, como cada página do front antigo fazia ao abrir. Card, chip e estado vazio são classes do `base.css`, não componentes. Componentes standalone; estado em Signals; sem NgRx.


## Observação sobre o estado atual

- O atalho de teclado da tela principal é `H` hoje, inicial de "Hoje". Com o nome "Gestão" ele passa a `G`; o `R` do Relatório não muda.
- O card "GUARDADO" baixa a lista inteira de reservas só para somar `saldoAtual`. Não há endpoint de total.
- Cada página autenticada monta a própria barra lateral e refaz `GET /api/reservas`; no Angular o shell é montado uma vez por sessão de navegação.

## Propostas

- Recarregar o card "GUARDADO" por evento do serviço de reservas, em vez de cada tela chamar `carregarGuardado()` na mão.
