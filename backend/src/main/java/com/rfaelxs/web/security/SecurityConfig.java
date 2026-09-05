package com.rfaelxs.web.security;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Autenticação por sessão em cookie {@code HttpOnly}, com CSRF habilitado.
 *
 * <p>Escolha registrada no escopo-web.md §2.4: mesmo sendo um SPA, o token não vai para
 * {@code localStorage} — qualquer XSS no front-end o leria. Um cookie {@code HttpOnly} não é
 * legível por JavaScript, e o custo é ter de lidar com CSRF, que o Spring já resolve.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final List<String> origensPermitidas;

  public SecurityConfig(@Value("${fincli.cors.origens}") List<String> origensPermitidas) {
    this.origensPermitidas = origensPermitidas;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
      throws Exception {
    return config.getAuthenticationManager();
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    // Desliga o carregamento tardio do token para que o cookie XSRF-TOKEN seja emitido já na
    // primeira requisição — sem isso o SPA não teria token para enviar no primeiro POST.
    CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
    csrfHandler.setCsrfRequestAttributeName(null);

    http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(csrf -> csrf
            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            .csrfTokenRequestHandler(csrfHandler))
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            .sessionFixation(fixation -> fixation.changeSessionId()))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/cadastro", "/api/login", "/api/csrf").permitAll()
            .requestMatchers("/actuator/health").permitAll()
            // Páginas do front servidas de resources/static — a UI em si é pública;
            // todo dado continua atrás de /api/**.
            .requestMatchers(org.springframework.http.HttpMethod.GET,
                "/", "/index.html", "/assets/**",
                "/login/**", "/cadastro/**", "/hoje/**", "/relatorio/**", "/dashboard/**",
                "/transacoes/**", "/reservas/**", "/extrato/**").permitAll()
            .anyRequest().authenticated())
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint((request, response, authException) ->
                responderJson(response, HttpStatus.UNAUTHORIZED, "Autenticação necessária."))
            .accessDeniedHandler((request, response, deniedException) ->
                responderJson(response, HttpStatus.FORBIDDEN, "Acesso negado.")))
        .logout(logout -> logout
            .logoutUrl("/api/logout")
            .deleteCookies("JSESSIONID")
            .logoutSuccessHandler((request, response, authentication) ->
                response.setStatus(HttpServletResponse.SC_NO_CONTENT)))
        .formLogin(form -> form.disable())
        .httpBasic(basic -> basic.disable());

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(origensPermitidas);
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    // Obrigatório para que o navegador envie o cookie de sessão em requisições cross-origin.
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
  }

  private static void responderJson(HttpServletResponse response, HttpStatus status, String detalhe)
      throws java.io.IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    response.getWriter().write(
        "{\"status\":%d,\"title\":\"%s\",\"detail\":\"%s\"}"
            .formatted(status.value(), status.getReasonPhrase(), detalhe));
  }
}
