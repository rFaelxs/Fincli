package com.rfaelxs.web.service;

import com.rfaelxs.web.domain.ContaPrevista;
import com.rfaelxs.web.repository.ContaPrevistaRepository;
import com.rfaelxs.web.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Contas previstas: o desconto que transforma "sobra" em "dá pra gastar". */
@Service
public class ContaPrevistaService {

  private final ContaPrevistaRepository contaRepository;
  private final UsuarioRepository usuarioRepository;

  public ContaPrevistaService(
      ContaPrevistaRepository contaRepository, UsuarioRepository usuarioRepository) {
    this.contaRepository = contaRepository;
    this.usuarioRepository = usuarioRepository;
  }

  /**
   * Cria uma conta prevista.
   *
   * @param mesReferencia mês da conta avulsa; {@code null} torna a conta recorrente
   * @throws ValorInvalidoException se o valor não for positivo ou o dia estiver fora de 1 a 31
   */
  @Transactional
  public ContaPrevista criar(Long usuarioId, String nome, BigDecimal valor, int diaVencimento,
      YearMonth mesReferencia) {
    if (valor == null || valor.signum() <= 0) {
      throw new ValorInvalidoException("O valor da conta prevista deve ser positivo.");
    }
    if (diaVencimento < 1 || diaVencimento > 31) {
      throw new ValorInvalidoException("O dia de vencimento deve estar entre 1 e 31.");
    }
    var usuario = usuarioRepository.getReferenceById(usuarioId);
    return contaRepository.save(new ContaPrevista(usuario, nome.trim(),
        Dinheiro.normalizar(valor), diaVencimento, mesReferencia));
  }

  /**
   * Remove uma conta prevista do usuário.
   *
   * @throws RecursoNaoEncontradoException se não existir ou for de outro usuário
   */
  @Transactional
  public void remover(Long usuarioId, UUID id) {
    contaRepository.delete(contaRepository.findByPublicIdAndUsuarioId(id, usuarioId)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Conta prevista não encontrada.")));
  }

  /**
   * Contas do mês que ainda não venceram, da que vence primeiro para a última.
   *
   * <p>O vencimento do próprio dia continua contando: a conta de hoje ainda vai sair da conta
   * bancária, então não pode entrar no "dá pra gastar".
   *
   * @param desde data de corte; na Gestão, hoje
   */
  @Transactional(readOnly = true)
  public List<ContaPrevista> aindaVencem(Long usuarioId, YearMonth mes, LocalDate desde) {
    return contaRepository.doMes(usuarioId, mes.toString()).stream()
        .filter(c -> !c.vencimentoEm(mes).isBefore(desde))
        .toList();
  }

  /** Soma dos valores das contas, em duas casas. */
  public static BigDecimal somar(List<ContaPrevista> contas) {
    return Dinheiro.normalizar(
        contas.stream().map(ContaPrevista::getValor).reduce(BigDecimal.ZERO, BigDecimal::add));
  }
}
