package com.rfaelxs.web.service;

import com.rfaelxs.web.repository.MovimentacaoReservaRepository;
import com.rfaelxs.web.repository.TransacaoRepository;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Extrato: transações e movimentações de reserva numa lista só (spec 08). */
@Service
public class ExtratoService {

  private final TransacaoRepository transacaoRepository;
  private final MovimentacaoReservaRepository movimentacaoRepository;

  public ExtratoService(
      TransacaoRepository transacaoRepository,
      MovimentacaoReservaRepository movimentacaoRepository) {
    this.transacaoRepository = transacaoRepository;
    this.movimentacaoRepository = movimentacaoRepository;
  }

  /** Histórico inteiro do usuário, na {@link ItemExtrato#ORDEM}. Sem filtro nem paginação. */
  @Transactional(readOnly = true)
  public List<ItemExtrato> listar(Long usuarioId) {
    Stream<ItemExtrato> transacoes = transacaoRepository
        .findByUsuarioIdOrderByDataDescIdDesc(usuarioId).stream().map(ItemExtrato::de);
    Stream<ItemExtrato> movimentacoes = movimentacaoRepository
        .findByUsuarioIdOrderByDataDescIdDesc(usuarioId).stream().map(ItemExtrato::de);
    return Stream.concat(transacoes, movimentacoes).sorted(ItemExtrato.ORDEM).toList();
  }
}
