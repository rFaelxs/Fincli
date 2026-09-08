package com.rfaelxs.web.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rfaelxs.web.domain.ContaPrevista;
import com.rfaelxs.web.domain.Usuario;
import com.rfaelxs.web.repository.TotalCategoria;
import com.rfaelxs.web.security.UsuarioPrincipal;
import com.rfaelxs.web.service.ResumoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Contrato HTTP das telas Hoje e Relatório.
 *
 * <p>Os filtros de segurança ficam de fora: o que se verifica aqui é a forma do JSON e o
 * repasse do usuário da sessão para o serviço. A autorização por dono é testada onde ela
 * mora — na consulta por id público mais usuário.
 */
@WebMvcTest(controllers = ResumoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TratadorDeErros.class)
class ResumoControllerTest {

  @Autowired private MockMvc mvc;

  @MockBean private ResumoService resumoService;

  private Usuario usuario;

  @BeforeEach
  void autenticar() {
    usuario = new Usuario("Rafael Siqueira", "11144477735", "hash");
    TestSecurityContextHolder.setAuthentication(
        new UsernamePasswordAuthenticationToken(
            new UsuarioPrincipal(usuario), "n/a", List.of()));
  }

  @org.junit.jupiter.api.AfterEach
  void limpar() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void hojeDevolveONumeroLivreEAsListas() throws Exception {
    when(resumoService.hoje(any())).thenReturn(hoje());

    mvc.perform(get("/api/hoje"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.mes").value("2026-09"))
        .andExpect(jsonPath("$.fimDoMes").value("2026-09-30"))
        .andExpect(jsonPath("$.diasRestantes").value(14))
        .andExpect(jsonPath("$.livre").value(2376.60))
        .andExpect(jsonPath("$.porDia").value(169.76))
        .andExpect(jsonPath("$.superfluas").value(813.40))
        .andExpect(jsonPath("$.previstasTotal").value(1190.00))
        .andExpect(jsonPath("$.ultimosLancamentos[0].tipo").value("SAIDA"))
        .andExpect(jsonPath("$.ultimosLancamentos[0].descricao").value("Mercado do mês"))
        .andExpect(jsonPath("$.previstas[0].nome").value("Cartão de crédito"))
        .andExpect(jsonPath("$.previstas[0].vencimento").value("2026-09-20"));
  }

  @Test
  void relatorioAceitaOMesNaQueryString() throws Exception {
    when(resumoService.relatorio(any(), eq(YearMonth.of(2026, 8)))).thenReturn(relatorio());

    mvc.perform(get("/api/relatorio").param("mes", "2026-08"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.mes").value("2026-08"))
        .andExpect(jsonPath("$.historico[0].mes").value("2026-08"))
        .andExpect(jsonPath("$.topCategorias[0].nome").value("Moradia"))
        .andExpect(jsonPath("$.reservas[0].previsao").value("2027-07"))
        .andExpect(jsonPath("$.projecao.sobraPrevista").value(3686.60));
  }

  @Test
  void reservaParadaVaiComPrevisaoNula() throws Exception {
    when(resumoService.relatorio(any(), any())).thenReturn(relatorioComReservaParada());

    mvc.perform(get("/api/relatorio"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reservas[0].previsao").doesNotExist())
        .andExpect(jsonPath("$.reservas[0].noRitmo").value(false));
  }

  // ── dados de apoio ──

  private ResumoService.Hoje hoje() {
    YearMonth setembro = YearMonth.of(2026, 9);
    ContaPrevista cartao =
        new ContaPrevista(usuario, "Cartão de crédito", new BigDecimal("980.00"), 20, null);
    ContaPrevista energia =
        new ContaPrevista(usuario, "Energia", new BigDecimal("210.00"), 22, null);

    return new ResumoService.Hoje(
        LocalDate.of(2026, 9, 16),
        setembro,
        new BigDecimal("2376.60"),
        new BigDecimal("169.76"),
        14,
        totais(),
        new BigDecimal("1190.00"),
        List.of(new ResumoService.Lancamento(
            UUID.randomUUID(),
            LocalDate.of(2026, 9, 3),
            "Mercado do mês",
            "Alimentação",
            true,
            ResumoService.TipoLancamento.SAIDA,
            new BigDecimal("742.18"))),
        List.of(cartao, energia));
  }

  private ResumoService.Relatorio relatorio() {
    return new ResumoService.Relatorio(
        YearMonth.of(2026, 8),
        totais(),
        List.of(new ResumoService.SobraMensal(
            YearMonth.of(2026, 8), new BigDecimal("3566.60"))),
        List.of(new TotalCategoria("Moradia", new BigDecimal("1269.90"))),
        List.of(new ResumoService.RitmoReserva(
            UUID.randomUUID(),
            "Viagem Chile",
            new BigDecimal("3000.00"),
            new BigDecimal("6000.00"),
            new BigDecimal("50.0"),
            new BigDecimal("300.00"),
            YearMonth.of(2027, 7),
            true)),
        new ResumoService.Projecao(
            new BigDecimal("2813.40"), new BigDecimal("980.00"), new BigDecimal("3686.60")));
  }

  private ResumoService.Relatorio relatorioComReservaParada() {
    return new ResumoService.Relatorio(
        YearMonth.of(2026, 9),
        totais(),
        List.of(),
        List.of(),
        List.of(new ResumoService.RitmoReserva(
            UUID.randomUUID(),
            "Curso de inglês",
            new BigDecimal("640.00"),
            new BigDecimal("2000.00"),
            new BigDecimal("32.0"),
            new BigDecimal("0.00"),
            null,
            false)),
        new ResumoService.Projecao(
            new BigDecimal("0.00"), new BigDecimal("0.00"), new BigDecimal("0.00")));
  }

  private static ResumoService.Totais totais() {
    return new ResumoService.Totais(
        new BigDecimal("7480.00"),
        new BigDecimal("2813.40"),
        new BigDecimal("2000.00"),
        new BigDecimal("813.40"),
        new BigDecimal("1100.00"),
        new BigDecimal("3566.60"));
  }
}
