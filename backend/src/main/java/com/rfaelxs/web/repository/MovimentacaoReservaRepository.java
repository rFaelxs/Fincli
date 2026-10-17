package com.rfaelxs.web.repository;

import com.rfaelxs.web.domain.MovimentacaoReserva;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acesso às movimentações de reserva. Toda consulta filtra pelo dono. */
public interface MovimentacaoReservaRepository extends JpaRepository<MovimentacaoReserva, Long> {

  /** Da mais recente para a mais antiga; no mesmo dia, a registrada por último vem antes. */
  List<MovimentacaoReserva> findByUsuarioIdOrderByDataDescIdDesc(Long usuarioId);

  /** As 6 mais recentes do período, na mesma ordem. */
  List<MovimentacaoReserva> findTop6ByUsuarioIdAndDataBetweenOrderByDataDescIdDesc(
      Long usuarioId, LocalDate inicio, LocalDate fim);

  /** Somas do período por mês e tipo, de onde sai o aporte de cada mês. */
  @Query("""
      select new com.rfaelxs.web.repository.TotalMovimentacaoMensal(
          year(m.data), month(m.data), m.tipo, sum(m.valor))
      from MovimentacaoReserva m
      where m.usuario.id = :usuarioId and m.data between :inicio and :fim
      group by year(m.data), month(m.data), m.tipo
      """)
  List<TotalMovimentacaoMensal> somarPorMes(@Param("usuarioId") Long usuarioId,
      @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

  /**
   * Somas do período por reserva e tipo, base do aporte médio. Movimentos de reserva já
   * excluída ficam de fora: o join com a reserva descarta {@code reserva_id} nulo.
   */
  @Query("""
      select new com.rfaelxs.web.repository.TotalPorReserva(r.id, m.tipo, sum(m.valor))
      from MovimentacaoReserva m join m.reserva r
      where m.usuario.id = :usuarioId and m.data between :inicio and :fim
      group by r.id, m.tipo
      """)
  List<TotalPorReserva> somarPorReserva(@Param("usuarioId") Long usuarioId,
      @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);
}
