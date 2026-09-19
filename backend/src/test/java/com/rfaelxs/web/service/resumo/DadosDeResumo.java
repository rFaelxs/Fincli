package com.rfaelxs.web.service.resumo;

import com.rfaelxs.web.domain.ContaPrevista;
import com.rfaelxs.web.domain.Reserva;
import com.rfaelxs.web.domain.TipoMovimentacaoReserva;
import com.rfaelxs.web.domain.TipoTransacao;
import com.rfaelxs.web.domain.Usuario;
import com.rfaelxs.web.repository.TotalMensal;
import com.rfaelxs.web.repository.TotalMovimentacaoMensal;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;

/**
 * Dados de apoio dos testes de Gestão e Relatório.
 *
 * <p>O relógio é fixo em 16/09/2026 — o mesmo dia do protótipo de design — para que os dias
 * restantes e o valor por dia sejam verificáveis.
 */
final class DadosDeResumo {

  static final LocalDate HOJE = LocalDate.of(2026, 9, 16);
  static final YearMonth SETEMBRO = YearMonth.of(2026, 9);

  private DadosDeResumo() {
  }

  static Clock relogio() {
    return Clock.fixed(HOJE.atStartOfDay(ZoneId.systemDefault()).toInstant(),
        ZoneId.systemDefault());
  }

  static TotalMensal entrada(YearMonth mes, String valor) {
    return new TotalMensal(
        mes.getYear(), mes.getMonthValue(), TipoTransacao.ENTRADA, false, new BigDecimal(valor));
  }

  static TotalMensal saida(YearMonth mes, String valor, boolean essencial) {
    return new TotalMensal(
        mes.getYear(), mes.getMonthValue(), TipoTransacao.SAIDA, essencial,
        new BigDecimal(valor));
  }

  static TotalMovimentacaoMensal alocacao(YearMonth mes, String valor) {
    return new TotalMovimentacaoMensal(
        mes.getYear(), mes.getMonthValue(), TipoMovimentacaoReserva.ALOCACAO,
        new BigDecimal(valor));
  }

  static TotalMovimentacaoMensal saque(YearMonth mes, String valor) {
    return new TotalMovimentacaoMensal(
        mes.getYear(), mes.getMonthValue(), TipoMovimentacaoReserva.SAQUE, new BigDecimal(valor));
  }

  static ContaPrevista conta(Usuario usuario, String nome, String valor, int dia) {
    return new ContaPrevista(usuario, nome, new BigDecimal(valor), dia, null);
  }

  static Reserva reserva(Usuario usuario, String nome, String meta, String saldo) {
    Reserva r = new Reserva(usuario, nome, new BigDecimal(meta), false);
    r.setSaldoAtual(new BigDecimal(saldo));
    return r;
  }
}
