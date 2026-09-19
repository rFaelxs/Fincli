# ADR 0004 — Integração com a Selic descartada

- **Status:** descartada
- **Data:** 2026-09-18
- **Substitui:** `docs/spec-selic.md`

## Contexto

O dashboard antigo mostrava a meta Selic lida da API do Banco Central. A primeira versão usava a série SGS 11, a Selic efetiva diária, e exibia "0,05%" onde o usuário esperava cerca de 14% ao ano — a série errada, não um arredondamento.

A correção trocou para a série SGS 432, a meta anual do Copom, com o rótulo "Meta Selic" e a unidade explícita ("14,00% a.a."). O backend web manteve a integração com cache de aplicação de 24 horas e fallback para "Indisponível" quando o BCB estava fora.

Na mesma época, uma issue sugeriu trocar a API do BCB por um proxy de terceiros. A sugestão foi recusada: o dado já estava na fonte oficial, e o proxy não tinha origem verificável. Ficou a regra de que dados econômicos vêm da fonte primária oficial.

O passo seguinte proposto era o rendimento estimado da Reserva de Emergência a 100% do CDI. Ele não foi feito.

## Decisão

A Selic sai do produto. As telas Gestão e Relatório substituíram o dashboard, e nenhuma delas usa a taxa: a Selic sozinha é informativa, mas não diz nada sobre as finanças de quem olha.

Saem `SelicService`, as propriedades `fincli.selic.*` e o `spec-selic.md`.

## Consequências

- O backend deixa de depender de uma API externa, e a suíte de testes não precisa mais de rede.
- A regra de usar a fonte primária oficial continua valendo para qualquer dado econômico que voltar.
- Se o rendimento estimado da reserva voltar ao escopo, ele precisa de spec própria. As perguntas em aberto eram: bruto ou líquido de IR, CDI ou meta Selic como base, e cache persistente.
