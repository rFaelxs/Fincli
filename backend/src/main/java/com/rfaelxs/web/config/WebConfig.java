package com.rfaelxs.web.config;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Rotas das páginas do front (resources/static, uma pasta por recurso).
 *
 * <p>O Spring só resolve {@code index.html} automaticamente na raiz; para as pastas
 * ({@code /login/}, {@code /transacoes/}, ...) o forward precisa ser explícito.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  private static final List<String> PAGINAS =
      List.of(
          "login", "cadastro", "hoje", "relatorio",
          "dashboard", "transacoes", "reservas", "extrato");

  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    for (String pagina : PAGINAS) {
      registry.addViewController("/" + pagina).setViewName("forward:/" + pagina + "/index.html");
      registry.addViewController("/" + pagina + "/")
          .setViewName("forward:/" + pagina + "/index.html");
    }
  }
}
