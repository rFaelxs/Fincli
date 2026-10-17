package com.rfaelxs.web.repository;

import com.rfaelxs.web.domain.Reserva;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acesso às reservas. Toda consulta filtra pelo dono. */
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

  /** A Reserva de Emergência primeiro; as demais, por nome. */
  List<Reserva> findByUsuarioIdOrderByEmergenciaDescNomeAsc(Long usuarioId);

  /**
   * Busca pelo id público <strong>e</strong> pelo dono. Consultar sempre pelos dois é o que
   * impede um usuário de alcançar a reserva de outro.
   */
  Optional<Reserva> findByPublicIdAndUsuarioId(UUID publicId, Long usuarioId);

  /** Total guardado em todas as reservas do usuário. */
  @Query("select coalesce(sum(r.saldoAtual), 0) from Reserva r where r.usuario.id = :usuarioId")
  BigDecimal somarSaldo(@Param("usuarioId") Long usuarioId);
}
