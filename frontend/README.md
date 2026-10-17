# FinCLI — frontend

Projeto Angular 22.1.8 gerado com o CLI 22.1.8: standalone, zoneless,
roteamento, CSS puro, TypeScript 6 em modo strict e Vitest com jsdom.
Sem SSR.

## Requisitos

- Node 24.15 ou mais novo.
- npm (o lockfile registra as versões instaladas).

## Desenvolvimento

Execute os comandos em `frontend/`:

```sh
npm ci
npm start
```

Abra `http://localhost:4200`. O servidor de desenvolvimento encaminha
`/api/**` para `http://localhost:8080` por meio de `proxy.conf.json`.
Execute o backend com `FINCLI_COOKIE_SECURE=false` no desenvolvimento local.

No PowerShell com scripts bloqueados, use `npm.cmd` no lugar de `npm`.

## Verificação e build

```sh
npm test
npm run build
```

Os testes rodam em Node com jsdom, sem navegador, e encerram ao concluir.
Para acompanhar alterações, use `npm test -- --watch`.
O build de produção fica em `dist/fincli/browser/`; a integração com o Maven
é responsabilidade do backend.

## Especificações

Os comportamentos das telas e os contratos estão em `../docs/specs/`,
e as decisões de arquitetura em `../docs/adr/`.
