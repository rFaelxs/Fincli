package com.rfaelxs.web.config;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Fallback de SPA: as rotas do Angular devolvem o {@code index.html}, e o roteador do front
 * decide a tela (ADR 0002).
 *
 * <p>A lista é explícita, e não um "pega tudo": aqui as view controllers têm prioridade sobre os
 * arquivos estáticos, e um padrão genérico engoliria {@code /assets/**} e os chunks do build.
 * Caminho fora da lista continua dando 404. A raiz já é resolvida pelo Spring Boot, que serve o
 * {@code index.html} em {@code /}.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  /** Espelho das rotas de {@code frontend/src/app/app.routes.ts}. */
  private static final List<String> ROTAS_DO_FRONT =
      List.of(
          "/login", "/cadastro", "/gestao", "/relatorio", "/extrato",
          "/transacoes", "/transacoes/**", "/reservas", "/reservas/**");

  /**
   * Endereços de telas renomeadas. Redirecionam para que um link ou favorito antigo não caia em
   * 404: Hoje virou Gestão, e o Dashboard foi substituído por ela antes disso.
   */
  private static final List<String> ROTAS_ANTIGAS =
      List.of("/hoje", "/hoje/", "/dashboard", "/dashboard/");

  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    for (String rota : ROTAS_DO_FRONT) {
      registry.addViewController(rota).setViewName("forward:/index.html");
    }
    for (String rota : ROTAS_ANTIGAS) {
      registry.addRedirectViewController(rota, "/gestao");
    }
  }
}
