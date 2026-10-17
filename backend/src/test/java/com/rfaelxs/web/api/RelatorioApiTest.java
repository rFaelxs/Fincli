package com.rfaelxs.web.api;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rfaelxs.web.repository.TotalCategoria;
import com.rfaelxs.web.security.SecurityConfig;
import com.rfaelxs.web.security.UsuarioLogado;
import com.rfaelxs.web.service.resumo.GestaoService;
import com.rfaelxs.web.service.resumo.Relatorio;
import com.rfaelxs.web.service.resumo.Relatorio.Projecao;
import com.rfaelxs.web.service.resumo.Relatorio.RitmoReserva;
import com.rfaelxs.web.service.resumo.Relatorio.SobraMensal;
import com.rfaelxs.web.service.resumo.RelatorioService;
import com.rfaelxs.web.service.resumo.Totais;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ResumoController.class)
@Import(SecurityConfig.class)
class RelatorioApiTest {

  private static final UsuarioLogado SESSAO = new UsuarioLogado(7L, "52998224725", null);
  private static final YearMonth SETEMBRO = YearMonth.of(2026, 9);

  @Autowired
  private MockMvc mvc;

  @MockBean
  private RelatorioService relatorioService;

  @MockBean
  private GestaoService gestaoService;

  @MockBean
  private UserDetailsService usuarios;

  // CA-REL-01
  @Test
  void devolveOMesPedidoNoFormatoDaSpec() throws Exception {
    when(relatorioService.doMes(7L, SETEMBRO)).thenReturn(relatorio());

    mvc.perform(get("/api/relatorio").param("mes", "2026-09").with(user(SESSAO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.mes").value("2026-09"))
        .andExpect(jsonPath("$.superfluas").value(800.00))
        .andExpect(jsonPath("$.historico[0].mes").value("2026-09"))
        .andExpect(jsonPath("$.historico[0].sobra").value(710.00))
        .andExpect(jsonPath("$.topCategorias[0].nome").value("Mercado"))
        .andExpect(jsonPath("$.reservas[0].previsao").value("2027-09"))
        .andExpect(jsonPath("$.reservas[0].noRitmo").value(true))
        .andExpect(jsonPath("$.projecao.aindaVence").value(210.00))
        .andExpect(jsonPath("$.projecao.sobraPrevista").value(500.00));
  }

  @Test
  void semMesUsaOCorrente() throws Exception {
    when(relatorioService.doMesCorrente(7L)).thenReturn(relatorio());

    mvc.perform(get("/api/relatorio").with(user(SESSAO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.mes").value("2026-09"));
  }

  @Test
  void mesMalformadoResponde400() throws Exception {
    mvc.perform(get("/api/relatorio").param("mes", "setembro").with(user(SESSAO)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Parâmetro inválido: mes."));
  }

  @Test
  void semSessaoResponde401() throws Exception {
    mvc.perform(get("/api/relatorio"))
        .andExpect(status().isUnauthorized());
  }

  private static Relatorio relatorio() {
    Totais totais = Totais.de(new BigDecimal("3210.00"), new BigDecimal("2500.00"),
        new BigDecimal("1700.00"), BigDecimal.ZERO);
    return new Relatorio(SETEMBRO, totais,
        List.of(new SobraMensal(SETEMBRO, totais.sobra())),
        List.of(new TotalCategoria("Mercado", new BigDecimal("800.00"))),
        List.of(new RitmoReserva(UUID.randomUUID(), "Viagem", new BigDecimal("4000.00"),
            new BigDecimal("10000.00"), new BigDecimal("40.0"), new BigDecimal("500.00"),
            YearMonth.of(2027, 9), true)),
        new Projecao(totais.saidas(), new BigDecimal("210.00"), new BigDecimal("500.00")));
  }
}
