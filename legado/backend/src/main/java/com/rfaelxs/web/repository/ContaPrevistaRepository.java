package com.rfaelxs.web.repository;

import com.rfaelxs.web.domain.ContaPrevista;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContaPrevistaRepository extends JpaRepository<ContaPrevista, Long> {

  /**
   * Contas que valem no mês informado: as recorrentes mais as avulsas daquele mês.
   *
   * <p>O filtro é feito no banco para não trazer o histórico inteiro de contas avulsas de
   * meses passados.
   */
  @Query("""
      select c from ContaPrevista c
      where c.usuario.id = :usuarioId
        and (c.recorrente = true or c.mesReferencia = :mes)
      order by c.diaVencimento asc, c.nome asc
      """)
  List<ContaPrevista> doMes(@Param("usuarioId") Long usuarioId, @Param("mes") String mes);

  /** Busca por id público <strong>e</strong> dono (IDOR — escopo-web.md §4). */
  Optional<ContaPrevista> findByPublicIdAndUsuarioId(UUID publicId, Long usuarioId);
}
