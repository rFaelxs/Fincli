package com.rfaelxs.web.service;

import com.rfaelxs.web.domain.MovimentacaoReserva;
import com.rfaelxs.web.domain.Reserva;
import com.rfaelxs.web.domain.TipoMovimentacaoReserva;
import com.rfaelxs.web.domain.Usuario;
import com.rfaelxs.web.repository.MovimentacaoReservaRepository;
import com.rfaelxs.web.repository.ReservaRepository;
import com.rfaelxs.web.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de reservas.
 *
 * <p>Alocar, sacar e excluir travam a linha do usuário antes de qualquer leitura. O saldo
 * disponível é do usuário, então travar só a reserva não bastaria (ADR 0008).
 */
@Service
public class ReservaService {

  static final String NAO_ENCONTRADA = "Reserva não encontrada.";

  private final ReservaRepository reservaRepository;
  private final MovimentacaoReservaRepository movimentacaoRepository;
  private final UsuarioRepository usuarioRepository;
  private final SaldoService saldoService;
  private final Clock relogio;

  /** Cria o serviço com os repositórios, o cálculo de saldo e o relógio da aplicação. */
  public ReservaService(
      ReservaRepository reservaRepository,
      MovimentacaoReservaRepository movimentacaoRepository,
      UsuarioRepository usuarioRepository,
      SaldoService saldoService,
      Clock relogio) {
    this.reservaRepository = reservaRepository;
    this.movimentacaoRepository = movimentacaoRepository;
    this.usuarioRepository = usuarioRepository;
    this.saldoService = saldoService;
    this.relogio = relogio;
  }

  /** Reservas do usuário, com a de emergência primeiro e as demais por nome. */
  @Transactional(readOnly = true)
  public List<Reserva> listar(Long usuarioId) {
    return reservaRepository.findByUsuarioIdOrderByEmergenciaDescNomeAsc(usuarioId);
  }

  /**
   * Busca uma reserva do usuário.
   *
   * @throws RecursoNaoEncontradoException se não existir ou for de outro usuário
   */
  @Transactional(readOnly = true)
  public Reserva buscar(Long usuarioId, UUID id) {
    return reservaRepository.findByPublicIdAndUsuarioId(id, usuarioId)
        .orElseThrow(() -> new RecursoNaoEncontradoException(NAO_ENCONTRADA));
  }

  /**
   * Cria uma reserva comum, com saldo zero.
   *
   * @throws ValorInvalidoException se a meta for negativa
   */
  @Transactional
  public Reserva criar(Long usuarioId, String nome, BigDecimal metaValor) {
    BigDecimal meta = naoNegativo(metaValor, "A meta da reserva não pode ser negativa.");
    var usuario = usuarioRepository.getReferenceById(usuarioId);
    return reservaRepository.save(new Reserva(usuario, nome.trim(), meta, false));
  }

  /**
   * Move dinheiro do saldo disponível para a reserva.
   *
   * @throws ValorInvalidoException        se o valor não for positivo ou passar do disponível
   * @throws RecursoNaoEncontradoException se a reserva não existir ou for de outro usuário
   */
  @Transactional
  public Reserva alocar(Long usuarioId, UUID id, BigDecimal valor) {
    BigDecimal montante = positivo(valor, "O valor de alocação deve ser positivo.");
    Usuario usuario = travar(usuarioId);
    Reserva reserva = buscar(usuarioId, id);

    // Lido depois do lock: um saldo lido antes poderia autorizar duas alocações que juntas
    // passam do disponível.
    if (montante.compareTo(saldoService.disponivel(usuarioId)) > 0) {
      throw new ValorInvalidoException("Saldo insuficiente para esta alocação.");
    }

    reserva.alocar(montante);
    registrar(usuario, reserva, montante, TipoMovimentacaoReserva.ALOCACAO);
    return reserva;
  }

  /**
   * Devolve dinheiro da reserva ao saldo disponível.
   *
   * @throws ValorInvalidoException        se o valor não for positivo ou passar do saldo da reserva
   * @throws RecursoNaoEncontradoException se a reserva não existir ou for de outro usuário
   */
  @Transactional
  public Reserva sacar(Long usuarioId, UUID id, BigDecimal valor) {
    BigDecimal montante = positivo(valor, "O valor do saque deve ser positivo.");
    Usuario usuario = travar(usuarioId);
    Reserva reserva = buscar(usuarioId, id);

    if (montante.compareTo(reserva.getSaldoAtual()) > 0) {
      throw new ValorInvalidoException("Saldo da reserva insuficiente para este saque.");
    }

    reserva.sacar(montante);
    registrar(usuario, reserva, montante, TipoMovimentacaoReserva.SAQUE);
    return reserva;
  }

  /**
   * Troca a meta da reserva.
   *
   * @throws ValorInvalidoException        se a meta for negativa
   * @throws RecursoNaoEncontradoException se a reserva não existir ou for de outro usuário
   */
  @Transactional
  public Reserva alterarMeta(Long usuarioId, UUID id, BigDecimal novaMeta) {
    BigDecimal meta = naoNegativo(novaMeta, "A meta não pode ser negativa.");
    Reserva reserva = buscar(usuarioId, id);
    reserva.alterarMeta(meta);
    return reserva;
  }

  /**
   * Exclui uma reserva sem saldo. As movimentações dela continuam no extrato.
   *
   * @throws ConflitoException             se for a Reserva de Emergência ou ainda tiver saldo
   * @throws RecursoNaoEncontradoException se a reserva não existir ou for de outro usuário
   */
  @Transactional
  public void excluir(Long usuarioId, UUID id) {
    // Travado para que uma alocação simultânea não ponha saldo numa reserva sendo excluída.
    travar(usuarioId);
    Reserva reserva = buscar(usuarioId, id);
    if (reserva.isEmergencia()) {
      throw new ConflitoException("A Reserva de Emergência não pode ser excluída.");
    }
    if (reserva.getSaldoAtual().signum() > 0) {
      throw new ConflitoException("A reserva ainda tem saldo. Saque o valor antes de excluí-la.");
    }
    reservaRepository.delete(reserva);
  }

  private Usuario travar(Long usuarioId) {
    return usuarioRepository.travar(usuarioId).orElseThrow();
  }

  private void registrar(Usuario usuario, Reserva reserva, BigDecimal valor,
      TipoMovimentacaoReserva tipo) {
    movimentacaoRepository.save(
        new MovimentacaoReserva(usuario, reserva, valor, tipo, LocalDate.now(relogio)));
  }

  private static BigDecimal positivo(BigDecimal valor, String mensagem) {
    if (valor == null || valor.signum() <= 0) {
      throw new ValorInvalidoException(mensagem);
    }
    return Dinheiro.normalizar(valor);
  }

  private static BigDecimal naoNegativo(BigDecimal valor, String mensagem) {
    if (valor == null || valor.signum() < 0) {
      throw new ValorInvalidoException(mensagem);
    }
    return Dinheiro.normalizar(valor);
  }
}
