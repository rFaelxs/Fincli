package com.rfaelxs.web.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rfaelxs.web.domain.ContaPrevista;
import com.rfaelxs.web.domain.Usuario;
import com.rfaelxs.web.repository.ContaPrevistaRepository;
import com.rfaelxs.web.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContaPrevistaServiceTest {

  private static final Long DONO = 7L;
  private static final YearMonth SETEMBRO = YearMonth.of(2026, 9);

  @Mock
  private ContaPrevistaRepository contaRepository;

  @Mock
  private UsuarioRepository usuarioRepository;

  private ContaPrevistaService service;
  private final Usuario usuario = new Usuario("Rafael", "52998224725", "hash");

  @BeforeEach
  void setUp() {
    service = new ContaPrevistaService(contaRepository, usuarioRepository);
  }

  // Item 25: a conta que vence hoje ainda conta
  @Test
  void aindaVencemIncluiAContaDeHojeEExcluiAsJaVencidas() {
    when(contaRepository.doMes(DONO, "2026-09")).thenReturn(List.of(
        conta("Aluguel", 5), conta("Energia", 18), conta("Internet", 22)));

    List<ContaPrevista> pendentes = service.aindaVencem(DONO, SETEMBRO, LocalDate.of(2026, 9, 18));

    assertThat(pendentes).extracting(ContaPrevista::getNome).containsExactly("Energia", "Internet");
  }

  @Test
  void somaEmDuasCasas() {
    assertThat(ContaPrevistaService.somar(List.of(conta("A", 1), conta("B", 2))))
        .isEqualByComparingTo("420.00").hasScaleOf(2);
    assertThat(ContaPrevistaService.somar(List.of())).isEqualByComparingTo("0.00");
  }

  @Test
  void criaRecorrenteComNomeAparadoEValorEmDuasCasas() {
    when(usuarioRepository.getReferenceById(DONO)).thenReturn(usuario);
    when(contaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    ContaPrevista criada = service.criar(DONO, " Energia ", new BigDecimal("210"), 22, null);

    assertThat(criada.getNome()).isEqualTo("Energia");
    assertThat(criada.getValor()).isEqualByComparingTo("210.00").hasScaleOf(2);
    assertThat(criada.isRecorrente()).isTrue();
  }

  @Test
  void criaAvulsaDoMesInformado() {
    when(usuarioRepository.getReferenceById(DONO)).thenReturn(usuario);
    when(contaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    ContaPrevista criada = service.criar(DONO, "IPVA", BigDecimal.TEN, 10, SETEMBRO);

    assertThat(criada.getMesReferencia()).isEqualTo(SETEMBRO);
  }

  @Test
  void recusaValorZero() {
    assertThatThrownBy(() -> service.criar(DONO, "Energia", BigDecimal.ZERO, 22, null))
        .isInstanceOf(ValorInvalidoException.class)
        .hasMessage("O valor da conta prevista deve ser positivo.");
  }

  @Test
  void recusaDiaForaDoMes() {
    assertThatThrownBy(() -> service.criar(DONO, "Energia", BigDecimal.TEN, 32, null))
        .isInstanceOf(ValorInvalidoException.class)
        .hasMessage("O dia de vencimento deve estar entre 1 e 31.");
    verify(contaRepository, never()).save(any());
  }

  @Test
  void removerContaAlheiaResponde404() {
    UUID alheia = UUID.randomUUID();
    when(contaRepository.findByPublicIdAndUsuarioId(alheia, DONO)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.remover(DONO, alheia))
        .isInstanceOf(RecursoNaoEncontradoException.class)
        .hasMessage("Conta prevista não encontrada.");
    verify(contaRepository, never()).delete(any());
  }

  private ContaPrevista conta(String nome, int dia) {
    return new ContaPrevista(usuario, nome, new BigDecimal("210.00"), dia, null);
  }
}
