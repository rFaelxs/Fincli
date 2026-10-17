package com.rfaelxs.web.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rfaelxs.web.domain.Reserva;
import com.rfaelxs.web.domain.Usuario;
import com.rfaelxs.web.repository.ReservaRepository;
import com.rfaelxs.web.repository.UsuarioRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

  @Mock
  private UsuarioRepository usuarioRepository;

  @Mock
  private ReservaRepository reservaRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  private UsuarioService service;

  @BeforeEach
  void setUp() {
    service = new UsuarioService(usuarioRepository, reservaRepository, passwordEncoder);
  }

  @Test
  void cadastraComCpfSoDeDigitosNomeAparadoESenhaComHash() {
    when(passwordEncoder.encode("segredo123")).thenReturn("hash");

    Usuario usuario = service.cadastrar("  Rafael  ", "529.982.247-25", "segredo123", null);

    assertThat(usuario.getNome()).isEqualTo("Rafael");
    assertThat(usuario.getCpf()).isEqualTo("52998224725");
    assertThat(usuario.getSenhaHash()).isEqualTo("hash");
    assertThat(usuario.getEmail()).isNull();
    verify(usuarioRepository).saveAndFlush(usuario);
  }

  @Test
  void criaAReservaDeEmergenciaComMetaZerada() {
    when(passwordEncoder.encode(any())).thenReturn("hash");

    Usuario usuario = service.cadastrar("Rafael", "52998224725", "segredo123", null);

    ArgumentCaptor<Reserva> reserva = ArgumentCaptor.forClass(Reserva.class);
    verify(reservaRepository).save(reserva.capture());
    assertThat(reserva.getValue().getUsuario()).isSameAs(usuario);
    assertThat(reserva.getValue().getNome()).isEqualTo("Reserva de Emergência");
    assertThat(reserva.getValue().isEmergencia()).isTrue();
    assertThat(reserva.getValue().getMetaValor()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(reserva.getValue().getSaldoAtual()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  void guardaOEmailAparadoQuandoInformado() {
    when(passwordEncoder.encode(any())).thenReturn("hash");

    Usuario usuario =
        service.cadastrar("Rafael", "52998224725", "segredo123", " rafael@exemplo.com ");

    assertThat(usuario.getEmail()).isEqualTo("rafael@exemplo.com");
  }

  @Test
  void recusaCpfJaCadastrado() {
    when(usuarioRepository.existsByCpf("52998224725")).thenReturn(true);

    assertThatThrownBy(() -> service.cadastrar("Rafael", "529.982.247-25", "segredo123", null))
        .isInstanceOf(ConflitoException.class)
        .hasMessage("CPF já cadastrado.");
    verify(usuarioRepository, never()).saveAndFlush(any());
  }

  @Test
  void cadastroSimultaneoComOMesmoCpfViraConflito() {
    when(passwordEncoder.encode(any())).thenReturn("hash");
    when(usuarioRepository.saveAndFlush(any()))
        .thenThrow(new DataIntegrityViolationException("uq_usuario_cpf"));

    assertThatThrownBy(() -> service.cadastrar("Rafael", "52998224725", "segredo123", null))
        .isInstanceOf(ConflitoException.class)
        .hasMessage("CPF já cadastrado.");
    verify(reservaRepository, never()).save(any());
  }

  @Test
  void recusaSenhaCurta() {
    assertThatThrownBy(() -> service.cadastrar("Rafael", "52998224725", "1234567", null))
        .isInstanceOf(ValorInvalidoException.class)
        .hasMessage("A senha deve ter pelo menos 8 caracteres.");
  }

  @Test
  void recusaCpfInvalidoSemTocarNoBanco() {
    assertThatThrownBy(() -> service.cadastrar("Rafael", "111.111.111-11", "segredo123", null))
        .isInstanceOf(ValorInvalidoException.class)
        .hasMessage("CPF inválido.");
    verify(usuarioRepository, never()).existsByCpf(any());
  }
}
