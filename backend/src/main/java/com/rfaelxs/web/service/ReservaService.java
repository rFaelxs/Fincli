package com.rfaelxs.web.service;

import com.rfaelxs.web.domain.MovimentacaoReserva;
import com.rfaelxs.web.domain.Reserva;
import com.rfaelxs.web.domain.TipoMovimentacaoReserva;
import com.rfaelxs.web.domain.Usuario;
import com.rfaelxs.web.repository.MovimentacaoReservaRepository;
import com.rfaelxs.web.repository.ReservaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de negócio de reservas financeiras.
 *
 * <p>A Reserva de Emergência não pode ser excluída — regra herdada do CLI. Alocação e saque
 * travam a linha da reserva para que duas requisições simultâneas não sobrescrevam o saldo.
 */
@Service
public class ReservaService {

  public static final String NOME_EMERGENCIA = "Reserva de Emergência";

  private final ReservaRepository reservaRepository;
  private final MovimentacaoReservaRepository movimentacaoRepository;
  private final TransacaoService transacaoService;

  public ReservaService(
      ReservaRepository reservaRepository,
      MovimentacaoReservaRepository movimentacaoRepository,
      TransacaoService transacaoService) {
    this.reservaRepository = reservaRepository;
    this.movimentacaoRepository = movimentacaoRepository;
    this.transacaoService = transacaoService;
  }

  /**
   * Cria a Reserva de Emergência do usuário. Chamada uma única vez, no cadastro.
   *
   * @return a reserva criada, com meta zerada
   */
  @Transactional
  public Reserva criarReservaDeEmergencia(Usuario usuario) {
    return reservaRepository.save(new Reserva(usuario, NOME_EMERGENCIA, Dinheiro.ZERO, true));
  }

  /**
   * Cria uma reserva comum.
   *
   * @throws ValorInvalidoException se a meta for negativa
   */
  @Transactional
  public Reserva criar(Usuario usuario, String nome, BigDecimal metaValor) {
    if (metaValor == null || metaValor.signum() < 0) {
      throw new ValorInvalidoException("A meta da reserva não pode ser negativa.");
    }
    return reservaRepository.save(
        new Reserva(usuario, nome, Dinheiro.normalizar(metaValor), false));
  }

  /**
   * Busca uma reserva do usuário informado.
   *
   * @throws RecursoNaoEncontradoException se não existir ou não pertencer ao usuário
   */
  @Transactional(readOnly = true)
  public Reserva buscarPorId(Usuario usuario, UUID publicId) {
    return buscar(usuario, publicId);
  }

  /** @return reservas do usuário, com a de emergência primeiro */
  @Transactional(readOnly = true)
  public List<Reserva> listar(Usuario usuario) {
    return reservaRepository.findByUsuarioIdOrderByEmergenciaDescNomeAsc(usuario.getId());
  }

  /**
   * Move saldo disponível para uma reserva.
   *
   * @throws ValorInvalidoException        se o valor não for positivo ou exceder o saldo livre
   * @throws RecursoNaoEncontradoException se a reserva não existir ou não for do usuário
   */
  @Transactional
  public Reserva alocar(Usuario usuario, UUID publicId, BigDecimal valor) {
    BigDecimal montante = exigirPositivo(valor, "O valor de alocação deve ser positivo.");
    Reserva reserva = buscarParaAtualizacao(usuario, publicId);

    // Lido dentro da transação, já com a reserva travada: um saldo calculado antes do lock
    // poderia autorizar duas alocações que juntas excedem o disponível.
    BigDecimal disponivel = transacaoService.calcularSaldoDisponivel(usuario);
    if (montante.compareTo(disponivel) > 0) {
      throw new ValorInvalidoException("Saldo insuficiente para esta alocação.");
    }

    reserva.setSaldoAtual(Dinheiro.normalizar(reserva.getSaldoAtual().add(montante)));
    registrar(usuario, reserva, montante, TipoMovimentacaoReserva.ALOCACAO);
    return reserva;
  }

  /**
   * Devolve saldo de uma reserva ao disponível.
   *
   * @throws ValorInvalidoException        se o valor não for positivo ou exceder o saldo da reserva
   * @throws RecursoNaoEncontradoException se a reserva não existir ou não for do usuário
   */
  @Transactional
  public Reserva sacar(Usuario usuario, UUID publicId, BigDecimal valor) {
    BigDecimal montante = exigirPositivo(valor, "O valor do saque deve ser positivo.");
    Reserva reserva = buscarParaAtualizacao(usuario, publicId);

    if (montante.compareTo(reserva.getSaldoAtual()) > 0) {
      throw new ValorInvalidoException("Saldo da reserva insuficiente para este saque.");
    }

    reserva.setSaldoAtual(Dinheiro.normalizar(reserva.getSaldoAtual().subtract(montante)));
    registrar(usuario, reserva, montante, TipoMovimentacaoReserva.SAQUE);
    return reserva;
  }

  /**
   * Atualiza a meta de uma reserva.
   *
   * @throws ValorInvalidoException se a meta for negativa
   */
  @Transactional
  public Reserva atualizarMeta(Usuario usuario, UUID publicId, BigDecimal novaMeta) {
    if (novaMeta == null || novaMeta.signum() < 0) {
      throw new ValorInvalidoException("A meta não pode ser negativa.");
    }
    Reserva reserva = buscar(usuario, publicId);
    reserva.setMetaValor(Dinheiro.normalizar(novaMeta));
    return reserva;
  }

  /**
   * Exclui uma reserva. A Reserva de Emergência é protegida, como no CLI.
   *
   * @throws ConflitoException             se for a Reserva de Emergência ou tiver saldo alocado
   * @throws RecursoNaoEncontradoException se a reserva não existir ou não for do usuário
   */
  @Transactional
  public void excluir(Usuario usuario, UUID publicId) {
    Reserva reserva = buscar(usuario, publicId);
    if (reserva.isEmergencia()) {
      throw new ConflitoException("A Reserva de Emergência não pode ser excluída.");
    }
    if (reserva.getSaldoAtual().signum() > 0) {
      throw new ConflitoException(
          "A reserva ainda tem saldo. Saque o valor antes de excluí-la.");
    }
    reservaRepository.delete(reserva);
  }

  /** @return movimentações do usuário, da mais recente para a mais antiga */
  @Transactional(readOnly = true)
  public List<MovimentacaoReserva> listarMovimentacoes(Usuario usuario) {
    return movimentacaoRepository.findByUsuarioIdOrderByDataMovimentoDescIdDesc(usuario.getId());
  }

  /** @return a Reserva de Emergência do usuário, se existir */
  @Transactional(readOnly = true)
  public Reserva buscarEmergencia(Usuario usuario) {
    return reservaRepository.findByUsuarioIdAndEmergenciaTrue(usuario.getId()).orElse(null);
  }

  private void registrar(
      Usuario usuario, Reserva reserva, BigDecimal valor, TipoMovimentacaoReserva tipo) {
    movimentacaoRepository.save(
        new MovimentacaoReserva(usuario, reserva, valor, tipo, LocalDate.now()));
  }

  private BigDecimal exigirPositivo(BigDecimal valor, String mensagem) {
    if (valor == null || valor.signum() <= 0) {
      throw new ValorInvalidoException(mensagem);
    }
    return Dinheiro.normalizar(valor);
  }

  private Reserva buscar(Usuario usuario, UUID publicId) {
    return reservaRepository
        .findByPublicIdAndUsuarioId(publicId, usuario.getId())
        .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva não encontrada."));
  }

  private Reserva buscarParaAtualizacao(Usuario usuario, UUID publicId) {
    return reservaRepository
        .findParaAtualizacao(publicId, usuario.getId())
        .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva não encontrada."));
  }
}
