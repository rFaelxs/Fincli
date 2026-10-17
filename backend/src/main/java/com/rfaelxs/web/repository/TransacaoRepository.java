package com.rfaelxs.web.repository;

import com.rfaelxs.web.domain.TipoTransacao;
import com.rfaelxs.web.domain.Transacao;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acesso às transações. Toda consulta filtra pelo dono. */
public interface TransacaoRepository extends JpaRepository<Transacao, Long> {

  /** Da mais recente para a mais antiga; no mesmo dia, a lançada por último vem antes. */
  List<Transacao> findByUsuarioIdOrderByDataDescIdDesc(Long usuarioId);

  /**
   * Busca pelo id público <strong>e</strong> pelo dono. Consultar sempre pelos dois é o que
   * impede um usuário de alcançar a transação de outro.
   */
  Optional<Transacao> findByPublicIdAndUsuarioId(UUID publicId, Long usuarioId);

  /** Soma de todas as transações do tipo, somada no banco. */
  @Query("""
      select coalesce(sum(t.valor), 0) from Transacao t
      where t.usuario.id = :usuarioId and t.tipo = :tipo
      """)
  BigDecimal somarPorTipo(@Param("usuarioId") Long usuarioId, @Param("tipo") TipoTransacao tipo);
}
