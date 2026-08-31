package com.rfaelxs.web.security;

import com.rfaelxs.web.domain.Usuario;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** Usuário autenticado, como o Spring Security o enxerga. O "username" é o CPF. */
public class UsuarioPrincipal implements UserDetails {

  private final transient Usuario usuario;

  public UsuarioPrincipal(Usuario usuario) {
    this.usuario = usuario;
  }

  public Usuario getUsuario() {
    return usuario;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_USUARIO"));
  }

  @Override
  public String getPassword() {
    return usuario.getSenhaHash();
  }

  @Override
  public String getUsername() {
    return usuario.getCpf();
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
}
