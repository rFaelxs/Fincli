# ADR 0002 — Empacotamento monolítico

- **Status:** aceita
- **Data:** 2026-09-18

## Contexto

Com o front em Angular (ADR 0001), há dois jeitos de entregar: publicar o front separado do backend, ou empacotar o build do Angular dentro do JAR.

Publicar separado exige CORS com credenciais, porque a sessão vive num cookie `HttpOnly` (ADR 0006). Exige também dois deploys e duas configurações de origem. O `escopo-web.md` §2.2 já recomendava servir o front pelo próprio Spring.

## Decisão

O build do Angular é empacotado no JAR do backend. Um artefato só, servido de um host só.

- **Produção:** o Spring serve o `index.html` do Angular para as rotas do front, por um fallback de SPA. `/api/**` continua exigindo sessão.
- **Desenvolvimento:** `ng serve` com proxy de `/api` para `localhost:8080`. O navegador vê uma origem só, então não há CORS.

## Consequências

- O CORS sai do backend: a propriedade `fincli.cors.origens`, a variável `FINCLI_CORS_ORIGENS` e o bean de `SecurityConfig`.
- Se um dia o front for hospedado à parte, o CORS volta por um novo ADR.
- O build do JAR passa a depender do build do front. O `backend/pom.xml` roda o `npm` num profile próprio, para que os testes do backend continuem rodando sem Node.
