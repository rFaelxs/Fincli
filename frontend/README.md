# FinCLI Frontend

SPA em Angular 22 sobre a API do backend. Componentes standalone, estado em Signals, sem NgRx. As decisões estão no [ADR 0001](../docs/adr/0001-front-em-angular.md) e o comportamento de cada tela nas [specs](../docs/specs/).

## Pré-requisitos

- Node **24.15 ou mais novo** — o CLI do Angular 22 recusa versões anteriores.
- O backend rodando em `http://localhost:8080` (ver [backend/README.md](../backend/README.md)).

## Desenvolvimento

```bash
npm ci
npm start          # ng serve em http://localhost:4200
```

O `ng serve` repassa `/api` para `localhost:8080` pelo `proxy.conf.json`. O navegador vê uma origem só, então a sessão em cookie e o CSRF funcionam sem CORS.

## Testes e build

```bash
npm test -- --watch=false   # Vitest, sem navegador
npm run build              # saída em dist/fincli/browser
```

Em produção o build vai dentro do JAR do backend, que serve o `index.html` para as rotas do Angular ([ADR 0002](../docs/adr/0002-empacotamento-monolitico.md)).

## Estrutura

```
src/
├── app/
│   ├── core/        # modelos da API, leitura de erro, sessão, guardas e interceptor de 401
│   ├── layout/shell # barra lateral, card GUARDADO, atalhos G e R
│   ├── shared/      # formatação (dinheiro, datas) e componentes de interface
│   └── features/    # auth, gestao, relatorio, transacoes, reservas, extrato
└── styles/          # tokens.css (sem alteração) e base.css
```

Cada feature tem `pages/` (componentes de rota), `components/` (partes da tela) e `data/` (acesso à API), e é carregada sob demanda.

## Convenções

- O front não recalcula regra financeira: formata e exibe o que a API devolve.
- Datas ISO são tratadas como texto, nunca com `new Date(iso)`.
- Dinheiro em BRL pt-BR; o sinal (`+`/`−`) vem da tela, junto do rótulo.
- Cores, espaçamento e tipografia só por `var(--fin-*)`.
- A gramática da barra de comando existe também em `ComandoParser.java`. Ao mexer numa, mexa na outra e nos testes espelhados.
