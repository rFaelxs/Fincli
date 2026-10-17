package com.rfaelxs.web.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rfaelxs.web.config.RelogioConfig;
import com.rfaelxs.web.domain.ContaPrevista;
import com.rfaelxs.web.domain.Usuario;
import com.rfaelxs.web.security.SecurityConfig;
import com.rfaelxs.web.security.UsuarioLogado;
import com.rfaelxs.web.service.ContaPrevistaService;
import com.rfaelxs.web.service.ItemExtrato;
import com.rfaelxs.web.service.RecursoNaoEncontradoException;
import com.rfaelxs.web.service.resumo.Gestao;
import com.rfaelxs.web.service.resumo.GestaoService;
import com.rfaelxs.web.service.resumo.Lancamento;
import com.rfaelxs.web.service.resumo.Totais;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

/** {@code GET /api/hoje} e as contas previstas. */
@WebMvcTest({ResumoController.class, ContaPrevistaController.class})
@Import({SecurityConfig.class, GestaoApiTest.Relogio.class})
class GestaoApiTest {

  private static final Long DONO = 7L;
  private static final UsuarioLogado SESSAO = new UsuarioLogado(DONO, "52998224725", null);
  private static final Usuario USUARIO = new Usuario("Rafael", "52998224725", "hash");

  @Autowired
  private MockMvc mvc;

  @MockBean
  private GestaoService gestaoService;

  @MockBean
  private ContaPrevistaService contaPrevistaService;

  @MockBean
  private UserDetailsService usuarios;

  @Test
  void hojeDevolveOsCamposDaSpec() throws Exception {
    YearMonth setembro = YearMonth.of(2026, 9);
    ContaPrevista energia =
        new ContaPrevista(USUARIO, "Energia", new BigDecimal("210.00"), 22, null);
    Lancamento aporte = new Lancamento(null, LocalDate.of(2026, 9, 16), "Viagem", "Reserva",
        false, Lancamento.Tipo.APORTE, new BigDecimal("300.00"), ItemExtrato.Origem.RESERVA, 1L);
    when(gestaoService.hoje(DONO)).thenReturn(new Gestao(LocalDate.of(2026, 9, 18), setembro,
        new BigDecimal("1014.00"), new BigDecimal("84.50"), 12,
        Totais.de(new BigDecimal("5000.00"), new BigDecimal("2500.00"),
            new BigDecimal("1000.00"), new BigDecimal("300.00")),
        new BigDecimal("210.00"), List.of(aporte), List.of(energia)));

    mvc.perform(get("/api/hoje").with(user(SESSAO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hoje").value("2026-09-18"))
        .andExpect(jsonPath("$.mes").value("2026-09"))
        .andExpect(jsonPath("$.fimDoMes").value("2026-09-30"))
        .andExpect(jsonPath("$.diasRestantes").value(12))
        .andExpect(jsonPath("$.livre").value(1014.00))
        .andExpect(jsonPath("$.porDia").value(84.50))
        .andExpect(jsonPath("$.entradas").value(5000.00))
        .andExpect(jsonPath("$.saidas").value(2500.00))
        .andExpect(jsonPath("$.essenciais").value(1000.00))
        .andExpect(jsonPath("$.superfluas").value(1500.00))
        .andExpect(jsonPath("$.aportes").value(300.00))
        .andExpect(jsonPath("$.sobra").value(2200.00))
        .andExpect(jsonPath("$.previstasTotal").value(210.00))
        .andExpect(jsonPath("$.ultimosLancamentos[0].tipo").value("APORTE"))
        .andExpect(jsonPath("$.ultimosLancamentos[0].id").isEmpty())
        .andExpect(jsonPath("$.ultimosLancamentos[0].origem").doesNotExist())
        .andExpect(jsonPath("$.previstas[0].nome").value("Energia"))
        .andExpect(jsonPath("$.previstas[0].vencimento").value("2026-09-22"))
        .andExpect(jsonPath("$.previstas[0].recorrente").value(true));
  }

  @Test
  void hojeSemSessaoResponde401() throws Exception {
    mvc.perform(get("/api/hoje"))
        .andExpect(status().isUnauthorized());
  }

  // CA-GESTAO-16: o desfazer de uma recorrente manda mesReferencia nulo
  @Test
  void recriaRecorrenteComVencimentoNoMesCorrente() throws Exception {
    when(contaPrevistaService.criar(DONO, "Energia", new BigDecimal("210.00"), 22, null))
        .thenReturn(new ContaPrevista(USUARIO, "Energia", new BigDecimal("210.00"), 22, null));

    mvc.perform(enviar("{\"nome\":\"Energia\",\"valor\":210.00,\"diaVencimento\":22,"
            + "\"mesReferencia\":null}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.vencimento").value("2026-09-22"))
        .andExpect(jsonPath("$.recorrente").value(true));
  }

  // CA-GESTAO-17 e CA-GESTAO-22
  @Test
  void recriaAvulsaComVencimentoNoMesDela() throws Exception {
    YearMonth fevereiro = YearMonth.of(2026, 2);
    when(contaPrevistaService.criar(eq(DONO), any(), any(), eq(31), eq(fevereiro)))
        .thenReturn(new ContaPrevista(USUARIO, "IPVA", BigDecimal.TEN, 31, fevereiro));

    mvc.perform(enviar("{\"nome\":\"IPVA\",\"valor\":10,\"diaVencimento\":31,"
            + "\"mesReferencia\":\"2026-02\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.vencimento").value("2026-02-28"))
        .andExpect(jsonPath("$.recorrente").value(false));
  }

  @Test
  void validaOsCamposDaConta() throws Exception {
    mvc.perform(enviar("{\"nome\":\"\",\"valor\":0,\"diaVencimento\":32}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campos.nome").value("Informe o nome da conta."))
        .andExpect(jsonPath("$.campos.valor")
            .value("O valor da conta prevista deve ser positivo."))
        .andExpect(jsonPath("$.campos.diaVencimento")
            .value("O dia de vencimento deve estar entre 1 e 31."));
  }

  // CA-GESTAO-15
  @Test
  void removeEResponde204() throws Exception {
    UUID id = UUID.randomUUID();

    mvc.perform(delete("/api/previstas/" + id).with(csrf()).with(user(SESSAO)))
        .andExpect(status().isNoContent());

    verify(contaPrevistaService).remover(DONO, id);
  }

  @Test
  void removerContaAlheiaResponde404() throws Exception {
    UUID alheia = UUID.randomUUID();
    doThrow(new RecursoNaoEncontradoException("Conta prevista não encontrada."))
        .when(contaPrevistaService).remover(DONO, alheia);

    mvc.perform(delete("/api/previstas/" + alheia).with(csrf()).with(user(SESSAO)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("Conta prevista não encontrada."));
  }

  private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder enviar(
      String corpo) {
    return post("/api/previstas").with(csrf()).with(user(SESSAO))
        .contentType(MediaType.APPLICATION_JSON)
        .content(corpo);
  }

  /** Relógio parado em 18/09/2026, no fuso da aplicação. */
  @TestConfiguration
  static class Relogio {
    @Bean
    Clock relogio() {
      return Clock.fixed(Instant.parse("2026-09-18T15:00:00Z"), RelogioConfig.FUSO);
    }
  }
}
