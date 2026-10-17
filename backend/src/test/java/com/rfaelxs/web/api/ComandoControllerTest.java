package com.rfaelxs.web.api;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rfaelxs.web.security.SecurityConfig;
import com.rfaelxs.web.security.UsuarioLogado;
import com.rfaelxs.web.service.ComandoNaoEntendidoException;
import com.rfaelxs.web.service.comando.ComandoParser.Tipo;
import com.rfaelxs.web.service.comando.ComandoService;
import com.rfaelxs.web.service.comando.ComandoService.Resultado;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(ComandoController.class)
@Import(SecurityConfig.class)
class ComandoControllerTest {

  private static final UsuarioLogado SESSAO = new UsuarioLogado(7L, "52998224725", null);

  @Autowired
  private MockMvc mvc;

  @MockBean
  private ComandoService comandoService;

  @MockBean
  private UserDetailsService usuarios;

  // CA-CMD-25: espaço comum depois de "R$", não o inseparável do formato de moeda do Java
  @Test
  void saidaVoltaComAMensagemDaFaixa() throws Exception {
    UUID id = UUID.randomUUID();
    when(comandoService.executar(7L, "mercado 120")).thenReturn(new Resultado(Tipo.SAIDA, id,
        null, new BigDecimal("120.00"), "Mercado", "Alimentação"));

    enviar("mercado 120")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tipo").value("SAIDA"))
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.reservaId").isEmpty())
        .andExpect(jsonPath("$.mensagem").value("Lançado R$ 120,00 · Mercado"));
  }

  // CA-CMD-26 e item 28
  @Test
  void cadaTipoTemOSeuTempoVerbal() throws Exception {
    when(comandoService.executar(7L, "recebi 450 freela")).thenReturn(new Resultado(
        Tipo.ENTRADA, UUID.randomUUID(), null, new BigDecimal("450"), "Freela", "Trabalho"));
    when(comandoService.executar(7L, "guardar 1234,5")).thenReturn(new Resultado(
        Tipo.APORTE, null, UUID.randomUUID(), new BigDecimal("1234.50"), "Viagem", "Aporte"));

    enviar("recebi 450 freela")
        .andExpect(jsonPath("$.mensagem").value("Recebido R$ 450,00 · Freela"));
    enviar("guardar 1234,5")
        .andExpect(jsonPath("$.mensagem").value("Guardado R$ 1.234,50 · Viagem"))
        .andExpect(jsonPath("$.id").isEmpty());
  }

  // CA-CMD-10
  @Test
  void textoNaoEntendidoResponde422() throws Exception {
    when(comandoService.executar(7L, "mercado sem numero")).thenThrow(
        new ComandoNaoEntendidoException("Não entendi \"mercado sem numero\" — tente algo."));

    enviar("mercado sem numero")
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.detail").value("Não entendi \"mercado sem numero\" — tente algo."));
  }

  // Item 36
  @Test
  void textoEmBrancoOuLongoDemaisResponde400() throws Exception {
    enviar("  ")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campos.texto").value("Escreva o que você quer lançar."));
    enviar("x".repeat(256))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campos.texto")
            .value("O comando deve ter no máximo 255 caracteres."));
  }

  private ResultActions enviar(String texto) throws Exception {
    return mvc.perform(post("/api/comando").with(csrf()).with(user(SESSAO))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"texto\":\"" + texto + "\"}"));
  }
}
