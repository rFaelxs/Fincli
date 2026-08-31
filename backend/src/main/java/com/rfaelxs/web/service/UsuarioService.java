package com.rfaelxs.web.service;

import com.rfaelxs.web.domain.Usuario;
import com.rfaelxs.web.repository.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cadastro de usuários. A autenticação em si é feita pelo Spring Security
 * (ver {@code UsuarioDetailsService}); aqui ficam as regras do cadastro.
 */
@Service
public class UsuarioService {

  private static final int TAMANHO_MINIMO_SENHA = 8;

  private final UsuarioRepository usuarioRepository;
  private final ReservaService reservaService;
  private final PasswordEncoder passwordEncoder;

  public UsuarioService(
      UsuarioRepository usuarioRepository,
      ReservaService reservaService,
      PasswordEncoder passwordEncoder) {
    this.usuarioRepository = usuarioRepository;
    this.reservaService = reservaService;
    this.passwordEncoder = passwordEncoder;
  }

  /**
   * Cadastra um usuário e cria sua Reserva de Emergência, como fazia o CLI.
   *
   * @param nome  nome de exibição
   * @param cpf   CPF com ou sem máscara; validado e gravado apenas com dígitos
   * @param senha senha em texto puro, gravada como hash BCrypt
   * @param email opcional, usado apenas para futura redefinição de senha
   * @return o usuário criado
   * @throws ValorInvalidoException se o CPF ou a senha forem inválidos
   * @throws ConflitoException      se o CPF já estiver cadastrado
   */
  @Transactional
  public Usuario cadastrar(String nome, String cpf, String senha, String email) {
    String cpfNormalizado = Cpf.normalizar(cpf);
    validarSenha(senha);

    if (usuarioRepository.existsByCpf(cpfNormalizado)) {
      throw new ConflitoException("CPF já cadastrado.");
    }

    Usuario usuario = new Usuario(nome.trim(), cpfNormalizado, passwordEncoder.encode(senha));
    if (email != null && !email.isBlank()) {
      usuario.setEmail(email.trim());
    }

    try {
      usuarioRepository.saveAndFlush(usuario);
    } catch (DataIntegrityViolationException e) {
      // Duas requisições simultâneas com o mesmo CPF: o índice único do banco é a autoridade.
      throw new ConflitoException("CPF já cadastrado.");
    }

    reservaService.criarReservaDeEmergencia(usuario);
    return usuario;
  }

  private void validarSenha(String senha) {
    if (senha == null || senha.length() < TAMANHO_MINIMO_SENHA) {
      throw new ValorInvalidoException(
          "A senha deve ter pelo menos " + TAMANHO_MINIMO_SENHA + " caracteres.");
    }
  }
}
