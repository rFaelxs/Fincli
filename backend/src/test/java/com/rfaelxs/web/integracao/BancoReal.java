package com.rfaelxs.web.integracao;

import com.rfaelxs.web.domain.Usuario;
import com.rfaelxs.web.service.UsuarioService;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base dos testes de integração: a aplicação inteira contra um SQL Server de verdade.
 *
 * <p>É o que os testes com mock não alcançam: as migrations do Flyway, a validação do Hibernate,
 * as consultas agregadas, os locks e as chaves estrangeiras. O container sobe uma vez e serve a
 * todas as classes; cada teste cria o próprio usuário, então não há limpeza entre eles.
 */
@SpringBootTest
abstract class BancoReal {

  /** A mesma imagem do {@code docker-compose.yml}. */
  static final MSSQLServerContainer<?> SQL_SERVER = new MSSQLServerContainer<>(
      DockerImageName.parse("mcr.microsoft.com/mssql/server:2022-latest"))
      .acceptLicense();

  static {
    SQL_SERVER.start();
  }

  @Autowired
  private UsuarioService usuarioService;

  @DynamicPropertySource
  static void banco(DynamicPropertyRegistry propriedades) {
    propriedades.add("spring.datasource.url", SQL_SERVER::getJdbcUrl);
    propriedades.add("spring.datasource.username", SQL_SERVER::getUsername);
    propriedades.add("spring.datasource.password", SQL_SERVER::getPassword);
  }

  /** Cadastra um usuário novo, com um CPF válido sorteado, e devolve o id interno. */
  Long novoUsuario() {
    Usuario u = usuarioService.cadastrar("Teste", cpfValido(), "segredo123", null);
    return u.getId();
  }

  /** Nove dígitos sorteados mais os dois verificadores calculados. */
  static String cpfValido() {
    int[] d = new int[11];
    do {
      for (int i = 0; i < 9; i++) {
        d[i] = ThreadLocalRandom.current().nextInt(10);
      }
    } while (d[0] == d[1] && d[1] == d[2]);
    for (int posicao = 9; posicao <= 10; posicao++) {
      int soma = 0;
      for (int i = 0; i < posicao; i++) {
        soma += d[i] * (posicao + 1 - i);
      }
      int resto = soma % 11;
      d[posicao] = resto < 2 ? 0 : 11 - resto;
    }
    StringBuilder cpf = new StringBuilder();
    for (int digito : d) {
      cpf.append(digito);
    }
    return cpf.toString();
  }
}
