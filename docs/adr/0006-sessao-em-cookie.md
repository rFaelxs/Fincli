# ADR 0006 — Sessão em cookie HttpOnly com CSRF

- **Status:** aceita
- **Data:** 2026-09-18
- **Origem:** `escopo-web.md` §2.4

## Contexto

Um front no navegador precisa provar, a cada chamada, quem é o usuário. O padrão mais comum é guardar um token em `localStorage`, e também o mais explorado: qualquer XSS no front lê o token e leva a sessão embora.

## Decisão

A sessão vive num cookie `HttpOnly`, `SameSite=Lax` e `Secure`, que o JavaScript não consegue ler. O tempo de sessão é de 30 minutos.

Como o navegador envia o cookie sozinho, o CSRF fica habilitado:

- `GET /api/csrf` emite o cookie `XSRF-TOKEN`, legível pelo front;
- toda mutação reenvia o valor no header `X-XSRF-TOKEN`;
- o suporte a XSRF do `HttpClient` do Angular usa esses mesmos nomes por padrão.

Nenhum endpoint aceita o id do dono vindo do cliente: o usuário vem sempre da sessão. Recurso de outro usuário responde 404, nunca 403, para não revelar que ele existe.

O login descarta a sessão anterior antes de gravar a nova, contra fixação de sessão.

## Consequências

- O front chama `GET /api/csrf` e depois `GET /api/me`, silencioso, ao carregar.
- Um 401 em chamada comum leva ao login. Nas chamadas silenciosas, ele só significa "sem sessão".
- `FINCLI_COOKIE_SECURE=false` só em desenvolvimento, sobre `http://localhost`.
