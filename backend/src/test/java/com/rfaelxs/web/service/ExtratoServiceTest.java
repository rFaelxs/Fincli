package com.rfaelxs.web.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.rfaelxs.web.domain.MovimentacaoReserva;
import com.rfaelxs.web.domain.Reserva;
import com.rfaelxs.web.domain.TipoMovimentacaoReserva;
import com.rfaelxs.web.domain.TipoTransacao;
import com.rfaelxs.web.domain.Transacao;
import com.rfaelxs.web.domain.Usuario;
import com.rfaelxs.web.repository.MovimentacaoReservaRepository;
import com.rfaelxs.web.repository.TransacaoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ExtratoServiceTest {

  private static final Long DONO = 7L;
  private static final LocalDate DIA_16 = LocalDate.of(2026, 9, 16);
  private static final LocalDate DIA_18 = LocalDate.of(2026, 9, 18);

  @Mock
  private TransacaoRepository transacaoRepository;

  @Mock
  private MovimentacaoReservaRepository movimentacaoRepository;

  @InjectMocks
  private ExtratoService service;

  private final Usuario usuario = new Usuario("Rafael", "52998224725", "hash");
  private final Reserva viagem = new Reserva(usuario, "Viagem", BigDecimal.TEN, false);

  // CA-EXT-01
  @Test
  void dataMaisRecentePrimeiro() {
    dados(List.of(transacao(1L, DIA_16)), List.of(movimentacao(1L, DIA_18, "ALOCACAO")));

    assertThat(service.listar(DONO))
        .extracting(ItemExtrato::origem)
        .containsExactly(ItemExtrato.Origem.RESERVA, ItemExtrato.Origem.TRANSACAO);
  }

  // Item 1 e CA-EXT-13: no mesmo dia, a transação vem antes, mesmo lançada antes
  @Test
  void noMesmoDiaTransacaoAntesDaMovimentacao() {
    dados(List.of(transacao(1L, DIA_16)), List.of(movimentacao(99L, DIA_16, "ALOCACAO")));

    assertThat(service.listar(DONO))
        .extracting(ItemExtrato::origem)
        .containsExactly(ItemExtrato.Origem.TRANSACAO, ItemExtrato.Origem.RESERVA);
  }

  @Test
  void dentroDaMesmaOrigemALancadaPorUltimoVemAntes() {
    dados(List.of(transacao(1L, DIA_16), transacao(2L, DIA_16)), List.of());

    assertThat(service.listar(DONO)).extracting(ItemExtrato::id).containsExactly(2L, 1L);
  }

  // CA-EXT-12: aporte desfeito vira dois itens, o saque primeiro
  @Test
  void aporteDesfeitoApareceComoAlocacaoESaque() {
    dados(List.of(), List.of(movimentacao(1L, DIA_16, "ALOCACAO"),
        movimentacao(2L, DIA_16, "SAQUE")));

    assertThat(service.listar(DONO)).extracting(ItemExtrato::tipo)
        .containsExactly("SAQUE", "ALOCACAO");
  }

  // CA-EXT-11
  @Test
  void movimentacaoMostraONomeGravadoNoMovimento() {
    MovimentacaoReserva mov = movimentacao(1L, DIA_16, "ALOCACAO");
    ReflectionTestUtils.setField(viagem, "nome", "Viagem 2027");
    dados(List.of(), List.of(mov));

    ItemExtrato item = service.listar(DONO).get(0);

    assertThat(item.descricao()).isEqualTo("Viagem");
    assertThat(item.categoria()).isEqualTo("Reserva");
    assertThat(item.tipo()).isEqualTo("ALOCACAO");
    assertThat(item.valor()).isEqualByComparingTo("300.00");
  }

  @Test
  void transacaoLevaTipoCategoriaEDescricao() {
    dados(List.of(transacao(1L, DIA_16)), List.of());

    ItemExtrato item = service.listar(DONO).get(0);

    assertThat(item.tipo()).isEqualTo("ENTRADA");
    assertThat(item.categoria()).isEqualTo("Trabalho");
    assertThat(item.descricao()).isEqualTo("Freela");
    assertThat(item.data()).isEqualTo(DIA_16);
  }

  // CA-EXT-08
  @Test
  void semLancamentosAListaVemVazia() {
    dados(List.of(), List.of());

    assertThat(service.listar(DONO)).isEmpty();
  }

  /** CA-EXT-10: as duas consultas filtram pelo dono da sessão. */
  private void dados(List<Transacao> transacoes, List<MovimentacaoReserva> movimentacoes) {
    when(transacaoRepository.findByUsuarioIdOrderByDataDescIdDesc(DONO)).thenReturn(transacoes);
    when(movimentacaoRepository.findByUsuarioIdOrderByDataDescIdDesc(DONO))
        .thenReturn(movimentacoes);
  }

  private Transacao transacao(Long id, LocalDate data) {
    Transacao t = new Transacao(usuario, new BigDecimal("450.00"), "Trabalho", "Freela", data,
        TipoTransacao.ENTRADA, false);
    ReflectionTestUtils.setField(t, "id", id);
    return t;
  }

  private MovimentacaoReserva movimentacao(Long id, LocalDate data, String tipo) {
    MovimentacaoReserva m = new MovimentacaoReserva(usuario, viagem, new BigDecimal("300.00"),
        TipoMovimentacaoReserva.valueOf(tipo), data);
    ReflectionTestUtils.setField(m, "id", id);
    return m;
  }
}
