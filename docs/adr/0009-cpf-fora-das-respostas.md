# ADR 0009 — O CPF não sai nas respostas da API

- **Status:** aceita
- **Data:** 2026-10-17
- **Substitui:** o item "O CPF só sai da aplicação mascarado" da [ADR 0007](0007-cpf-e-senha.md)

## Contexto

A ADR 0007 mandava devolver o CPF mascarado em `/api/me`, `/api/cadastro` e `/api/login`. As fontes divergiam sobre a máscara: o texto da spec 01 e o código antigo mostravam três dígitos, e os critérios de aceite mostravam dois.

Nenhuma tela usa o CPF. O rodapé do shell mostra o nome e as iniciais; o único consumidor do campo era o teste que conferia a máscara.

O CPF é metade da credencial de login e dado pessoal sob a LGPD. Tudo que sai do servidor pode ficar nas ferramentas do navegador, em cache, em log de proxy ou de monitoramento de erros, ou ao alcance de um script injetado.

## Decisão

As respostas da API não trazem o CPF. O usuário da sessão volta como `{ id, nome }`.

## Consequências

- Não há máscara para manter nem para divergir.
- Se uma tela futura precisar do CPF, como uma página de perfil, ele volta mascarado por um ADR novo, que define a máscara.
- O CPF continua entrando no cadastro e no login, e continua gravado só com dígitos.
