package com.rfaelxs.web.security;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Usuário autenticado, como fica guardado na sessão. O "username" é o CPF.
 *
 * <p>Guarda só o id interno, e não a entidade: a sessão pode ser serializada, e um nome guardado
 * aqui ficaria velho. Quem precisa do usuário o busca no banco pelo id (ADR 0008). O hash da senha
 * existe só durante a autenticação e é apagado logo depois.
 */
public class UsuarioLogado implements UserDetails, CredentialsContainer {

  private static final long serialVersionUID = 1L;

  private final Long id;
  private final String cpf;
  private String senhaHash;

  /**
   * Cria o principal da sessão.
   *
   * @param id        id interno do usuário
   * @param cpf       só os 11 dígitos
   * @param senhaHash hash BCrypt, conferido no login e apagado em seguida
   */
  public UsuarioLogado(Long id, String cpf, String senhaHash) {
    this.id = id;
    this.cpf = cpf;
    this.senhaHash = senhaHash;
  }

  public Long getId() {
    return id;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_USUARIO"));
  }

  @Override
  public String getPassword() {
    return senhaHash;
  }

  @Override
  public String getUsername() {
    return cpf;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public void eraseCredentials() {
    senhaHash = null;
  }
}
