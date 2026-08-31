package com.rfaelxs.web.api;

import com.rfaelxs.web.api.dto.CadastroRequest;
import com.rfaelxs.web.api.dto.LoginRequest;
import com.rfaelxs.web.api.dto.UsuarioResponse;
import com.rfaelxs.web.domain.Usuario;
import com.rfaelxs.web.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Cadastro, login e identidade da sessão. */
@RestController
@RequestMapping("/api")
public class AuthController {

  private final UsuarioService usuarioService;
  private final AuthenticationManager authenticationManager;
  private final SecurityContextRepository contextRepository =
      new HttpSessionSecurityContextRepository();

  public AuthController(UsuarioService usuarioService, AuthenticationManager authenticationManager) {
    this.usuarioService = usuarioService;
    this.authenticationManager = authenticationManager;
  }

  /**
   * Cadastra um usuário e já cria sua Reserva de Emergência.
   *
   * @return 201 com o usuário criado; 409 se o CPF já existir; 400 se CPF ou senha forem inválidos
   */
  @PostMapping("/cadastro")
  public ResponseEntity<UsuarioResponse> cadastrar(@Valid @RequestBody CadastroRequest req) {
    Usuario usuario =
        usuarioService.cadastrar(req.nome(), req.cpf(), req.senha(), req.email());
    return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponse.de(usuario));
  }

  /**
   * Autentica e abre a sessão.
   *
   * @return 200 com o usuário; 401 se as credenciais não conferirem
   */
  @PostMapping("/login")
  public UsuarioResponse login(
      @Valid @RequestBody LoginRequest req,
      HttpServletRequest request,
      HttpServletResponse response) {

    Authentication autenticacao =
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(
                req.cpf().replaceAll("\\D", ""), req.senha()));

    // Descarta qualquer sessão anterior antes de gravar a nova: sem isso, um id de sessão
    // plantado no navegador da vítima continuaria válido depois do login (session fixation).
    HttpSession sessaoAnterior = request.getSession(false);
    if (sessaoAnterior != null) {
      sessaoAnterior.invalidate();
    }
    request.getSession(true);

    SecurityContext contexto = SecurityContextHolder.createEmptyContext();
    contexto.setAuthentication(autenticacao);
    SecurityContextHolder.setContext(contexto);
    contextRepository.saveContext(contexto, request, response);

    return UsuarioResponse.de(((com.rfaelxs.web.security.UsuarioPrincipal)
        autenticacao.getPrincipal()).getUsuario());
  }

  /**
   * Usuário da sessão corrente. O SPA chama isto ao carregar para saber se já está logado.
   *
   * @return 200 com o usuário; 401 se não houver sessão
   */
  @GetMapping("/me")
  public UsuarioResponse me(@UsuarioAtual Usuario usuario) {
    return UsuarioResponse.de(usuario);
  }

  /**
   * Emite o cookie {@code XSRF-TOKEN}. O SPA chama antes do primeiro POST.
   *
   * @return 204 — o token vai no cookie, não no corpo
   */
  @GetMapping("/csrf")
  public ResponseEntity<Void> csrf() {
    return ResponseEntity.noContent().build();
  }
}
