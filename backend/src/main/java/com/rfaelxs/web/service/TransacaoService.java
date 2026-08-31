package com.rfaelxs.web.service;

import com.rfaelxs.web.domain.TipoTransacao;
import com.rfaelxs.web.domain.Transacao;
import com.rfaelxs.web.domain.Usuario;
import com.rfaelxs.web.repository.ReservaRepository;
import com.rfaelxs.web.repository.TransacaoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de negócio de transações.
 *
 * <p>Diferente da versão CLI, o serviço não guarda estado do usuário: cada operação recebe o
 * dono e vai ao banco. Era o estado em campo que causava a perda de dados no CLI
 * (escopo-web.md §1.2).
 */
@Service
public class TransacaoService {

  private final TransacaoRepository transacaoRepository;
  private final ReservaRepository reservaRepository;

  public TransacaoService(
      TransacaoRepository transacaoRepository, ReservaRepository reservaRepository) {
    this.transacaoRepository = transacaoRepository;
    this.reservaRepository = reservaRepository;
  }

  /**
   * Cria uma transação para o usuário informado.
   *
   * @throws ValorInvalidoException se o valor for negativo
   */
  @Transactional
  public Transacao adicionar(
      Usuario usuario,
      BigDecimal valor,
      String categoria,
      String descricao,
      LocalDate data,
      TipoTransacao tipo,
      boolean essencial) {
    return transacaoRepository.save(
        new Transacao(
            usuario, validarValor(valor), categoria, descricao, data, tipo, essencial));
  }

  /**
   * Edita uma transação do usuário informado.
   *
   * @throws RecursoNaoEncontradoException se não existir ou não pertencer ao usuário
   * @throws ValorInvalidoException        se o valor for negativo
   */
  @Transactional
  public Transacao editar(
      Usuario usuario,
      UUID publicId,
      BigDecimal valor,
      String categoria,
      String descricao,
      LocalDate data,
      TipoTransacao tipo,
      boolean essencial) {
    Transacao transacao = buscar(usuario, publicId);
    transacao.setValor(validarValor(valor));
    transacao.setCategoria(categoria);
    transacao.setDescricao(descricao);
    transacao.setDataTransacao(data);
    transacao.setTipo(tipo);
    transacao.setEssencial(essencial);
    return transacao;
  }

  /**
   * Remove uma transação do usuário informado.
   *
   * @throws RecursoNaoEncontradoException se não existir ou não pertencer ao usuário
   */
  @Transactional
  public void remover(Usuario usuario, UUID publicId) {
    transacaoRepository.delete(buscar(usuario, publicId));
  }

  /** @return transações do usuário, da mais recente para a mais antiga */
  @Transactional(readOnly = true)
  public List<Transacao> listar(Usuario usuario) {
    return transacaoRepository.findByUsuarioIdOrderByDataTransacaoDescIdDesc(usuario.getId());
  }

  /**
   * Saldo disponível: entradas − saídas − total alocado em reservas.
   *
   * <p>Somado no banco, não em memória: a versão CLI carregava todas as transações para somar.
   *
   * @return saldo livre do usuário, podendo ser negativo
   */
  @Transactional(readOnly = true)
  public BigDecimal calcularSaldoDisponivel(Usuario usuario) {
    BigDecimal entradas =
        transacaoRepository.somarPorTipo(usuario.getId(), TipoTransacao.ENTRADA);
    BigDecimal saidas = transacaoRepository.somarPorTipo(usuario.getId(), TipoTransacao.SAIDA);
    BigDecimal alocado = reservaRepository.somarSaldoAlocado(usuario.getId());
    return Dinheiro.normalizar(entradas.subtract(saidas).subtract(alocado));
  }

  /** @return soma das entradas do mês informado */
  @Transactional(readOnly = true)
  public BigDecimal totalEntradasMes(Usuario usuario, YearMonth mes) {
    return somarMes(usuario, TipoTransacao.ENTRADA, mes);
  }

  /** @return soma das saídas do mês informado */
  @Transactional(readOnly = true)
  public BigDecimal totalSaidasMes(Usuario usuario, YearMonth mes) {
    return somarMes(usuario, TipoTransacao.SAIDA, mes);
  }

  private BigDecimal somarMes(Usuario usuario, TipoTransacao tipo, YearMonth mes) {
    return Dinheiro.normalizar(
        transacaoRepository.somarPorTipoNoPeriodo(
            usuario.getId(), tipo, mes.atDay(1), mes.atEndOfMonth()));
  }

  private Transacao buscar(Usuario usuario, UUID publicId) {
    return transacaoRepository
        .findByPublicIdAndUsuarioId(publicId, usuario.getId())
        .orElseThrow(() -> new RecursoNaoEncontradoException("Transação não encontrada."));
  }

  private BigDecimal validarValor(BigDecimal valor) {
    if (valor == null || valor.signum() < 0) {
      throw new ValorInvalidoException("O valor da transação não pode ser negativo.");
    }
    return Dinheiro.normalizar(valor);
  }
}
