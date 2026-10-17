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

import com.rfaelxs.web.domain.Reserva;
import com.rfaelxs.web.domain.Usuario;
import com.rfaelxs.web.security.SecurityConfig;
import com.rfaelxs.web.security.UsuarioLogado;
import com.rfaelxs.web.service.ConflitoException;
import com.rfaelxs.web.service.RecursoNaoEncontradoException;
import com.rfaelxs.web.service.ReservaService;
import com.rfaelxs.web.service.SaldoService;
import com.rfaelxs.web.service.ValorInvalidoException;
import java.math.BigDecimal;
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

@WebMvcTest({ReservaController.class, SaldoController.class})
@Import(SecurityConfig.class)
class ReservaControllerTest {

  private static final Long DONO = 7L;
  private static final UsuarioLogado SESSAO = new UsuarioLogado(DONO, "52998224725", null);
  private static final Usuario USUARIO = new Usuario("Rafael", "52998224725", "hash");

  @Autowired
  private MockMvc mvc;

  @MockBean
  private ReservaService reservaService;

  @MockBean
  private SaldoService saldoService;

  @MockBean
  private UserDetailsService usuarios;

  @Test
  void listaComProgressoCalculadoPeloServidor() throws Exception {
    Reserva emergencia = Reserva.emergenciaDe(USUARIO);
    Reserva viagem = new Reserva(USUARIO, "Viagem", new BigDecimal("10000.00"), false);
    viagem.alocar(new BigDecimal("1250.00"));
    when(reservaService.listar(DONO)).thenReturn(List.of(emergencia, viagem));

    mvc.perform(get("/api/reservas").with(user(SESSAO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].nome").value("Reserva de Emergência"))
        .andExpect(jsonPath("$[0].emergencia").value(true))
        .andExpect(jsonPath("$[0].progresso").value(0.0))
        .andExpect(jsonPath("$[1].id").value(viagem.getPublicId().toString()))
        .andExpect(jsonPath("$[1].metaValor").value(10000.00))
        .andExpect(jsonPath("$[1].saldoAtual").value(1250.00))
        .andExpect(jsonPath("$[1].progresso").value(12.5));
  }

  // CA-RES-24
  @Test
  void saldoDisponivel() throws Exception {
    when(saldoService.disponivel(DONO)).thenReturn(new BigDecimal("2000.00"));

    mvc.perform(get("/api/saldo").with(user(SESSAO)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.saldoDisponivel").value(2000.00));
  }

  @Test
  void saldoSemSessaoResponde401() throws Exception {
    mvc.perform(get("/api/saldo"))
        .andExpect(status().isUnauthorized());
  }

  // CA-RES-11
  @Test
  void criaEResponde201() throws Exception {
    when(reservaService.criar(DONO, "Viagem", new BigDecimal("8000.00")))
        .thenReturn(new Reserva(USUARIO, "Viagem", new BigDecimal("8000.00"), false));

    mvc.perform(enviar(post("/api/reservas"), "{\"nome\":\"Viagem\",\"metaValor\":8000.00}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.nome").value("Viagem"));
  }

  // Item 29 e CA-RES-12
  @Test
  void metaNegativaNaCriacaoTrazAFraseDaSpec() throws Exception {
    mvc.perform(enviar(post("/api/reservas"), "{\"nome\":\"Viagem\",\"metaValor\":-1}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campos.metaValor")
            .value("A meta da reserva não pode ser negativa."));
  }

  // Item 30 e CA-RES-13
  @Test
  void nomeEmBranco() throws Exception {
    mvc.perform(enviar(post("/api/reservas"), "{\"nome\":\" \",\"metaValor\":10}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campos.nome").value("Informe o nome da reserva."));
  }

  // Item 30
  @Test
  void nomeLongoDemais() throws Exception {
    mvc.perform(enviar(post("/api/reservas"),
            "{\"nome\":\"" + "x".repeat(121) + "\",\"metaValor\":10}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campos.nome").value("O nome deve ter no máximo 120 caracteres."));
  }

  // Item 31
  @Test
  void metaAusente() throws Exception {
    mvc.perform(enviar(post("/api/reservas"), "{\"nome\":\"Viagem\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campos.metaValor").value("Informe a meta."));
  }

  // Item 31
  @Test
  void valorAusenteAoAlocar() throws Exception {
    mvc.perform(enviar(post("/api/reservas/" + UUID.randomUUID() + "/alocar"), "{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campos.valor").value("Informe o valor."));
  }

  // CA-RES-18
  @Test
  void alocacaoAcimaDoDisponivelResponde400() throws Exception {
    UUID id = UUID.randomUUID();
    when(reservaService.alocar(DONO, id, new BigDecimal("300.00")))
        .thenThrow(new ValorInvalidoException("Saldo insuficiente para esta alocação."));

    mvc.perform(enviar(post("/api/reservas/" + id + "/alocar"), "{\"valor\":300.00}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Saldo insuficiente para esta alocação."));
  }

  @Test
  void sacaEDevolveAReserva() throws Exception {
    Reserva viagem = new Reserva(USUARIO, "Viagem", new BigDecimal("10000.00"), false);
    when(reservaService.sacar(DONO, viagem.getPublicId(), new BigDecimal("40")))
        .thenReturn(viagem);

    mvc.perform(enviar(post("/api/reservas/" + viagem.getPublicId() + "/sacar"),
            "{\"valor\":40}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nome").value("Viagem"));
  }

  // CA-RES-15
  @Test
  void alteraAMeta() throws Exception {
    Reserva viagem = new Reserva(USUARIO, "Viagem", new BigDecimal("12000.00"), false);
    when(reservaService.alterarMeta(eq(DONO), eq(viagem.getPublicId()), any()))
        .thenReturn(viagem);

    mvc.perform(enviar(put("/api/reservas/" + viagem.getPublicId() + "/meta"),
            "{\"valor\":12000.00}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.metaValor").value(12000.00));
  }

  // Item 37 e CA-RES-23
  @Test
  void reservaDeOutroUsuarioResponde404() throws Exception {
    UUID alheia = UUID.randomUUID();
    when(reservaService.buscar(DONO, alheia))
        .thenThrow(new RecursoNaoEncontradoException("Reserva não encontrada."));

    mvc.perform(get("/api/reservas/" + alheia).with(user(SESSAO)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("Reserva não encontrada."));
  }

  // CA-RES-08
  @Test
  void excluiEResponde204() throws Exception {
    UUID id = UUID.randomUUID();

    mvc.perform(delete("/api/reservas/" + id).with(csrf()).with(user(SESSAO)))
        .andExpect(status().isNoContent());

    verify(reservaService).excluir(DONO, id);
  }

  // CA-RES-10
  @Test
  void excluirAEmergenciaResponde409() throws Exception {
    UUID id = UUID.randomUUID();
    doThrow(new ConflitoException("A Reserva de Emergência não pode ser excluída."))
        .when(reservaService).excluir(DONO, id);

    mvc.perform(delete("/api/reservas/" + id).with(csrf()).with(user(SESSAO)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.detail").value("A Reserva de Emergência não pode ser excluída."));
  }

  private static MockHttpServletRequestBuilder enviar(
      MockHttpServletRequestBuilder requisicao, String corpo) {
    return requisicao.with(csrf()).with(user(SESSAO))
        .contentType(MediaType.APPLICATION_JSON)
        .content(corpo);
  }
}
