package com.rfaelxs.web.service;

import com.rfaelxs.web.domain.TipoTransacao;
import com.rfaelxs.web.repository.ReservaRepository;
import com.rfaelxs.web.repository.TransacaoRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Saldo disponível do usuário (RN-09). */
@Service
public class SaldoService {

  private final TransacaoRepository transacaoRepository;
  private final ReservaRepository reservaRepository;

  public SaldoService(
      TransacaoRepository transacaoRepository, ReservaRepository reservaRepository) {
    this.transacaoRepository = transacaoRepository;
    this.reservaRepository = reservaRepository;
  }

  /**
   * Entradas menos saídas menos o total guardado em reservas, somado no banco (ADR 0008).
   *
   * <p>Pode ser negativo: uma saída lançada depois de uma alocação não é barrada. Chamado de
   * dentro da alocação, participa da transação dela e lê o saldo depois do lock.
   */
  @Transactional(readOnly = true)
  public BigDecimal disponivel(Long usuarioId) {
    BigDecimal entradas = transacaoRepository.somarPorTipo(usuarioId, TipoTransacao.ENTRADA);
    BigDecimal saidas = transacaoRepository.somarPorTipo(usuarioId, TipoTransacao.SAIDA);
    BigDecimal guardado = reservaRepository.somarSaldo(usuarioId);
    return Dinheiro.normalizar(entradas.subtract(saidas).subtract(guardado));
  }
}
