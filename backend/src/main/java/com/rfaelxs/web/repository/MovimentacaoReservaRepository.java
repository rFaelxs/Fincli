package com.rfaelxs.web.repository;

import com.rfaelxs.web.domain.MovimentacaoReserva;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimentacaoReservaRepository extends JpaRepository<MovimentacaoReserva, Long> {

  List<MovimentacaoReserva> findByUsuarioIdOrderByDataMovimentoDescIdDesc(Long usuarioId);
}
