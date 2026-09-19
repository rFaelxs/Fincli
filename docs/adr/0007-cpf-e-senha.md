# ADR 0007 — Login por CPF e senha

- **Status:** aceita, com pendência
- **Data:** 2026-09-18
- **Origem:** `escopo-web.md` §1.1 e §1.5

## Contexto

No CLI, conhecer o CPF era a autenticação: não havia senha. Num binário rodando na máquina do próprio usuário, isso era uma conveniência. Publicado na web, seria acesso total para quem soubesse o CPF de alguém — e CPF não é segredo.

## Decisão

O login passa a ser CPF e senha.

- A senha é gravada com hash BCrypt e tem entre 8 e 100 caracteres.
- O CPF é validado pelos dígitos verificadores no cadastro e gravado só com dígitos, sob índice único.
- A mensagem de falha é sempre "CPF ou senha inválidos.", para CPF inexistente e para senha errada. Distinguir os dois permitiria descobrir quem tem conta.
- O CPF só sai da aplicação mascarado, no formato `***.***.**NN`.

## Consequências

- **Pendência: não há recuperação de senha.** Com o CPF como único identificador, não existe canal para provar identidade. A coluna `email` existe e é opcional, mas não há fluxo. A tela de cadastro avisa: "ainda não existe recuperação de senha — guarde-a com cuidado."
- Também ficam fora desta leva o bloqueio por tentativas repetidas, a exclusão de conta e a cifra do CPF em repouso. Cada um precisa de spec própria, porque mexe no schema.
- O CPF é dado pessoal sob a LGPD e nunca pode aparecer em log, mensagem de erro ou URL.
