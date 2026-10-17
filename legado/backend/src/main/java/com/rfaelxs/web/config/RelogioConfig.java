package com.rfaelxs.web.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Relógio da aplicação.
 *
 * <p>Existe como bean para que "hoje" seja injetável: os cálculos da tela Hoje dependem do dia
 * do mês, e um teste preso a {@code LocalDate.now()} passaria ou falharia conforme a data em
 * que rodasse.
 */
@Configuration
public class RelogioConfig {

  @Bean
  public Clock relogio() {
    return Clock.systemDefaultZone();
  }
}
