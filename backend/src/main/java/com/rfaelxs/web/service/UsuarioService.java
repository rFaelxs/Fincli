package com.rfaelxs.web.service;

import com.rfaelxs.web.domain.Reserva;
import com.rfaelxs.web.domain.Usuario;
import com.rfaelxs.web.repository.ReservaRepository;
import com.rfaelxs.web.repository.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cadastro e leitura de usuários. A autenticação em si é do Spring Security, pelo
 * {@code UsuarioDetailsService}.
 */
@Service
public class UsuarioService {

  private static final int TAMANHO_MINIMO_SENHA = 8;

  private final UsuarioRepository usuarioRepository;
  private final ReservaRepository reservaRepository;
  private final PasswordEncoder passwordEncoder;

  /** Cria o serviço com os repositórios e o codificador de senha. */
  public UsuarioService(
      UsuarioRepository usuarioRepository,
      ReservaRepository reservaRepository,
      PasswordEncoder passwordEncoder) {
    this.usuarioRepository = usuarioRepository;
    this.reservaRepository = reservaRepository;
    this.passwordEncoder = passwordEncoder;
  }

  /**
   * Cadastra um usuário e cria a Reserva de Emergência dele, na mesma transação.
   *
   * @param nome  nome de exibição
   * @param cpf   CPF com ou sem máscara; validado e gravado só com dígitos
   * @param senha senha em texto puro, gravada como hash BCrypt
   * @param email opcional
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
      // Dois cadastros simultâneos com o mesmo CPF: o índice único do banco é a autoridade.
      throw new ConflitoException("CPF já cadastrado.");
    }

    reservaRepository.save(Reserva.emergenciaDe(usuario));
    return usuario;
  }

  /**
   * Usuário da sessão, lido do banco a cada requisição (ADR 0008).
   *
   * @param id id interno, vindo da sessão
   * @return o usuário
   */
  @Transactional(readOnly = true)
  public Usuario buscar(Long id) {
    return usuarioRepository.findById(id).orElseThrow();
  }

  private void validarSenha(String senha) {
    if (senha == null || senha.length() < TAMANHO_MINIMO_SENHA) {
      throw new ValorInvalidoException(
          "A senha deve ter pelo menos " + TAMANHO_MINIMO_SENHA + " caracteres.");
    }
  }
}
