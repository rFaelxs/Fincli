package com.rfaelxs.web.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.rfaelxs.web.config.RelogioConfig;
import com.rfaelxs.web.domain.MovimentacaoReserva;
import com.rfaelxs.web.domain.Reserva;
import com.rfaelxs.web.domain.TipoMovimentacaoReserva;
import com.rfaelxs.web.domain.Usuario;
import com.rfaelxs.web.repository.MovimentacaoReservaRepository;
import com.rfaelxs.web.repository.ReservaRepository;
import com.rfaelxs.web.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReservaServiceTest {

  private static final Long DONO = 7L;

  // 22h30 de 16/09 em Brasília, já 17/09 em UTC.
  private static final Clock RELOGIO =
      Clock.fixed(Instant.parse("2026-09-17T01:30:00Z"), RelogioConfig.FUSO);

  @Mock
  private ReservaRepository reservaRepository;

  @Mock
  private MovimentacaoReservaRepository movimentacaoRepository;

  @Mock
  private UsuarioRepository usuarioRepository;

  @Mock
  private SaldoService saldoService;

  private ReservaService service;
  private Usuario usuario;
  private Reserva viagem;

  @BeforeEach
  void setUp() {
    service = new ReservaService(
        reservaRepository, movimentacaoRepository, usuarioRepository, saldoService, RELOGIO);
    usuario = new Usuario("Rafael", "52998224725", "hash");
    viagem = new Reserva(usuario, "Viagem", new BigDecimal("10000.00"), false);
    when(usuarioRepository.travar(DONO)).thenReturn(Optional.of(usuario));
    when(reservaRepository.findByPublicIdAndUsuarioId(viagem.getPublicId(), DONO))
        .thenReturn(Optional.of(viagem));
  }

  @Test
  void criaComNomeAparadoMetaEmDuasCasasESaldoZero() {
    when(usuarioRepository.getReferenceById(DONO)).thenReturn(usuario);
    when(reservaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    Reserva criada = service.criar(DONO, "  Viagem  ", new BigDecimal("8000"));

    assertThat(criada.getNome()).isEqualTo("Viagem");
    assertThat(criada.getMetaValor()).isEqualByComparingTo("8000.00").hasScaleOf(2);
    assertThat(criada.getSaldoAtual()).isEqualByComparingTo("0");
    assertThat(criada.isEmergencia()).isFalse();
  }

  // Item 29 e CA-RES-12
  @Test
  void recusaMetaNegativaNaCriacao() {
    assertThatThrownBy(() -> service.criar(DONO, "Viagem", new BigDecimal("-1")))
        .isInstanceOf(ValorInvalidoException.class)
        .hasMessage("A meta da reserva não pode ser negativa.");
  }

  // CA-RES-19
  @Test
  void alocaDentroDoDisponivelERegistraAMovimentacao() {
    when(saldoService.disponivel(DONO)).thenReturn(new BigDecimal("500.00"));

    service.alocar(DONO, viagem.getPublicId(), new BigDecimal("300"));

    assertThat(viagem.getSaldoAtual()).isEqualByComparingTo("300.00");
    MovimentacaoReserva mov = movimentacaoGravada();
    assertThat(mov.getTipo()).isEqualTo(TipoMovimentacaoReserva.ALOCACAO);
    assertThat(mov.getValor()).isEqualByComparingTo("300.00");
    assertThat(mov.getReservaNome()).isEqualTo("Viagem");
    assertThat(mov.getUsuario()).isSameAs(usuario);
  }

  @Test
  void aMovimentacaoUsaODiaDeBrasiliaENaoODeUtc() {
    when(saldoService.disponivel(DONO)).thenReturn(new BigDecimal("500.00"));

    service.alocar(DONO, viagem.getPublicId(), BigDecimal.TEN);

    assertThat(movimentacaoGravada().getData()).isEqualTo(LocalDate.of(2026, 9, 16));
  }

  // Item 38: o saldo é lido depois do lock do usuário
  @Test
  void travaOUsuarioAntesDeLerOSaldo() {
    when(saldoService.disponivel(DONO)).thenReturn(new BigDecimal("500.00"));

    service.alocar(DONO, viagem.getPublicId(), BigDecimal.TEN);

    InOrder ordem = inOrder(usuarioRepository, saldoService);
    ordem.verify(usuarioRepository).travar(DONO);
    ordem.verify(saldoService).disponivel(DONO);
  }

  // Item 33 e CA-RES-18
  @Test
  void recusaAlocacaoAcimaDoDisponivel() {
    when(saldoService.disponivel(DONO)).thenReturn(new BigDecimal("200.00"));

    assertThatThrownBy(() -> service.alocar(DONO, viagem.getPublicId(), new BigDecimal("300")))
        .isInstanceOf(ValorInvalidoException.class)
        .hasMessage("Saldo insuficiente para esta alocação.");
    assertThat(viagem.getSaldoAtual()).isEqualByComparingTo("0");
    verify(movimentacaoRepository, never()).save(any());
  }

  // Item 32 e CA-RES-21
  @Test
  void recusaAlocacaoZeroSemTravarNada() {
    assertThatThrownBy(() -> service.alocar(DONO, viagem.getPublicId(), BigDecimal.ZERO))
        .isInstanceOf(ValorInvalidoException.class)
        .hasMessage("O valor de alocação deve ser positivo.");
    verify(usuarioRepository, never()).travar(any());
  }

  @Test
  void sacaDentroDoSaldoDaReserva() {
    viagem.alocar(new BigDecimal("100.00"));

    service.sacar(DONO, viagem.getPublicId(), new BigDecimal("40"));

    assertThat(viagem.getSaldoAtual()).isEqualByComparingTo("60.00");
    assertThat(movimentacaoGravada().getTipo()).isEqualTo(TipoMovimentacaoReserva.SAQUE);
    verify(usuarioRepository).travar(DONO);
    verifyNoInteractions(saldoService);
  }

  // Item 34 e CA-RES-20
  @Test
  void recusaSaqueAcimaDoSaldoDaReserva() {
    viagem.alocar(new BigDecimal("100.00"));

    assertThatThrownBy(() -> service.sacar(DONO, viagem.getPublicId(), new BigDecimal("150")))
        .isInstanceOf(ValorInvalidoException.class)
        .hasMessage("Saldo da reserva insuficiente para este saque.");
    assertThat(viagem.getSaldoAtual()).isEqualByComparingTo("100.00");
  }

  // Item 32
  @Test
  void recusaSaqueNegativo() {
    assertThatThrownBy(() -> service.sacar(DONO, viagem.getPublicId(), new BigDecimal("-5")))
        .isInstanceOf(ValorInvalidoException.class)
        .hasMessage("O valor do saque deve ser positivo.");
  }

  @Test
  void alteraAMeta() {
    service.alterarMeta(DONO, viagem.getPublicId(), new BigDecimal("12000"));

    assertThat(viagem.getMetaValor()).isEqualByComparingTo("12000.00");
  }

  // Item 29
  @Test
  void recusaMetaNegativaNaEdicao() {
    assertThatThrownBy(() -> service.alterarMeta(DONO, viagem.getPublicId(), new BigDecimal("-1")))
        .isInstanceOf(ValorInvalidoException.class)
        .hasMessage("A meta não pode ser negativa.");
  }

  // Item 35 e CA-RES-10
  @Test
  void naoExcluiAReservaDeEmergencia() {
    Reserva emergencia = Reserva.emergenciaDe(usuario);
    when(reservaRepository.findByPublicIdAndUsuarioId(emergencia.getPublicId(), DONO))
        .thenReturn(Optional.of(emergencia));

    assertThatThrownBy(() -> service.excluir(DONO, emergencia.getPublicId()))
        .isInstanceOf(ConflitoException.class)
        .hasMessage("A Reserva de Emergência não pode ser excluída.");
    verify(reservaRepository, never()).delete(any());
  }

  // Item 36 e CA-RES-09
  @Test
  void naoExcluiReservaComSaldo() {
    viagem.alocar(new BigDecimal("300.00"));

    assertThatThrownBy(() -> service.excluir(DONO, viagem.getPublicId()))
        .isInstanceOf(ConflitoException.class)
        .hasMessage("A reserva ainda tem saldo. Saque o valor antes de excluí-la.");
  }

  // CA-RES-08
  @Test
  void excluiReservaSemSaldoComOUsuarioTravado() {
    service.excluir(DONO, viagem.getPublicId());

    InOrder ordem = inOrder(usuarioRepository, reservaRepository);
    ordem.verify(usuarioRepository).travar(DONO);
    ordem.verify(reservaRepository).delete(viagem);
  }

  // Item 37 e CA-RES-23
  @Test
  void reservaDeOutroUsuarioNaoEhEncontrada() {
    UUID alheia = UUID.randomUUID();
    when(reservaRepository.findByPublicIdAndUsuarioId(alheia, DONO)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.alocar(DONO, alheia, BigDecimal.TEN))
        .isInstanceOf(RecursoNaoEncontradoException.class)
        .hasMessage("Reserva não encontrada.");
  }

  private MovimentacaoReserva movimentacaoGravada() {
    ArgumentCaptor<MovimentacaoReserva> mov = ArgumentCaptor.forClass(MovimentacaoReserva.class);
    verify(movimentacaoRepository).save(mov.capture());
    return mov.getValue();
  }
}
