package com.rfaelxs.web.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * Sessão em cookie {@code HttpOnly}, com CSRF habilitado (ADR 0006).
 *
 * <p>O token não vai para {@code localStorage}, onde qualquer XSS no front o leria. O custo é
 * lidar com CSRF: o cookie {@code XSRF-TOKEN} é legível pelo front, que reenvia o valor no header
 * {@code X-XSRF-TOKEN} em toda mutação.
 *
 * <p>Tudo em {@code /api/**} exige sessão. As rotas públicas da API, como login e cadastro, entram
 * junto com o módulo de autenticação. O resto é o front, servido pelo próprio Spring (ADR 0002).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final ObjectMapper json;

  public SecurityConfig(ObjectMapper json) {
    this.json = json;
  }

  /** Cadeia de filtros: CSRF por cookie, sessão no servidor e {@code /api/**} fechado. */
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    // Desliga o carregamento tardio do token para que o cookie XSRF-TOKEN seja emitido já na
    // primeira requisição. Sem isso o front não teria token para enviar no primeiro POST.
    CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
    csrfHandler.setCsrfRequestAttributeName(null);

    http.csrf(csrf -> csrf
            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            .csrfTokenRequestHandler(csrfHandler))
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            .sessionFixation(fixation -> fixation.changeSessionId()))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/**").authenticated()
            .anyRequest().permitAll())
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint((request, response, authException) ->
                responder(response, HttpStatus.UNAUTHORIZED, "Não autorizado",
                    "Autenticação necessária."))
            .accessDeniedHandler((request, response, deniedException) ->
                responder(response, HttpStatus.FORBIDDEN, "Acesso negado", "Acesso negado.")))
        .formLogin(form -> form.disable())
        .httpBasic(basic -> basic.disable());

    return http.build();
  }

  /**
   * Os filtros de segurança rodam antes dos controllers, então o {@code TratadorDeErros} não os
   * alcança. A resposta é montada aqui, no mesmo formato {@code problem+json}.
   */
  private void responder(HttpServletResponse response, HttpStatus status, String titulo,
      String detalhe) throws IOException {
    ProblemDetail problema = ProblemDetail.forStatus(status);
    problema.setTitle(titulo);
    problema.setDetail(detalhe);

    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    json.writeValue(response.getWriter(), problema);
  }
}
