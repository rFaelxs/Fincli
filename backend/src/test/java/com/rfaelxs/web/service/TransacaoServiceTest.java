package com.rfaelxs.web.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rfaelxs.web.domain.TipoTransacao;
import com.rfaelxs.web.domain.Transacao;
import com.rfaelxs.web.domain.Usuario;
import com.rfaelxs.web.repository.TransacaoRepository;
import com.rfaelxs.web.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransacaoServiceTest {

  private static final Long DONO = 7L;
  private static final LocalDate DIA = LocalDate.of(2026, 9, 16);

  @Mock
  private TransacaoRepository transacaoRepository;

  @Mock
  private UsuarioRepository usuarioRepository;

  private TransacaoService service;
  private Usuario usuario;

  @BeforeEach
  void setUp() {
    service = new TransacaoService(transacaoRepository, usuarioRepository);
    usuario = new Usuario("Rafael", "52998224725", "hash");
  }

  @Test
  void criaComValorEmDuasCasasCategoriaAparadaEDescricaoEmBrancoNula() {
    when(usuarioRepository.getReferenceById(DONO)).thenReturn(usuario);
    when(transacaoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    Transacao criada = service.criar(DONO, dados(new BigDecimal("10.005"), "  Mercado  ", "   "));

    assertThat(criada.getUsuario()).isSameAs(usuario);
    assertThat(criada.getValor()).isEqualByComparingTo("10.01").hasScaleOf(2);
    assertThat(criada.getCategoria()).isEqualTo("Mercado");
    assertThat(criada.getDescricao()).isNull();
    assertThat(criada.getData()).isEqualTo(DIA);
    assertThat(criada.getTipo()).isEqualTo(TipoTransacao.SAIDA);
    assertThat(criada.isEssencial()).isTrue();
  }

  @Test
  void criaComDescricaoAparada() {
    when(usuarioRepository.getReferenceById(DONO)).thenReturn(usuario);
    when(transacaoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    Transacao criada = service.criar(DONO, dados(BigDecimal.TEN, "Alimentação", " Feira "));

    assertThat(criada.getDescricao()).isEqualTo("Feira");
  }

  @Test
  void aceitaValorZero() {
    when(usuarioRepository.getReferenceById(DONO)).thenReturn(usuario);
    when(transacaoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    assertThat(service.criar(DONO, dados(BigDecimal.ZERO, "Teste", null)).getValor())
        .isEqualByComparingTo("0.00");
  }

  // RN-TRX-01
  @Test
  void recusaValorNegativoSemGravar() {
    assertThatThrownBy(() -> service.criar(DONO, dados(new BigDecimal("-10"), "Mercado", null)))
        .isInstanceOf(ValorInvalidoException.class)
        .hasMessage("O valor da transação não pode ser negativo.");
    verify(transacaoRepository, never()).save(any());
  }

  // RN-TRX-02
  @Test
  void transacaoDeOutroUsuarioNaoEhEncontrada() {
    UUID id = UUID.randomUUID();
    when(transacaoRepository.findByPublicIdAndUsuarioId(id, DONO)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.buscar(DONO, id))
        .isInstanceOf(RecursoNaoEncontradoException.class)
        .hasMessage("Transação não encontrada.");
  }

  @Test
  void editaTodosOsCampos() {
    Transacao existente = new Transacao(usuario, new BigDecimal("120.00"), "Alimentação",
        "Mercado", DIA, TipoTransacao.SAIDA, false);
    when(transacaoRepository.findByPublicIdAndUsuarioId(existente.getPublicId(), DONO))
        .thenReturn(Optional.of(existente));

    service.editar(DONO, existente.getPublicId(), new DadosTransacao(new BigDecimal("150"),
        "Salário", null, DIA.plusDays(1), TipoTransacao.ENTRADA, false));

    assertThat(existente.getValor()).isEqualByComparingTo("150.00");
    assertThat(existente.getCategoria()).isEqualTo("Salário");
    assertThat(existente.getDescricao()).isNull();
    assertThat(existente.getData()).isEqualTo(DIA.plusDays(1));
    assertThat(existente.getTipo()).isEqualTo(TipoTransacao.ENTRADA);
  }

  @Test
  void edicaoComValorNegativoNaoAlteraNada() {
    UUID id = UUID.randomUUID();

    assertThatThrownBy(() -> service.editar(DONO, id, dados(new BigDecimal("-1"), "X", null)))
        .isInstanceOf(ValorInvalidoException.class);
    verify(transacaoRepository, never()).findByPublicIdAndUsuarioId(any(), any());
  }

  @Test
  void excluirTransacaoDeOutroUsuarioNaoApagaNada() {
    UUID id = UUID.randomUUID();
    when(transacaoRepository.findByPublicIdAndUsuarioId(id, DONO)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.excluir(DONO, id))
        .isInstanceOf(RecursoNaoEncontradoException.class);
    verify(transacaoRepository, never()).delete(any());
  }

  private static DadosTransacao dados(BigDecimal valor, String categoria, String descricao) {
    return new DadosTransacao(valor, categoria, descricao, DIA, TipoTransacao.SAIDA, true);
  }
}
