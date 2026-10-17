package com.rfaelxs.web.repository;

import com.rfaelxs.web.domain.MovimentacaoReserva;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acesso às movimentações de reserva. As consultas entram com o extrato e a Gestão. */
public interface MovimentacaoReservaRepository extends JpaRepository<MovimentacaoReserva, Long> {
}
