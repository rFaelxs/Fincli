# ADR 0003 — CLI fora do repositório

- **Status:** aceita
- **Data:** 2026-09-18

## Contexto

O repositório nasceu como um CLI de terminal: `src/` com `Main`, `CommandHandler`, modelos e serviços que guardavam dados em `data/{uuid}.json`. A versão web reaproveitou as regras de negócio e reescreveu a estrutura em `backend/`.

Desde então, as duas gerações conviviam na raiz. O `pom.xml` da raiz ainda construía o CLI, e o CI rodava `mvn test` nele — os testes do backend não rodavam no CI.

O CLI tinha ainda um bug de perda de dados: dois serviços carregavam cada um a própria cópia do usuário, e uma gravava por cima da outra (`escopo-web.md` §1.2). O backend corrigiu isso com serviços sem estado.

## Decisão

O CLI sai do repositório: `src/`, `pom.xml` e `dependency-reduced-pom.xml` da raiz, e `gasto.json`. O `data/` já não era versionado e continua ignorado.

O repositório fica com dois projetos independentes, `backend/` e `frontend/`, mais `docs/`.

## Consequências

- O CI passa a construir `backend/` e `frontend/`, cada um num job.
- O Checkstyle, que só estava configurado no pom da raiz, passa para `backend/pom.xml` com a mesma configuração.
- Os dados de quem usou o CLI não migram: a web começou do zero (`escopo-web.md` §2.5).
- O histórico do CLI continua no git, nos commits anteriores a esta decisão.
