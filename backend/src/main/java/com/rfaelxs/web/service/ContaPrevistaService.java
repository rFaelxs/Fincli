package com.rfaelxs.web.service;

import com.rfaelxs.web.domain.ContaPrevista;
import com.rfaelxs.web.domain.Usuario;
import com.rfaelxs.web.repository.ContaPrevistaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Regras das contas previstas — o desconto que transforma "sobra" em "dá pra gastar". */
@Service
public class ContaPrevistaService {

  private final ContaPrevistaRepository repository;

  public ContaPrevistaService(ContaPrevistaRepository repository) {
    this.repository = repository;
  }

  /**
   * Cria uma conta prevista.
   *
   * @param mesReferencia mês da conta avulsa; {@code null} torna a conta recorrente
   * @throws ValorInvalidoException se o valor não for positivo ou o dia estiver fora de 1..31
   */
  @Transactional
  public ContaPrevista criar(
      Usuario usuario,
      String nome,
      BigDecimal valor,
      int diaVencimento,
      YearMonth mesReferencia) {
    if (valor == null || valor.signum() <= 0) {
      throw new ValorInvalidoException("O valor da conta prevista deve ser positivo.");
    }
    if (diaVencimento < 1 || diaVencimento > 31) {
      throw new ValorInvalidoException("O dia de vencimento deve estar entre 1 e 31.");
    }
    return repository.save(
        new ContaPrevista(
            usuario, nome, Dinheiro.normalizar(valor), diaVencimento, mesReferencia));
  }

  /**
   * Remove uma conta prevista do usuário informado.
   *
   * @throws RecursoNaoEncontradoException se não existir ou não pertencer ao usuário
   */
  @Transactional
  public void remover(Usuario usuario, UUID publicId) {
    repository.delete(
        repository
            .findByPublicIdAndUsuarioId(publicId, usuario.getId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Conta prevista não encontrada.")));
  }

  /** @return contas que valem no mês, da que vence primeiro para a última */
  @Transactional(readOnly = true)
  public List<ContaPrevista> doMes(Usuario usuario, YearMonth mes) {
    return repository.doMes(usuario.getId(), mes.toString());
  }

  /**
   * Contas que ainda não venceram.
   *
   * <p>O vencimento do próprio dia continua contando: a conta de hoje ainda vai sair da conta
   * bancária, então não pode entrar no "dá pra gastar".
   *
   * @param mes      mês consultado
   * @param aPartirDe data de corte, normalmente hoje
   * @return as contas do mês com vencimento maior ou igual à data de corte
   */
  @Transactional(readOnly = true)
  public List<ContaPrevista> aVencer(Usuario usuario, YearMonth mes, LocalDate aPartirDe) {
    return doMes(usuario, mes).stream()
        .filter(c -> !c.vencimentoEm(mes).isBefore(aPartirDe))
        .toList();
  }

  /** @return soma das contas ainda por vencer no mês */
  public static BigDecimal somar(List<ContaPrevista> contas) {
    return Dinheiro.normalizar(
        contas.stream().map(ContaPrevista::getValor).reduce(BigDecimal.ZERO, BigDecimal::add));
  }
}
