package com.rfaelxs.web.api;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rfaelxs.web.security.SecurityConfig;
import com.rfaelxs.web.security.UsuarioLogado;
import com.rfaelxs.web.service.ExtratoService;
import com.rfaelxs.web.service.ItemExtrato;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ExtratoController.class)
@Import(SecurityConfig.class)
class ExtratoControllerTest {

  private static final UsuarioLogado SESSAO = new UsuarioLogado(7L, "52998224725", null);

  @Autowired
  private MockMvc mvc;

  @MockBean
  private ExtratoService extratoService;

  @MockBean
  private UserDetailsService usuarios;

  @Test
  void devolveOsItensNoFormatoDaSpecSemOIdInterno() throws Exception {
    when(extratoService.listar(7L)).thenReturn(List.of(
        new ItemExtrato(ItemExtrato.Origem.RESERVA, LocalDate.of(2026, 9, 18),
            new BigDecimal("300.00"), "ALOCACAO", "Reserva", "Viagem", 4L),
        new ItemExtrato(ItemExtrato.Origem.TRANSACAO, LocalDate.of(2026, 9, 16),
            new BigDecimal("120.00"), "SAIDA", "Alimentação", null, 9L)));

    mvc.perform(get("/api/extrato").with(user(SESSAO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].origem").value("RESERVA"))
        .andExpect(jsonPath("$[0].data").value("2026-09-18"))
        .andExpect(jsonPath("$[0].valor").value(300.00))
        .andExpect(jsonPath("$[0].tipo").value("ALOCACAO"))
        .andExpect(jsonPath("$[0].categoria").value("Reserva"))
        .andExpect(jsonPath("$[0].descricao").value("Viagem"))
        .andExpect(jsonPath("$[0].id").doesNotExist())
        .andExpect(jsonPath("$[1].origem").value("TRANSACAO"))
        .andExpect(jsonPath("$[1].descricao").isEmpty());
  }

  @Test
  void semSessaoResponde401() throws Exception {
    mvc.perform(get("/api/extrato"))
        .andExpect(status().isUnauthorized());
  }
}
