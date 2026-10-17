package com.rfaelxs.web.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rfaelxs.web.security.SecurityConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest
@Import({SecurityConfig.class, TratadorDeErrosTest.RotasDeTeste.class})
class TratadorDeErrosTest {

  @Autowired
  private MockMvc mvc;

  @Test
  void semSessaoAApiResponde401EmProblemJson() throws Exception {
    mvc.perform(get("/api/teste/falha"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.detail").value("Autenticação necessária."));
  }

  @Test
  @WithMockUser
  void mutacaoSemTokenCsrfResponde403() throws Exception {
    mvc.perform(post("/api/teste/validacao")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"nome\":\"x\"}"))
        .andExpect(status().isForbidden())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
  }

  @Test
  @WithMockUser
  void validacaoDevolveAsMensagensPorCampo() throws Exception {
    mvc.perform(post("/api/teste/validacao").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"nome\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.detail").value("Verifique os campos enviados."))
        .andExpect(jsonPath("$.campos.nome").value("Informe o nome."));
  }

  @Test
  @WithMockUser
  void jsonMalformadoResponde400() throws Exception {
    mvc.perform(post("/api/teste/validacao").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("O corpo da requisição não pôde ser lido."));
  }

  @Test
  @WithMockUser
  void erroInesperadoNaoVazaAMensagemOriginal() throws Exception {
    mvc.perform(get("/api/teste/falha"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.detail")
            .value("Não foi possível concluir a operação. Tente novamente."));
  }

  @Test
  @WithMockUser
  void rotaInexistenteResponde404() throws Exception {
    mvc.perform(get("/api/nao-existe"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("Recurso não encontrado."));
  }

  record Corpo(@NotBlank(message = "Informe o nome.") String nome) {
  }

  @RestController
  static class RotasDeTeste {

    @PostMapping("/api/teste/validacao")
    void validacao(@Valid @RequestBody Corpo corpo) {
    }

    @GetMapping("/api/teste/falha")
    void falha() {
      throw new IllegalStateException("CPF 123.456.789-09 não deveria aparecer");
    }
  }
}
