package com.rfaelxs.web.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rfaelxs.web.domain.Usuario;
import com.rfaelxs.web.repository.UsuarioRepository;
import com.rfaelxs.web.security.SecurityConfig;
import com.rfaelxs.web.security.UsuarioDetailsService;
import com.rfaelxs.web.security.UsuarioLogado;
import com.rfaelxs.web.service.ConflitoException;
import com.rfaelxs.web.service.UsuarioService;
import com.rfaelxs.web.service.ValorInvalidoException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, UsuarioDetailsService.class})
class AuthControllerTest {

  private static final String CPF = "52998224725";
  private static final String SENHA = "segredo123";

  @Autowired
  private MockMvc mvc;

  @MockBean
  private UsuarioService usuarioService;

  @MockBean
  private UsuarioRepository usuarioRepository;

  private Usuario rafael;

  @BeforeEach
  void setUp() {
    rafael = new Usuario("Rafael Siqueira", CPF, new BCryptPasswordEncoder().encode(SENHA));
    ReflectionTestUtils.setField(rafael, "id", 7L);
    when(usuarioRepository.findByCpf(CPF)).thenReturn(Optional.of(rafael));
    when(usuarioService.buscar(7L)).thenReturn(rafael);
  }

  // O csrf() dos outros testes troca, no filtro compartilhado, o repositório de token por um de
  // teste que não grava cookie. Um contexto limpo testa o repositório de verdade.
  @Test
  @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
  void csrfEmiteOCookieSemCorpo() throws Exception {
    mvc.perform(get("/api/csrf"))
        .andExpect(status().isNoContent())
        .andExpect(cookie().exists("XSRF-TOKEN"));
  }

  // CA-AUTH-03
  @Test
  void loginAceitaCpfComMascaraEDevolveOUsuario() throws Exception {
    mvc.perform(login("529.982.247-25", SENHA))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(rafael.getPublicId().toString()))
        .andExpect(jsonPath("$.nome").value("Rafael Siqueira"));
  }

  // CA-AUTH-04
  @Test
  void loginComCpfInexistenteResponde401() throws Exception {
    mvc.perform(login("123.456.789-09", SENHA))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.detail").value("CPF ou senha inválidos."));
  }

  // CA-AUTH-05
  @Test
  void loginComSenhaErradaTemAMesmaMensagem() throws Exception {
    mvc.perform(login(CPF, "outra-senha"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.detail").value("CPF ou senha inválidos."));
  }

  // CA-AUTH-14
  @Test
  void loginInvalidaASessaoAnterior() throws Exception {
    MockHttpSession anterior = new MockHttpSession();

    mvc.perform(login(CPF, SENHA).session(anterior))
        .andExpect(status().isOk());

    assertThat(anterior.isInvalid()).isTrue();
  }

  @Test
  void loginSemTokenCsrfResponde403() throws Exception {
    mvc.perform(post("/api/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(credenciais(CPF, SENHA)))
        .andExpect(status().isForbidden());
  }

  @Test
  void meDevolveOUsuarioDaSessaoSemOCpf() throws Exception {
    mvc.perform(get("/api/me").with(user(new UsuarioLogado(7L, CPF, null))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nome").value("Rafael Siqueira"))
        .andExpect(jsonPath("$.cpf").doesNotExist());
  }

  // CA-SHELL-01
  @Test
  void meSemSessaoResponde401() throws Exception {
    mvc.perform(get("/api/me"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void logoutResponde204() throws Exception {
    mvc.perform(post("/api/logout").with(csrf()).with(user(new UsuarioLogado(7L, CPF, null))))
        .andExpect(status().isNoContent());
  }

  // CA-AUTH-12
  @Test
  void cadastroResponde201SemOCpf() throws Exception {
    when(usuarioService.cadastrar(any(), any(), any(), any())).thenReturn(rafael);

    mvc.perform(cadastro("{\"nome\":\"Rafael Siqueira\",\"cpf\":\"52998224725\","
            + "\"senha\":\"segredo123\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.nome").value("Rafael Siqueira"))
        .andExpect(jsonPath("$.cpf").doesNotExist());
  }

  // CA-AUTH-06
  @Test
  void cadastroComCpfInvalidoResponde400() throws Exception {
    when(usuarioService.cadastrar(any(), any(), any(), any()))
        .thenThrow(new ValorInvalidoException("CPF inválido."));

    mvc.perform(cadastro("{\"nome\":\"Rafael\",\"cpf\":\"111.111.111-11\","
            + "\"senha\":\"segredo123\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("CPF inválido."));
  }

  // CA-AUTH-07
  @Test
  void cadastroComCpfRepetidoResponde409() throws Exception {
    when(usuarioService.cadastrar(any(), any(), any(), any()))
        .thenThrow(new ConflitoException("CPF já cadastrado."));

    mvc.perform(cadastro("{\"nome\":\"Rafael\",\"cpf\":\"52998224725\","
            + "\"senha\":\"segredo123\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.detail").value("CPF já cadastrado."));
  }

  // CA-AUTH-08
  @Test
  void cadastroComSenhaCurtaResponde400() throws Exception {
    mvc.perform(cadastro("{\"nome\":\"Rafael\",\"cpf\":\"52998224725\",\"senha\":\"1234567\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.campos.senha").value("A senha deve ter entre 8 e 100 caracteres."));
  }

  // CA-AUTH-09
  @Test
  void cadastroComCamposEmBrancoListaAsMensagens() throws Exception {
    mvc.perform(cadastro("{\"nome\":\"\",\"cpf\":\"\",\"senha\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Verifique os campos enviados."))
        .andExpect(jsonPath("$.campos.nome").value("Informe seu nome."))
        .andExpect(jsonPath("$.campos.cpf").value("Informe seu CPF."));
  }

  private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder login(
      String cpf, String senha) {
    return post("/api/login").with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(credenciais(cpf, senha));
  }

  private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
      cadastro(String corpo) {
    return post("/api/cadastro").with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(corpo);
  }

  private static String credenciais(String cpf, String senha) {
    return "{\"cpf\":\"%s\",\"senha\":\"%s\"}".formatted(cpf, senha);
  }
}
