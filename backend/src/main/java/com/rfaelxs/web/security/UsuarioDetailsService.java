package com.rfaelxs.web.security;

import com.rfaelxs.web.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Carrega o usuário pelo CPF para a autenticação do Spring Security. */
@Service
public class UsuarioDetailsService implements UserDetailsService {

  private final UsuarioRepository usuarioRepository;

  public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
    this.usuarioRepository = usuarioRepository;
  }

  /**
   * {@inheritDoc}
   *
   * <p>A mensagem é sempre genérica: dizer "CPF não encontrado" permitiria descobrir quem tem
   * conta na aplicação apenas testando CPFs (escopo-web.md §1.1).
   */
  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String cpf) throws UsernameNotFoundException {
    return usuarioRepository
        .findByCpf(cpf == null ? "" : cpf.replaceAll("\\D", ""))
        .map(UsuarioPrincipal::new)
        .orElseThrow(() -> new UsernameNotFoundException("CPF ou senha inválidos."));
  }
}
