package com.rfaelxs.web.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rfaelxs.web.domain.Usuario;
import com.rfaelxs.web.security.UsuarioPrincipal;
import com.rfaelxs.web.service.TransacaoService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

/** Contrato HTTP de {@code GET /api/saldo}; a conta em si é testada em TransacaoServiceTest. */
@WebMvcTest(controllers = SaldoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TratadorDeErros.class)
class SaldoControllerTest {

  @Autowired private MockMvc mvc;

  @MockBean private TransacaoService transacaoService;

  private Usuario usuario;

  @BeforeEach
  void autenticar() {
    usuario = new Usuario("Rafael Siqueira", "11144477735", "hash");
    TestSecurityContextHolder.setAuthentication(
        new UsernamePasswordAuthenticationToken(
            new UsuarioPrincipal(usuario), "n/a", List.of()));
  }

  @AfterEach
  void limpar() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void devolveOSaldoDisponivelDoUsuarioDaSessao() throws Exception {
    when(transacaoService.calcularSaldoDisponivel(usuario)).thenReturn(new BigDecimal("2000.00"));

    mvc.perform(get("/api/saldo"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.saldoDisponivel").value(2000.00))
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void devolveSaldoNegativoSemAjuste() throws Exception {
    when(transacaoService.calcularSaldoDisponivel(usuario)).thenReturn(new BigDecimal("-150.00"));

    mvc.perform(get("/api/saldo"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.saldoDisponivel").value(-150.00));
  }
}
