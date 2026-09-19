# ADR 0001 — Front em Angular

- **Status:** aceita
- **Data:** 2026-09-18
- **Substitui:** a decisão de interface do `escopo-web.md` §2.2 (REST + MPA por recurso)

## Contexto

O front atual é um conjunto de páginas HTML, CSS e JS por recurso em `backend/src/main/resources/static/`, servidas pelo próprio Spring. Cada página monta a própria barra lateral, repete a guarda de sessão e tem o próprio script solto.

Com Gestão, Relatório e a barra de comando, o front passou a ter estado compartilhado entre telas — sessão, card "GUARDADO", faixa de desfazer — e regras de apresentação que se repetem. O MPA não tem onde pôr isso sem duplicar.

O contrato REST sempre foi a fronteira entre as duas pontas. O `escopo-web.md` já registrava que a MPA mantinha a porta aberta para um SPA sem tocar o backend.

## Decisão

Reescrever o front em Angular, num projeto próprio em `frontend/`, na versão estável vigente na criação do projeto.

- Componentes standalone e estado em Signals; sem NgRx nesta fase.
- Organização por feature (`features/<nome>/pages|components|data`), com o comum em `core/`, `layout/` e `shared/`.
- Uma rota-raiz por feature, carregada sob demanda.
- O front não recalcula regra financeira: formata e exibe o que a API devolve.

A funcionalidade de cada tela é preservada ao pé da letra; as specs em `docs/specs/` citam os textos e comportamentos a manter.

## Versão

Angular **22.1.8**, a estável na criação do projeto (2026-09-18), com:

- Node **24.15 ou mais novo** — o CLI do Angular 22 recusa versões anteriores; o projeto nasceu com o 24.19 LTS;
- TypeScript 6, com `strict` ligado por padrão;
- aplicação **zoneless**, que casa com o estado em Signals;
- **Vitest** como executor de testes, padrão do Angular 22, rodando em Node sem navegador;
- sem SSR: o build é estático e servido pelo Spring (ADR 0002).

## Consequências

- Passa a existir um build de front, com dependências npm e um job de CI próprio.
- O backend continua o mesmo, com o contrato do `backend/README.md`; só `GET /api/dashboard` sai e `GET /api/saldo` entra.
- `backend/src/main/resources/static/**` é removido quando o Angular cobrir as oito telas, não antes.
- A gramática da barra de comando continua existindo em duas cópias, uma em Java e uma em TypeScript. A fonte única foi adiada.
