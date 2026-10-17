# Specs da reescrita — FinCLI

Oito specs, uma por módulo, escritas a partir do código que rodava antes da reescrita — o front em `backend/src/main/resources/static/**`, hoje removido, e os controllers e services. Elas descrevem o comportamento que o front Angular preserva, com o backend mantido e as regras de negócio intactas. O histórico do git guarda o front antigo para comparação.

| Spec | Rotas Angular | Endpoints principais |
|---|---|---|
| [01 — Shell e sessão](01-shell-e-sessao.md) | moldura de todas as rotas autenticadas | `/api/csrf`, `/api/me`, `/api/logout`, `/api/reservas` |
| [02 — Autenticação](02-autenticacao.md) | `/login`, `/cadastro` | `/api/cadastro`, `/api/login` |
| [03 — Gestão](03-gestao.md) | `/gestao` | `/api/hoje`, `/api/previstas` |
| [04 — Barra de comando](04-barra-de-comando.md) | `/gestao` | `/api/comando` |
| [05 — Relatório](05-relatorio.md) | `/relatorio` | `/api/relatorio` |
| [06 — Transações](06-transacoes.md) | `/transacoes` e subrotas | `/api/transacoes` |
| [07 — Reservas](07-reservas.md) | `/reservas` e subrotas | `/api/reservas`, `/api/saldo` |
| [08 — Extrato](08-extrato.md) | `/extrato` | `/api/extrato` |

Ordem sugerida de construção: shell e autenticação; depois transações, reservas e extrato, que são as telas mais simples; por fim Gestão, barra de comando e Relatório, que dependem de mais dados do servidor.

## Convenções que valem para todas

- **Nome da tela.** A tela antes chamada "Hoje" agora se chama "Gestão". "Hoje" como termo de data não muda, e o endpoint continua `GET /api/hoje`.
- **Cálculo.** O front não recalcula regra financeira: formata e exibe o que a API devolve.
- **Datas.** ISO `yyyy-MM-dd` tratadas como texto, nunca com `new Date(iso)`.
- **Dinheiro.** BRL em pt-BR; o sinal `+` ou `−` vem da tela, junto do rótulo.
- **Erros.** `application/problem+json`, com `detail` e, na validação, um objeto `campos` com a mensagem por campo.
- **Sessão.** Cookie `HttpOnly` com CSRF: `GET /api/csrf` emite o cookie `XSRF-TOKEN`, reenviado no header `X-XSRF-TOKEN` em toda mutação.

## Mudanças no contrato

| Endpoint | Situação |
|---|---|
| `GET /api/saldo` | **Novo.** Devolve `{ "saldoDisponivel": número }` para a tela de alocar. |
| `GET /api/dashboard` | Removido junto com a tela antiga; a leitura de saldo passa para `/api/saldo`. |
| `GET /api/extrato` | Muda de controller, sem mudar o corpo da resposta. |

As decisões que moldam estas specs — Angular, empacotamento no JAR, sessão em cookie, saída do CLI e da Selic — estão nos [ADRs](../adr/).

## Sobre os identificadores

Não existe documento de requisitos no repositório. Os ids **RF** vêm do mapa das specs da reescrita e os **RN** foram numerados aqui: todos estão marcados como **proposto** dentro de cada spec, e precisam ser conferidos contra o documento de requisitos antes de virarem referência.
