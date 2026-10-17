package com.rfaelxs.web.repository;

import com.rfaelxs.web.domain.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

  Optional<Usuario> findByCpf(String cpf);

  boolean existsByCpf(String cpf);
}
