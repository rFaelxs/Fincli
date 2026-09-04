package com.rfaelxs.web.repository;

import com.rfaelxs.web.domain.MovimentacaoReserva;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovimentacaoReservaRepository extends JpaRepository<MovimentacaoReserva, Long> {

  List<MovimentacaoReserva> findByUsuarioIdOrderByDataMovimentoDescIdDesc(Long usuarioId);

  /** @return movimentações do período, da mais recente para a mais antiga */
  List<MovimentacaoReserva> findByUsuarioIdAndDataMovimentoBetweenOrderByDataMovimentoDescIdDesc(
      Long usuarioId, LocalDate inicio, LocalDate fim);

  /** @return somas do período agrupadas por mês e tipo, para derivar o aporte de cada mês */
  @Query("""
      select new com.rfaelxs.web.repository.TotalMovimentacaoMensal(
          year(m.dataMovimento), month(m.dataMovimento), m.tipo, sum(m.valor))
      from MovimentacaoReserva m
      where m.usuario.id = :usuarioId and m.dataMovimento between :inicio and :fim
      group by year(m.dataMovimento), month(m.dataMovimento), m.tipo
      """)
  List<TotalMovimentacaoMensal> somarPorMes(
      @Param("usuarioId") Long usuarioId,
      @Param("inicio") LocalDate inicio,
      @Param("fim") LocalDate fim);

  /** @return somas do período por reserva e tipo, base do aporte médio de cada meta */
  @Query("""
      select new com.rfaelxs.web.repository.TotalPorReserva(m.reserva.id, m.tipo, sum(m.valor))
      from MovimentacaoReserva m
      where m.usuario.id = :usuarioId and m.dataMovimento between :inicio and :fim
      group by m.reserva.id, m.tipo
      """)
  List<TotalPorReserva> somarPorReserva(
      @Param("usuarioId") Long usuarioId,
      @Param("inicio") LocalDate inicio,
      @Param("fim") LocalDate fim);
}
