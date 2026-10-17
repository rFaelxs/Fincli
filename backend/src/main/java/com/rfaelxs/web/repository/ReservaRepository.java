package com.rfaelxs.web.repository;

import com.rfaelxs.web.domain.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acesso às reservas. As consultas por usuário entram com o módulo de reservas. */
public interface ReservaRepository extends JpaRepository<Reserva, Long> {
}
