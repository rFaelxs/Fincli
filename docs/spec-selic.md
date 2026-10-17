# Spec — Integração Selic no Dashboard

Status: **etapa 1 concluída** · Próxima etapa: proposta abaixo (aguardando decisão)

## 1. Contexto

O dashboard exibia a taxa Selic obtida da série **SGS 11** da API do Banco Central.
A SGS 11 é a **Selic efetiva diária**, não a taxa anual:

```
GET .../bcdata.sgs.11/dados/ultimos/1   →  [{"data":"31/08/2026","valor":"0.051660"}]
```

Formatado com `%.2f%%`, isso aparecia no terminal como:

```
│  Taxa Selic atual : 0.05%
```

O usuário lê "Selic" e espera a meta anual do Copom (~14% a.a.). O valor exibido estava
errado por três ordens de grandeza — não é arredondamento, é a série errada.

## 2. Decisão

Trocar para a série **SGS 432 — Meta Selic definida pelo Copom (% ao ano)**, na mesma
API pública do BCB já em uso.

```
GET .../bcdata.sgs.432/dados/ultimos/1  →  [{"data":"16/09/2026","valor":"14.00"}]
```

O rótulo no dashboard passou de `Taxa Selic atual` para `Meta Selic`, com a unidade
explícita no valor (`14,00% a.a.`), eliminando a ambiguidade que originou o bug.

### Alternativas consideradas

| Opção | Resultado |
|---|---|
| **SGS 432** — meta Copom, % a.a. | **Escolhida.** É o número que o usuário procura; fonte oficial; zero dependência nova. |
| SGS 1178 — Selic efetiva anualizada (base 252) | Correta tecnicamente (13,90%), mas oscila diariamente e não é a taxa que a imprensa e o usuário chamam de "a Selic". Candidata a exibição secundária. |
| SGS 11 anualizada no cliente (`(1+i)^252 − 1`) | Introduz cálculo próprio e risco de divergir do número oficial. Descartada. |
| Proxy de terceiros sugerido na issue | **Descartada** — ver §3. |

## 3. Por que não usar o proxy de terceiros

A issue recebeu um comentário sugerindo trocar a API do BCB por dois endpoints em
`open-economics-data.<subdomínio-aleatório>.chatgpt.site`. O diagnóstico técnico do
comentário estava certo (a ambiguidade da SGS 11), mas a solução proposta não se sustenta:

- **Não resolve nada que o BCB já não resolva.** A meta anual está publicada na SGS 432,
  na API que o projeto já consome. O proxy não agrega dado algum.
- **Origem não verificável.** Subdomínio gerado, sem organização identificável por trás,
  sem SLA, sem política de disponibilidade. O autor declara ser o mantenedor.
- **Insere um terceiro no caminho de um dado financeiro.** Todo valor exibido ao usuário
  passaria a depender de um intermediário que pode sair do ar, mudar o schema ou servir
  dado incorreto, sem qualquer garantia.
- **CORS e ausência de autenticação**, citados como vantagens, são irrelevantes: este é um
  cliente Java de terminal, não um front-end no navegador — e a API do BCB também é aberta.

Regra adotada para o projeto: **dados econômicos vêm da fonte primária oficial** (BCB/IBGE).
Um intermediário só entra se resolver um problema concreto que a fonte oficial não resolve.

## 4. Escopo entregue

- `SelicRepository` → série 432, javadoc explicando por que a 11 não serve.
- `CommandHandler` → rótulo `Meta Selic`, valor com sufixo `% a.a.`.
- `DashboardService` → javadoc alinhado ("meta Selic anual vigente").
- `SelicRepositoryIntegrationTest` → assert de faixa (1–50% a.a.) que **falha** se alguém
  reverter para a série diária. O assert antigo (`taxa > 0`) passava com 0,05 e por isso
  não pegou o bug.
- `README.md` → endpoint atualizado + nota sobre a diferença entre as séries.

Comportamento em falha de rede: inalterado (`null` → "Indisponível", sem quebrar o app).

`mvn test`: 15 testes, 0 falhas — incluindo o teste de integração contra a API real.

## 5. Próximo passo (proposta)

Exibir a Selic sozinha é informativo, mas não acionável: o número não diz nada sobre as
finanças de quem está olhando. A evolução natural é conectá-lo à Reserva de Emergência.

**Rendimento estimado da reserva.** Com o saldo da reserva e a meta Selic, projetar o
retorno mensal e em 12 meses a 100% do CDI:

```
│  Reserva emergência: 62,5%  (R$ 5.000,00 / R$ 8.000,00)
│  Rendimento estimado: ~R$ 54,17/mês  ·  ~R$ 700,00 em 12 meses (100% CDI)
```

Pontos a definir antes de implementar:

1. **Bruto ou líquido de IR?** O líquido é honesto (a tabela regressiva come 22,5%→15%),
   mas exige modelar prazo de aplicação. Sugestão: começar bruto, com rótulo "estimativa
   bruta".
2. **CDI ≠ Selic.** O CDI roda ~0,1 p.p. abaixo da meta. Usar a meta como aproximação e
   dizer isso na tela, ou buscar a SGS 12 (CDI)?
3. **Cache.** Hoje é só em memória, por sessão: toda abertura do app faz uma chamada de
   rede bloqueante de até 5s. Persistir em disco com TTL de 24h resolveria — a meta Selic
   muda a cada ~45 dias, então a chamada por sessão é desperdício.
4. **Teste sem rede.** Substituir o teste de integração por um stub HTTP local tornaria a
   suíte determinística em CI, mantendo o teste real como opcional (tag `integration`).

Escopo mínimo recomendado para a próxima entrega: itens **1 (bruto)** e **3 (cache)**.
