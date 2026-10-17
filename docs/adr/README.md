# Decisões de arquitetura

Cada decisão que mudou a forma do projeto fica registrada aqui, com o contexto que a motivou e o que ela custa. Uma decisão revista não é apagada: ganha um ADR novo que a substitui.

| ADR | Decisão | Status |
|---|---|---|
| [0001](0001-front-em-angular.md) | Front em Angular | aceita |
| [0002](0002-empacotamento-monolitico.md) | Build do Angular dentro do JAR | aceita |
| [0003](0003-cli-fora-do-repositorio.md) | CLI fora do repositório | aceita |
| [0004](0004-selic-descartada.md) | Integração com a Selic | descartada |
| [0005](0005-resumoservice-separado.md) | ResumoService separado por tela | aceita |
| [0006](0006-sessao-em-cookie.md) | Sessão em cookie HttpOnly com CSRF | aceita |
| [0007](0007-cpf-e-senha.md) | Login por CPF e senha | aceita, com pendência |
| [0008](0008-persistencia.md) | SQL Server, BigDecimal e serviços sem estado | aceita |

O comportamento de cada tela está nas [specs](../specs/).
