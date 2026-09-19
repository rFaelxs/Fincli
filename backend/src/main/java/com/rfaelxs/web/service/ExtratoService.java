package com.rfaelxs.web.service;

import com.rfaelxs.web.domain.Usuario;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Extrato cronológico: transações e movimentações de reserva na mesma lista. */
@Service
public class ExtratoService {

  private final TransacaoService transacaoService;
  private final ReservaService reservaService;

  public ExtratoService(TransacaoService transacaoService, ReservaService reservaService) {
    this.transacaoService = transacaoService;
    this.reservaService = reservaService;
  }

  /**
   * Junta as duas origens do mais recente para o mais antigo.
   *
   * <p>A ordenação é estável: no mesmo dia, as transações vêm antes das movimentações, cada
   * grupo na ordem em que o repositório o devolveu.
   *
   * @param usuario dono dos lançamentos
   * @return itens do mais recente para o mais antigo
   */
  @Transactional(readOnly = true)
  public List<ItemExtrato> listar(Usuario usuario) {
    return Stream.concat(
            transacaoService.listar(usuario).stream().map(ItemExtrato::de),
            reservaService.listarMovimentacoes(usuario).stream().map(ItemExtrato::de))
        .sorted(Comparator.comparing(ItemExtrato::data).reversed())
        .toList();
  }
}
