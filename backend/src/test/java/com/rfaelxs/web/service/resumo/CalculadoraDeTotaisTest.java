package com.rfaelxs.web.service.resumo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.rfaelxs.web.domain.TipoMovimentacaoReserva;
import com.rfaelxs.web.domain.TipoTransacao;
import com.rfaelxs.web.repository.MovimentacaoReservaRepository;
import com.rfaelxs.web.repository.TotalMensal;
import com.rfaelxs.web.repository.TotalMovimentacaoMensal;
import com.rfaelxs.web.repository.TransacaoRepository;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CalculadoraDeTotaisTest {

  private static final Long DONO = 7L;
  private static final YearMonth AGOSTO = YearMonth.of(2026, 8);
  private static final YearMonth SETEMBRO = YearMonth.of(2026, 9);

  @Mock
  private TransacaoRepository transacaoRepository;

  @Mock
  private MovimentacaoReservaRepository movimentacaoRepository;

  @InjectMocks
  private CalculadoraDeTotais calculadora;

  // RN-01 a RN-04: essenciais, supérfluas, aportes e sobra
  @Test
  void aplicaAsFormulasDoMes() {
    when(transacaoRepository.somarPorMes(DONO, SETEMBRO.atDay(1), SETEMBRO.atEndOfMonth()))
        .thenReturn(List.of(
            transacao(SETEMBRO, TipoTransacao.ENTRADA, false, "5000.00"),
            transacao(SETEMBRO, TipoTransacao.SAIDA, true, "1000.00"),
            transacao(SETEMBRO, TipoTransacao.SAIDA, false, "1500.00")));
    when(movimentacaoRepository.somarPorMes(DONO, SETEMBRO.atDay(1), SETEMBRO.atEndOfMonth()))
        .thenReturn(List.of(
            movimentacao(SETEMBRO, TipoMovimentacaoReserva.ALOCACAO, "500.00"),
            movimentacao(SETEMBRO, TipoMovimentacaoReserva.SAQUE, "200.00")));

    Totais totais = calculadora.doMes(DONO, SETEMBRO);

    assertThat(totais.entradas()).isEqualByComparingTo("5000.00");
    assertThat(totais.saidas()).isEqualByComparingTo("2500.00");
    assertThat(totais.essenciais()).isEqualByComparingTo("1000.00");
    assertThat(totais.superfluas()).isEqualByComparingTo("1500.00");
    assertThat(totais.aportes()).isEqualByComparingTo("300.00");
    assertThat(totais.sobra()).isEqualByComparingTo("2200.00");
  }

  // CA-GESTAO-07: sacar mais do que guardou deixa o aporte negativo
  @Test
  void aporteFicaNegativoQuandoOSaquePassaDaAlocacao() {
    when(transacaoRepository.somarPorMes(DONO, SETEMBRO.atDay(1), SETEMBRO.atEndOfMonth()))
        .thenReturn(List.of());
    when(movimentacaoRepository.somarPorMes(DONO, SETEMBRO.atDay(1), SETEMBRO.atEndOfMonth()))
        .thenReturn(List.of(movimentacao(SETEMBRO, TipoMovimentacaoReserva.SAQUE, "200.00")));

    Totais totais = calculadora.doMes(DONO, SETEMBRO);

    assertThat(totais.aportes()).isEqualByComparingTo("-200.00");
    assertThat(totais.sobra()).isEqualByComparingTo("200.00");
  }

  @Test
  void mesSemMovimentoVemZerado() {
    when(transacaoRepository.somarPorMes(DONO, SETEMBRO.atDay(1), SETEMBRO.atEndOfMonth()))
        .thenReturn(List.of());
    when(movimentacaoRepository.somarPorMes(DONO, SETEMBRO.atDay(1), SETEMBRO.atEndOfMonth()))
        .thenReturn(List.of());

    assertThat(calculadora.doMes(DONO, SETEMBRO)).isEqualTo(Totais.zero());
  }

  @Test
  void separaOsMesesDoIntervalo() {
    when(transacaoRepository.somarPorMes(DONO, AGOSTO.atDay(1), SETEMBRO.atEndOfMonth()))
        .thenReturn(List.of(
            transacao(AGOSTO, TipoTransacao.ENTRADA, false, "100.00"),
            transacao(SETEMBRO, TipoTransacao.ENTRADA, false, "300.00")));
    when(movimentacaoRepository.somarPorMes(DONO, AGOSTO.atDay(1), SETEMBRO.atEndOfMonth()))
        .thenReturn(List.of());

    Map<YearMonth, Totais> porMes = calculadora.porMes(DONO, AGOSTO, SETEMBRO);

    assertThat(porMes.get(AGOSTO).sobra()).isEqualByComparingTo("100.00");
    assertThat(porMes.get(SETEMBRO).sobra()).isEqualByComparingTo("300.00");
  }

  private static TotalMensal transacao(YearMonth mes, TipoTransacao tipo, boolean essencial,
      String total) {
    return new TotalMensal(mes.getYear(), mes.getMonthValue(), tipo, essencial,
        new BigDecimal(total));
  }

  private static TotalMovimentacaoMensal movimentacao(YearMonth mes, TipoMovimentacaoReserva tipo,
      String total) {
    return new TotalMovimentacaoMensal(mes.getYear(), mes.getMonthValue(), tipo,
        new BigDecimal(total));
  }
}
