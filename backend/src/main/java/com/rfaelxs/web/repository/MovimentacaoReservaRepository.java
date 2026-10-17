package com.rfaelxs.web.repository;

import com.rfaelxs.web.domain.MovimentacaoReserva;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acesso às movimentações de reserva. Toda consulta filtra pelo dono. */
public interface MovimentacaoReservaRepository extends JpaRepository<MovimentacaoReserva, Long> {

  /** Da mais recente para a mais antiga; no mesmo dia, a registrada por último vem antes. */
  List<MovimentacaoReserva> findByUsuarioIdOrderByDataDescIdDesc(Long usuarioId);
}
