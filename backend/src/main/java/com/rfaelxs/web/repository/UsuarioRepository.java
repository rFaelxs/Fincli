package com.rfaelxs.web.repository;

import com.rfaelxs.web.domain.Usuario;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acesso aos usuários. O CPF chega aqui sempre normalizado, só com dígitos. */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

  Optional<Usuario> findByCpf(String cpf);

  boolean existsByCpf(String cpf);

  /**
   * Trava a linha do usuário até o fim da transação.
   *
   * <p>O saldo disponível é do usuário, não de uma reserva. Travar só a reserva deixaria duas
   * alocações simultâneas em reservas diferentes lerem o mesmo saldo e passarem juntas. Com o
   * usuário travado, as operações de reserva de um mesmo usuário entram em fila (ADR 0008).
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from Usuario u where u.id = :id")
  Optional<Usuario> travar(@Param("id") Long id);
}
