package com.rfaelxs.web.repository;

import com.rfaelxs.web.domain.ContaPrevista;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acesso às contas previstas. Toda consulta filtra pelo dono. */
public interface ContaPrevistaRepository extends JpaRepository<ContaPrevista, Long> {

  /**
   * Contas que valem no mês: as recorrentes mais as avulsas daquele mês, da que vence primeiro
   * para a última. O filtro roda no banco para não trazer as avulsas de meses passados.
   *
   * @param mes mês em {@code yyyy-MM}
   */
  @Query("""
      select c from ContaPrevista c
      where c.usuario.id = :usuarioId
        and (c.recorrente = true or c.mesReferencia = :mes)
      order by c.diaVencimento asc, c.nome asc
      """)
  List<ContaPrevista> doMes(@Param("usuarioId") Long usuarioId, @Param("mes") String mes);

  /** Busca pelo id público <strong>e</strong> pelo dono. */
  Optional<ContaPrevista> findByPublicIdAndUsuarioId(UUID publicId, Long usuarioId);
}
