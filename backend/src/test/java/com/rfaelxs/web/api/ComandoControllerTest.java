package com.rfaelxs.web.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rfaelxs.web.domain.Usuario;
import com.rfaelxs.web.security.UsuarioPrincipal;
import com.rfaelxs.web.service.ComandoNaoEntendidoException;
import com.rfaelxs.web.service.ComandoParser;
import com.rfaelxs.web.service.ComandoService;
import com.rfaelxs.web.service.ValorInvalidoException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

/** Contrato HTTP da barra de comando, incluindo os caminhos de erro que a tela mostra. */
@WebMvcTest(controllers = ComandoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TratadorDeErros.class)
class ComandoControllerTest {

  @Autowired private MockMvc mvc;

  @MockBean private ComandoService comandoService;

  @BeforeEach
  void autenticar() {
    TestSecurityContextHolder.setAuthentication(
        new UsernamePasswordAuthenticationToken(
            new UsuarioPrincipal(new Usuario("Rafael Siqueira", "11144477735", "hash")),
            "n/a",
            List.of()));
  }

  @AfterEach
  void limpar() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void saidaVoltaComAMensagemDaFaixaDeConfirmacao() throws Exception {
    UUID id = UUID.randomUUID();
    when(comandoService.executar(any(), eq("mercado 120 ontem"))).thenReturn(
        new ComandoService.Resultado(
            ComandoParser.Tipo.SAIDA, id, null, new BigDecimal("120.00"), "Mercado",
            "Alimentação"));

    mvc.perform(json("mercado 120 ontem"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tipo").value("SAIDA"))
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.reservaId").doesNotExist())
        .andExpect(jsonPath("$.mensagem").value(
            org.hamcrest.Matchers.startsWith("Lançado R$")))
        .andExpect(jsonPath("$.mensagem").value(
            org.hamcrest.Matchers.endsWith("· Mercado")));
  }

  @Test
  void aporteVoltaComAReservaParaODesfazer() throws Exception {
    UUID reserva = UUID.randomUUID();
    when(comandoService.executar(any(), any())).thenReturn(
        new ComandoService.Resultado(
            ComandoParser.Tipo.APORTE, null, reserva, new BigDecimal("300.00"),
            "Viagem Chile", "Aporte"));

    mvc.perform(json("guardar 300 viagem"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tipo").value("APORTE"))
        .andExpect(jsonPath("$.id").doesNotExist())
        .andExpect(jsonPath("$.reservaId").value(reserva.toString()))
        .andExpect(jsonPath("$.mensagem").value(
            org.hamcrest.Matchers.startsWith("Guardado R$")));
  }

  @Test
  void textoQueNaoViraLancamentoDa422ComOMotivo() throws Exception {
    when(comandoService.executar(any(), any()))
        .thenThrow(new ComandoNaoEntendidoException(
            "Não entendi \"blablabla\" — tente algo como `mercado 120 ontem`."));

    mvc.perform(json("blablabla"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.title").value("Comando não entendido"))
        .andExpect(jsonPath("$.detail").value(
            org.hamcrest.Matchers.containsString("blablabla")));
  }

  @Test
  void regraDeNegocioVioladaContinuaSendo400() throws Exception {
    when(comandoService.executar(any(), any()))
        .thenThrow(new ValorInvalidoException("Saldo insuficiente para esta alocação."));

    mvc.perform(json("guardar 999999 viagem"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Saldo insuficiente para esta alocação."));
  }

  /** Regressão: sem handler próprio, a exceção caía na rede de segurança e virava 500. */
  @Test
  void caminhoInexistenteDa404ENao500() throws Exception {
    mvc.perform(post("/api/comando/inexistente")
            .contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void textoVazioNemChegaAoServico() throws Exception {
    mvc.perform(json("   "))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campos.texto").value("Escreva o que você quer lançar."));

    Mockito.verify(comandoService, Mockito.never()).executar(any(), any());
  }

  private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder json(
      String texto) {
    return post("/api/comando")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"texto\":\"" + texto + "\"}");
  }
}
