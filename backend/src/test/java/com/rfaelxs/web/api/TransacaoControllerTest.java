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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rfaelxs.web.domain.TipoTransacao;
import com.rfaelxs.web.domain.Transacao;
import com.rfaelxs.web.domain.Usuario;
import com.rfaelxs.web.security.SecurityConfig;
import com.rfaelxs.web.security.UsuarioLogado;
import com.rfaelxs.web.service.DadosTransacao;
import com.rfaelxs.web.service.RecursoNaoEncontradoException;
import com.rfaelxs.web.service.TransacaoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(TransacaoController.class)
@Import(SecurityConfig.class)
class TransacaoControllerTest {

  private static final Long DONO = 7L;
  private static final UsuarioLogado SESSAO = new UsuarioLogado(DONO, "52998224725", null);
  private static final String VALIDO = """
      {"valor":120.00,"categoria":"Alimentação","descricao":"Mercado",
       "data":"2026-09-16","tipo":"SAIDA","essencial":true}""";

  @Autowired
  private MockMvc mvc;

  @MockBean
  private TransacaoService transacaoService;

  @MockBean
  private UserDetailsService usuarios;

  @Test
  void listaComDataIsoEValorEmDuasCasas() throws Exception {
    Transacao mercado = new Transacao(new Usuario("Rafael", "52998224725", "hash"),
        new BigDecimal("120.00"), "Alimentação", null, LocalDate.of(2026, 9, 16),
        TipoTransacao.SAIDA, true);
    when(transacaoService.listar(DONO)).thenReturn(List.of(mercado));

    mvc.perform(get("/api/transacoes").with(user(SESSAO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(mercado.getPublicId().toString()))
        .andExpect(jsonPath("$[0].valor").value(120.00))
        .andExpect(jsonPath("$[0].categoria").value("Alimentação"))
        .andExpect(jsonPath("$[0].descricao").isEmpty())
        .andExpect(jsonPath("$[0].data").value("2026-09-16"))
        .andExpect(jsonPath("$[0].tipo").value("SAIDA"))
        .andExpect(jsonPath("$[0].essencial").value(true));
  }

  @Test
  void semSessaoResponde401() throws Exception {
    mvc.perform(get("/api/transacoes"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void criaComODonoDaSessaoEResponde201() throws Exception {
    when(transacaoService.criar(eq(DONO), any())).thenReturn(new Transacao(
        new Usuario("Rafael", "52998224725", "hash"), new BigDecimal("120.00"), "Alimentação",
        "Mercado", LocalDate.of(2026, 9, 16), TipoTransacao.SAIDA, true));

    mvc.perform(enviar(post("/api/transacoes"), VALIDO))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.descricao").value("Mercado"));

    verify(transacaoService).criar(DONO, new DadosTransacao(new BigDecimal("120.00"),
        "Alimentação", "Mercado", LocalDate.of(2026, 9, 16), TipoTransacao.SAIDA, true));
  }

  // Item 33 e CA-TRX-16
  @Test
  void valorNegativoTrazAFraseDaSpec() throws Exception {
    mvc.perform(enviar(post("/api/transacoes"), VALIDO.replace("120.00", "-10")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campos.valor").value("O valor da transação não pode ser negativo."));
  }

  // Item 33
  @Test
  void valorAusente() throws Exception {
    mvc.perform(enviar(post("/api/transacoes"), VALIDO.replace("\"valor\":120.00,", "")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campos.valor").value("Informe o valor."));
  }

  // Item 34 e CA-TRX-17
  @Test
  void categoriaEmBranco() throws Exception {
    mvc.perform(enviar(post("/api/transacoes"), VALIDO.replace("Alimentação", "  ")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campos.categoria").value("Informe a categoria."));
  }

  // Item 34
  @Test
  void categoriaLongaDemais() throws Exception {
    mvc.perform(enviar(post("/api/transacoes"), VALIDO.replace("Alimentação", "x".repeat(81))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campos.categoria")
            .value("A categoria deve ter no máximo 80 caracteres."));
  }

  // Item 35
  @Test
  void descricaoLongaDemais() throws Exception {
    mvc.perform(enviar(post("/api/transacoes"), VALIDO.replace("Mercado", "x".repeat(256))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campos.descricao")
            .value("A descrição deve ter no máximo 255 caracteres."));
  }

  // Item 36
  @Test
  void dataETipoAusentes() throws Exception {
    String semDataETipo = VALIDO
        .replace("\"data\":\"2026-09-16\",", "")
        .replace("\"tipo\":\"SAIDA\",", "");

    mvc.perform(enviar(post("/api/transacoes"), semDataETipo))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campos.data").value("Informe a data."))
        .andExpect(jsonPath("$.campos.tipo").value("Informe o tipo (ENTRADA ou SAIDA)."));
  }

  // Item 37 e CA-TRX-20
  @Test
  void transacaoDeOutroUsuarioResponde404() throws Exception {
    UUID alheia = UUID.randomUUID();
    when(transacaoService.buscar(DONO, alheia))
        .thenThrow(new RecursoNaoEncontradoException("Transação não encontrada."));

    mvc.perform(get("/api/transacoes/" + alheia).with(user(SESSAO)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("Transação não encontrada."));
  }

  @Test
  void edicaoDeTransacaoAlheiaResponde404() throws Exception {
    UUID alheia = UUID.randomUUID();
    when(transacaoService.editar(eq(DONO), eq(alheia), any()))
        .thenThrow(new RecursoNaoEncontradoException("Transação não encontrada."));

    mvc.perform(enviar(put("/api/transacoes/" + alheia), VALIDO))
        .andExpect(status().isNotFound());
  }

  @Test
  void idMalformadoResponde404() throws Exception {
    mvc.perform(get("/api/transacoes/abc").with(user(SESSAO)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("Recurso não encontrado."));
  }

  @Test
  void excluiEResponde204() throws Exception {
    UUID id = UUID.randomUUID();

    mvc.perform(delete("/api/transacoes/" + id).with(csrf()).with(user(SESSAO)))
        .andExpect(status().isNoContent());

    verify(transacaoService).excluir(DONO, id);
  }

  @Test
  void excluirTransacaoAlheiaResponde404() throws Exception {
    UUID alheia = UUID.randomUUID();
    doThrow(new RecursoNaoEncontradoException("Transação não encontrada."))
        .when(transacaoService).excluir(DONO, alheia);

    mvc.perform(delete("/api/transacoes/" + alheia).with(csrf()).with(user(SESSAO)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("Transação não encontrada."));
  }

  private static MockHttpServletRequestBuilder enviar(
      MockHttpServletRequestBuilder requisicao, String corpo) {
    return requisicao.with(csrf()).with(user(SESSAO))
        .contentType(MediaType.APPLICATION_JSON)
        .content(corpo);
  }
}
