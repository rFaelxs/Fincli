# Spec — Autenticação

Cadastro e login por CPF e senha, com a sessão em cookie e a Reserva de Emergência criada junto da conta.

**Cobre:** RF-01, RF-02 (ids do mapa das specs, **proposto**). RN-CAD-01 a RN-CAD-04 (**proposto**): CPF validado pelos dígitos verificadores, senha com no mínimo 8 caracteres, CPF único, Reserva de Emergência criada no cadastro.

## Telas e rotas

| Rota Angular | Tela | Acesso |
|---|---|---|
| `/login` | Entrar | Pública |
| `/cadastro` | Criar conta | Pública |

Com sessão ativa, as duas redirecionam para `/gestao`.

## API

| Método e rota | Uso ou corpo | Respostas |
|---|---|---|
| `GET /api/csrf` | Emite o cookie `XSRF-TOKEN` antes do primeiro POST | 204 |
| `GET /api/me` | Detecta sessão ativa; chamada silenciosa | 200 · 401 |
| `POST /api/cadastro` | `{ nome, cpf, senha, email? }` | 201 `{ id, nome }` · 400 · 409 |
| `POST /api/login` | `{ cpf, senha }` | 200 `{ id, nome }` · 401 |

O CPF pode ir com ou sem máscara: o servidor remove tudo que não é dígito. `email` é opcional e o formulário atual não o envia.

## Comportamento

1. Ao abrir `/login` ou `/cadastro`, a tela chama `GET /api/csrf` e depois `GET /api/me` em modo silencioso. Havendo sessão, substitui a rota por `/gestao`.
2. Sem sessão, o 401 do `GET /api/me` é engolido e a tela pública continua visível.
3. A tela de login mostra o título "Acesse sua conta" e o subtítulo "Receitas, despesas e reservas em um só lugar.".
4. O campo de CPF usa teclado numérico e o exemplo "000.000.000-00"; o de senha usa o exemplo "••••••••".
5. O botão de login diz "Entrar"; abaixo do formulário há "Não tem conta? Cadastre-se", ligado a `/cadastro`.
6. Ao enviar o login, a tela chama `POST /api/login` em modo silencioso e, em caso de sucesso, substitui a rota por `/gestao`.
7. Credencial errada responde 401 e a tela mostra "CPF ou senha inválidos." acima do formulário. A mensagem é a mesma para CPF inexistente e senha errada.
8. Qualquer outro erro no login mostra o `detail` devolvido pelo servidor.
9. A tela de cadastro mostra o título "Criar conta" e o subtítulo "A Reserva de Emergência já vem criada.".
10. O campo de senha do cadastro tem o rótulo "Senha (mínimo 8 caracteres)", exige 8 caracteres e aceita no máximo 100. O nome aceita no máximo 120.
11. O botão do cadastro diz "Cadastrar e entrar"; abaixo dele há o aviso "Atenção: ainda não existe recuperação de senha — guarde-a com cuidado." e "Já tem conta? Entrar".
12. O cadastro chama `POST /api/cadastro` e, em seguida, `POST /api/login` com as mesmas credenciais; só então substitui a rota por `/gestao`.
13. CPF com dígito verificador errado responde 400 com `detail` "CPF inválido."; CPF nulo ou vazio responde "CPF é obrigatório.".
14. CPF já cadastrado responde 409 com `detail` "CPF já cadastrado.".
15. Senha com menos de 8 caracteres responde 400 com `detail` "A senha deve ter entre 8 e 100 caracteres." vindo da validação de campo; a validação de serviço usa "A senha deve ter pelo menos 8 caracteres.".
16. Campos em branco respondem 400 com `detail` "Verifique os campos enviados." e `campos` com as mensagens "Informe seu nome.", "Informe seu CPF." e "Informe uma senha.".
17. O erro fica num bloco fixo acima do formulário e é apagado a cada novo envio.
18. O cadastro cria a "Reserva de Emergência" com meta zerada, na mesma transação.
19. O login descarta a sessão anterior antes de gravar a nova.
20. O CPF não volta em nenhuma resposta ([ADR 0009](../adr/0009-cpf-fora-das-respostas.md)).

## Critérios de aceite

- [x] CA-AUTH-01: Dado um usuário com sessão ativa, quando ele abre `/login`, então a aplicação substitui a rota por `/gestao` sem mostrar o formulário.
- [x] CA-AUTH-02: Dado um navegador sem sessão, quando `/login` monta, então o 401 de `GET /api/me` não redireciona nem mostra erro.
- [x] CA-AUTH-03: Dado o CPF `529.982.247-25` e a senha correta, quando o usuário envia o login, então `POST /api/login` recebe `cpf` com a máscara e a aplicação navega para `/gestao`.
- [x] CA-AUTH-04: Dado um CPF não cadastrado, quando o usuário tenta entrar, então a tela mostra "CPF ou senha inválidos." e permanece em `/login`.
- [x] CA-AUTH-05: Dado um CPF cadastrado com a senha errada, quando o usuário tenta entrar, então a mensagem é exatamente a mesma do CA-AUTH-04.
- [x] CA-AUTH-06: Dado o CPF `111.111.111-11`, quando o usuário envia o cadastro, então o servidor responde 400 com `detail` "CPF inválido.".
- [x] CA-AUTH-07: Dado um CPF já cadastrado, quando o usuário envia o cadastro, então o servidor responde 409 e a tela mostra "CPF já cadastrado.".
- [x] CA-AUTH-08: Dada a senha "1234567", quando o usuário envia o cadastro, então o servidor responde 400 com "A senha deve ter entre 8 e 100 caracteres.".
- [x] CA-AUTH-09: Dado um cadastro com nome vazio, quando o formulário é enviado, então a resposta traz `campos.nome` igual a "Informe seu nome.".
- [x] CA-AUTH-10: Dado um cadastro válido, quando ele conclui, então `POST /api/login` é chamado em seguida e a aplicação navega para `/gestao`.
- [x] CA-AUTH-11: Dado um cadastro concluído, quando `GET /api/reservas` é consultado, então existe uma reserva "Reserva de Emergência" com `metaValor` 0,00 e `emergencia` verdadeiro.
- [x] CA-AUTH-12: Dado um usuário recém-cadastrado com CPF `52998224725`, quando `POST /api/cadastro` responde, então o corpo traz só `id` e `nome`, sem nenhum campo de CPF.
- [x] CA-AUTH-13: Dado um erro exibido no formulário, quando o usuário envia de novo, então o bloco de erro é apagado antes da chamada.
- [x] CA-AUTH-14: Dada uma sessão aberta no navegador, quando o usuário faz login de novo, então o identificador de sessão anterior deixa de ser válido.

## Estrutura Angular

```
features/auth/
├── auth.css                   # o CSS das duas telas, referenciado por ambas
├── pages/
│   ├── login.page.ts          # rota /login
│   └── cadastro.page.ts       # rota /cadastro
└── data/
    └── auth.service.ts        # POST /api/cadastro e /api/login, silenciosos

core/auth/guardas.ts           # publicoGuard: com sessão, /login e /cadastro vão para /gestao
shared/ui/erro-formulario.component.ts
```

A validação dos campos é a nativa do navegador (`required`, `minlength`, `maxlength`), mantida com `ngNativeValidate`: o Angular a desliga por padrão. O CPF não tem máscara nem validação no cliente, como hoje. Componentes standalone com formulários reativos; estado em Signals; sem NgRx.


## Observação sobre o estado atual

- Não existe recuperação de senha. O aviso na tela de cadastro é a única forma de dizer isso ao usuário, e a coluna `email` existe mas não é preenchida pelo formulário.
- Não há bloqueio por tentativas repetidas de login.
- O CPF é validado só no servidor: o formulário atual envia qualquer texto e espera o 400.

## Propostas

- Validar os dígitos verificadores do CPF no cliente, para dar o erro antes do envio, mantendo a validação do servidor como autoridade.
- Desabilitar o botão de envio enquanto a chamada está em voo, como a barra de comando já faz com o próprio campo.

## A confirmar

- Se o campo de e-mail opcional entra no formulário de cadastro agora, já que a coluna existe e o contrato aceita.
