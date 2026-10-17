# Spec — Barra de comando

Texto livre vira lançamento: o cliente mostra o preview enquanto o usuário digita, e o servidor reinterpreta o texto cru antes de gravar.

**Cobre:** RF-17, RF-18 (ids do mapa das specs, **proposto**). RN-CMD-01 a RN-CMD-06 (**proposto**): tipo pela palavra inicial, valor pelo primeiro número, categoria por palavra-chave, data relativa, conta prevista com dia e recorrência, escolha da reserva no aporte.

A barra vive dentro da tela Gestão. O preview nunca é aceito como entrada: só o texto trafega.

## Telas e rotas

| Rota Angular | Tela | Acesso |
|---|---|---|
| `/gestao` | Barra de comando, dentro de Gestão | Autenticada |

## API

| Método e rota | Uso ou corpo | Respostas |
|---|---|---|
| `POST /api/comando` | `{ texto }` | 200 · 400 · 422 |
| `DELETE /api/transacoes/{id}` | Desfazer de entrada e saída | 204 · 404 |
| `POST /api/reservas/{id}/sacar` | Desfazer de aporte: saque de mesmo valor | 200 · 400 · 404 |
| `DELETE /api/previstas/{id}` | Desfazer de conta prevista | 204 · 404 |

A resposta de `POST /api/comando` traz `tipo`, `id`, `reservaId`, `valor`, `descricao`, `categoria` e `mensagem`. `id` é nulo em aporte; `reservaId` só vem em aporte.

## Comportamento

### Gramática, decidida no servidor

1. O tipo vem da primeira palavra: `prever` ou `conta` é conta prevista; `guardar`, `poupar` ou `alocar` é aporte; `entrada`, `recebi` ou `+` é entrada. Sem nenhuma delas, o texto é uma saída.
2. As palavras `salario`, `freela`, `freelance`, `pix` e `bonus` em qualquer posição também classificam o texto como entrada.
3. O valor é o primeiro número do texto. Com vírgula, o ponto é separador de milhar: `1.234,56` vale 1234,56. Sem vírgula, um ponto com uma ou duas casas é decimal (`12.50`); qualquer outro ponto é milhar (`1.234` vale 1234).
4. Texto sem número não vira lançamento.
5. A ordem das palavras não importa: `mercado 120 ontem essencial` e `ontem 120 mercado essencial` dão o mesmo resultado.
6. `hoje`, `ontem` e `anteontem` definem a data; sem nenhuma delas, a data é hoje.
7. A categoria é deduzida por palavra-chave, sem acento e em minúsculas: "Alimentação", "Moradia", "Saúde", "Transporte", "Lazer" e "Trabalho". Sem palavra-chave, entrada cai em "Trabalho" e saída em "Outros". Toda palavra-chave, inclusive as dos itens 1, 2, 6 e 8, casa como palavra inteira, delimitada por qualquer coisa que não seja letra: `gas` não pega "gasolina" e `bar` não pega "barato", mas `mercado` ainda pega "mercado120".
8. Alimentação, Moradia, Saúde e Transporte nascem marcadas como essenciais; Lazer e Trabalho não. A palavra `essencial` força a marcação em qualquer saída.
9. A descrição é o que sobra depois de retiradas as palavras de controle e o número. Artigos de ligação no começo são descartados, e a primeira letra vira maiúscula.
10. Descrição vazia vira "Aporte" em aporte, "Conta" em conta prevista e "Lançamento" nos demais.
11. Em conta prevista, `dia N` define o vencimento; sem ele, a conta vence no dia de hoje.
12. Conta prevista nasce recorrente, a menos que o texto traga "uma vez", "so esse mes" ou "so este mes".
13. Em aporte sem nome de reserva, o dinheiro vai para a Reserva de Emergência.
14. Em aporte com nome, a reserva é escolhida por trecho do nome, ignorando acentos, maiúsculas e pontuação.

### Tela

15. O campo tem o rótulo oculto "Lançar por texto", o exemplo "mercado 120 ontem essencial" e a tecla mostrada ao lado.
16. A tecla mostrada é "⌘K" em Mac, iPhone e iPad, e "Ctrl K" no resto.
17. `Ctrl+K` ou `⌘K` em qualquer ponto da tela foca o campo e seleciona o conteúdo.
18. A cada tecla digitada, o cliente refaz o preview com a mesma gramática do servidor e mostra o resultado numa região com `aria-live="polite"`.
19. O preview abre com o verbo do tipo — "vai lançar", "vai receber", "vai guardar" ou "vai prever" —, seguido do valor rotulado: "saída R$ 120,00", "entrada", "aporte" ou "conta".
20. Fora de conta prevista, o preview mostra ainda a categoria em minúsculas, o dia no formato `16/09` e, em saída, "essencial" ou "supérflua".
21. Em conta prevista, o preview mostra "vence dia 22" e "todo mês" ou "só este mês".
22. O preview termina com a dica "enter para salvar".
23. Sem preview possível — campo vazio ou texto sem número —, a tela mostra "tente:" e os três exemplos "guardar 300 viagem", "uber 32 hoje" e "freela 450".
24. Clicar num exemplo preenche o campo, devolve o foco a ele e refaz o preview.
25. `Enter` envia; `Esc` limpa o campo e o preview. Texto sem preview não é enviado.
26. Enquanto o envio está em voo, o campo fica desabilitado; ao terminar, ele é reabilitado, esvaziado e refocado.
27. Sucesso mostra a faixa com a mensagem do servidor e a ação "desfazer", e recarrega a tela Gestão.
28. A mensagem segue o tempo verbal do tipo: "Recebido", "Lançado", "Guardado" ou "Previsto", seguido do valor em BRL e da descrição, separados por " · ".
29. "desfazer" apaga a transação em entrada e saída, apaga a conta em conta prevista, e em aporte faz um saque de mesmo valor na mesma reserva.
30. Depois de desfazer um aporte, a tela mostra o toast "Aporte desfeito — o saque ficou registrado no extrato.".
31. Texto que não vira lançamento responde 422 com `detail` "Não entendi \"...\" — tente algo como `mercado 120 ontem`.", ecoando no máximo 40 caracteres do que foi digitado, seguidos de reticências.
32. Aporte sem nenhuma reserva cadastrada responde 422 com "Você ainda não tem reservas para guardar dinheiro.".
33. Aporte com nome que não casa responde 422 com "Não achei a reserva \"viagem\". Crie a reserva antes de guardar nela.".
34. Aporte com nome ambíguo responde 422 com "\"viagem\" casa com mais de uma reserva: Viagem Chile, Viagem Japão. Escreva o nome inteiro.".
35. Aporte acima do saldo disponível responde 400 com "Saldo insuficiente para esta alocação.".
36. Texto em branco responde 400 com `campos.texto` igual a "Escreva o que você quer lançar."; acima de 255 caracteres, "O comando deve ter no máximo 255 caracteres.".
37. Erro aparece na faixa, em tratamento de erro, com o botão "tentar de novo"; as sugestões somem enquanto a faixa está visível.

## Critérios de aceite

- [ ] CA-CMD-01: Dado o texto `mercado 120 ontem`, quando o usuário envia, então é criada uma saída de 120,00 na categoria "Alimentação", marcada como essencial, com a data de ontem e a descrição "Mercado".
- [ ] CA-CMD-02: Dado o texto `ontem 120 mercado`, quando o usuário envia, então o resultado é igual ao do CA-CMD-01.
- [ ] CA-CMD-03: Dado o texto `cinema 60`, quando o usuário envia, então é criada uma saída na categoria "Lazer" não essencial.
- [ ] CA-CMD-04: Dado o texto `cinema 60 essencial`, quando o usuário envia, então a saída é marcada como essencial.
- [ ] CA-CMD-05: Dado o texto `freela 450`, quando o usuário envia, então é criada uma entrada de 450,00 na categoria "Trabalho".
- [ ] CA-CMD-06: Dado o texto `1.234,56 aluguel`, quando o usuário envia, então o valor gravado é 1234,56.
- [ ] CA-CMD-07: Dado o texto `almoço 12.50`, quando o usuário envia, então o valor gravado é 12,50.
- [ ] CA-CMD-08: Dado o texto `feira 1.234`, quando o usuário envia, então o valor gravado é 1234,00.
- [ ] CA-CMD-09: Dado o texto `mercado sem numero`, quando o usuário digita, então nenhum preview aparece e o envio não acontece.
- [ ] CA-CMD-10: Dado o texto `mercado sem numero` enviado por outro cliente, quando `POST /api/comando` responde, então o status é 422 e o `detail` começa com "Não entendi".
- [ ] CA-CMD-11: Dado o texto `prever energia 210 dia 22`, quando o usuário envia, então é criada uma conta prevista "Energia" de 210,00, com vencimento no dia 22 e recorrente.
- [ ] CA-CMD-12: Dado o texto `prever energia 210 dia 22 uma vez`, quando o usuário envia, então a conta é avulsa, com `mesReferencia` igual ao mês da data do comando.
- [ ] CA-CMD-13: Dado o texto `prever internet 99`, quando o usuário envia num dia 5, então o vencimento é o dia 5.
- [ ] CA-CMD-14: Dado o texto `guardar 300`, quando o usuário envia, então 300,00 são alocados na Reserva de Emergência.
- [ ] CA-CMD-15: Dado o texto `guardar 300 viagem` e a reserva "Viagem Japão", quando o usuário envia, então a alocação cai em "Viagem Japão".
- [ ] CA-CMD-16: Dadas as reservas "Viagem Chile" e "Viagem Japão", quando o usuário envia `guardar 300 viagem`, então a resposta é 422 e o `detail` lista os dois nomes.
- [ ] CA-CMD-17: Dado um saldo disponível de 200,00, quando o usuário envia `guardar 300`, então a resposta é 400 com "Saldo insuficiente para esta alocação.".
- [ ] CA-CMD-18: Dado um texto de 60 caracteres que não vira lançamento, quando a resposta 422 chega, então o eco no `detail` tem 40 caracteres seguidos de reticências.
- [ ] CA-CMD-19: Dado o texto `uber 32 hoje` no campo, quando o preview pinta, então mostra "vai lançar", "saída R$ 32,00", "transporte", o dia de hoje e "essencial".
- [ ] CA-CMD-20: Dado o texto `prever energia 210 dia 22` no campo, quando o preview pinta, então mostra "vai prever", "conta R$ 210,00", "vence dia 22" e "todo mês".
- [ ] CA-CMD-21: Dado o campo vazio, quando a tela pinta, então as três sugestões aparecem sob o rótulo "tente:".
- [ ] CA-CMD-22: Dado o clique na sugestão "uber 32 hoje", quando o campo é preenchido, então ele recebe o foco e o preview aparece.
- [ ] CA-CMD-23: Dado o foco fora do campo, quando o usuário pressiona `Ctrl+K`, então o campo recebe foco e o texto fica selecionado.
- [ ] CA-CMD-24: Dado o texto `mercado 120` no campo, quando o usuário pressiona `Esc`, então o campo esvazia e o preview some.
- [ ] CA-CMD-25: Dado o envio de `mercado 120`, quando a resposta chega, então a faixa mostra "Lançado R$ 120,00 · Mercado" com a ação "desfazer".
- [ ] CA-CMD-26: Dado o envio de `recebi 450 freela`, quando a resposta chega, então a mensagem começa com "Recebido".
- [ ] CA-CMD-27: Dada uma saída recém-lançada, quando o usuário clica em "desfazer", então `DELETE /api/transacoes/{id}` é chamado e a tela recarrega.
- [ ] CA-CMD-28: Dado um aporte de 300,00 recém-lançado, quando o usuário clica em "desfazer", então `POST /api/reservas/{reservaId}/sacar` é chamado com 300,00 e o toast "Aporte desfeito — o saque ficou registrado no extrato." aparece.
- [ ] CA-CMD-29: Dado um aporte desfeito, quando o extrato é aberto, então a alocação e o saque aparecem como dois itens.
- [ ] CA-CMD-30: Dado um envio em voo, quando a resposta ainda não chegou, então o campo está desabilitado.
- [ ] CA-CMD-31: Dado um erro de servidor no envio, quando a faixa de erro aparece, então as sugestões ficam escondidas.
- [ ] CA-CMD-32: Dado um navegador identificado como Mac, quando a barra monta, então a tecla mostrada é "⌘K".

## Estrutura Angular

```
features/gestao/
├── comando/
│   ├── gramatica.ts                   # espelho de ComandoParser.java, só para o preview
│   └── gramatica.spec.ts              # os mesmos casos de ComandoParserTest
├── components/
│   └── barra-comando.component.ts     # campo, preview aria-live, sugestões, faixa, Esc e Ctrl/⌘K
└── data/
    ├── gestao-api.service.ts          # POST /api/comando e as chamadas do desfazer
    └── gestao.store.ts                # texto, preview, faixa e o último lançamento desfazível
```

Reaproveita `shared/ui/faixa-aviso.component.ts` e `shared/format`. Componentes standalone; estado em Signals; sem NgRx.


## Observação sobre o estado atual

- A gramática existe duas vezes: em `ComandoParser.java` e no parser do cliente. Divergência entre os dois produz um preview que promete o que não acontece. A fonte única foi adiada nas decisões da reescrita.
- O "desfazer" guarda o último lançamento em memória; recarregar a tela perde a ação.
- Desfazer um aporte não apaga a movimentação original: ela fica no extrato, com o saque ao lado.
- No front antigo, as sugestões e a faixa eram escondidas com o atributo `hidden`, mas o CSS declara `display: flex` nas duas classes, e isso vence o `hidden`. Na prática as sugestões nunca sumiam, e depois do primeiro "desfazer" ficava uma faixa vazia na tela. O Angular implementa o que os itens 23 e 37 descrevem.

## A confirmar

- Se o parser do cliente passa a ser gerado a partir das tabelas do servidor, ou se as duas cópias continuam sincronizadas à mão com testes espelhados.
- **Adiado na reescrita (2026-10-17):** atalhos e foco por teclado. Ficaram fora da primeira entrega do Angular os itens 16 e 17 (tecla mostrada ao lado do campo e `Ctrl+K`/`⌘K`), os CA-CMD-23 e 32, e, na spec 03, o item 23 e o CA-GESTAO-20 (link "Pular para a barra de comando"). `Enter` e `Esc` continuam, por serem o comportamento básico do campo. Os atalhos `G` e `R` do shell (spec 01) não são afetados.
