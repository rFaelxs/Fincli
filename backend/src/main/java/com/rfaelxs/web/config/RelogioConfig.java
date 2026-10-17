package com.rfaelxs.web.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Relógio da aplicação, no fuso de Brasília.
 *
 * <p>O fuso é fixo, e não o do servidor: num servidor em UTC, um movimento feito depois das 21h
 * cairia no dia seguinte. Como bean, "hoje" também fica injetável nos testes.
 */
@Configuration
public class RelogioConfig {

  public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

  @Bean
  public Clock relogio() {
    return Clock.system(FUSO);
  }
}
