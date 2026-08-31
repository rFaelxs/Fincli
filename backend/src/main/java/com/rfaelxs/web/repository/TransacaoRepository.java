package com.rfaelxs.web.repository;

import com.rfaelxs.web.domain.TipoTransacao;
import com.rfaelxs.web.domain.Transacao;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransacaoRepository extends JpaRepository<Transacao, Long> {

  List<Transacao> findByUsuarioIdOrderByDataTransacaoDescIdDesc(Long usuarioId);

  /**
   * Busca por id público <strong>e</strong> dono. Consultar sempre pelos dois é o que impede
   * que um usuário alcance o recurso de outro (escopo-web.md §4).
   */
  Optional<Transacao> findByPublicIdAndUsuarioId(UUID publicId, Long usuarioId);

  @Query("""
      select coalesce(sum(t.valor), 0) from Transacao t
      where t.usuario.id = :usuarioId and t.tipo = :tipo
      """)
  BigDecimal somarPorTipo(@Param("usuarioId") Long usuarioId, @Param("tipo") TipoTransacao tipo);

  @Query("""
      select coalesce(sum(t.valor), 0) from Transacao t
      where t.usuario.id = :usuarioId and t.tipo = :tipo
        and t.dataTransacao between :inicio and :fim
      """)
  BigDecimal somarPorTipoNoPeriodo(
      @Param("usuarioId") Long usuarioId,
      @Param("tipo") TipoTransacao tipo,
      @Param("inicio") LocalDate inicio,
      @Param("fim") LocalDate fim);
}
