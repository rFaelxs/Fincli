package com.rfaelxs.web.repository;

import com.rfaelxs.web.domain.Reserva;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

  List<Reserva> findByUsuarioIdOrderByEmergenciaDescNomeAsc(Long usuarioId);

  Optional<Reserva> findByPublicIdAndUsuarioId(UUID publicId, Long usuarioId);

  /**
   * Trava a linha da reserva para alocação e saque. Sem isso, duas requisições simultâneas
   * leriam o mesmo saldo e uma sobrescreveria a outra (escopo-web.md §1.3).
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from Reserva r where r.publicId = :publicId and r.usuario.id = :usuarioId")
  Optional<Reserva> findParaAtualizacao(
      @Param("publicId") UUID publicId, @Param("usuarioId") Long usuarioId);

  Optional<Reserva> findByUsuarioIdAndEmergenciaTrue(Long usuarioId);

  @Query("select coalesce(sum(r.saldoAtual), 0) from Reserva r where r.usuario.id = :usuarioId")
  BigDecimal somarSaldoAlocado(@Param("usuarioId") Long usuarioId);
}
