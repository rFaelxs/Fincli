package com.rfaelxs.web.service;

import com.rfaelxs.web.domain.Transacao;
import com.rfaelxs.web.repository.TransacaoRepository;
import com.rfaelxs.web.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de transações. O serviço não guarda estado: cada operação recebe o id do dono, vindo da
 * sessão, e vai ao banco (ADR 0008).
 */
@Service
public class TransacaoService {

  static final String NAO_ENCONTRADA = "Transação não encontrada.";
  static final String VALOR_NEGATIVO = "O valor da transação não pode ser negativo.";

  private final TransacaoRepository transacaoRepository;
  private final UsuarioRepository usuarioRepository;

  public TransacaoService(
      TransacaoRepository transacaoRepository, UsuarioRepository usuarioRepository) {
    this.transacaoRepository = transacaoRepository;
    this.usuarioRepository = usuarioRepository;
  }

  /** Transações do usuário, da mais recente para a mais antiga. */
  @Transactional(readOnly = true)
  public List<Transacao> listar(Long usuarioId) {
    return transacaoRepository.findByUsuarioIdOrderByDataDescIdDesc(usuarioId);
  }

  /**
   * Busca uma transação do usuário.
   *
   * @throws RecursoNaoEncontradoException se não existir ou for de outro usuário
   */
  @Transactional(readOnly = true)
  public Transacao buscar(Long usuarioId, UUID id) {
    return transacaoRepository.findByPublicIdAndUsuarioId(id, usuarioId)
        .orElseThrow(() -> new RecursoNaoEncontradoException(NAO_ENCONTRADA));
  }

  /**
   * Cria uma transação para o usuário.
   *
   * @throws ValorInvalidoException se o valor for negativo
   */
  @Transactional
  public Transacao criar(Long usuarioId, DadosTransacao dados) {
    BigDecimal valor = validarValor(dados.valor());
    // Referência sem consulta: o id vem da sessão, então o usuário existe.
    var usuario = usuarioRepository.getReferenceById(usuarioId);
    return transacaoRepository.save(new Transacao(usuario, valor, dados.categoria().trim(),
        descricao(dados.descricao()), dados.data(), dados.tipo(), dados.essencial()));
  }

  /**
   * Substitui os campos de uma transação do usuário.
   *
   * @throws RecursoNaoEncontradoException se não existir ou for de outro usuário
   * @throws ValorInvalidoException        se o valor for negativo
   */
  @Transactional
  public Transacao editar(Long usuarioId, UUID id, DadosTransacao dados) {
    BigDecimal valor = validarValor(dados.valor());
    Transacao transacao = buscar(usuarioId, id);
    transacao.alterar(valor, dados.categoria().trim(), descricao(dados.descricao()),
        dados.data(), dados.tipo(), dados.essencial());
    return transacao;
  }

  /**
   * Exclui uma transação do usuário.
   *
   * @throws RecursoNaoEncontradoException se não existir ou for de outro usuário
   */
  @Transactional
  public void excluir(Long usuarioId, UUID id) {
    transacaoRepository.delete(buscar(usuarioId, id));
  }

  private static BigDecimal validarValor(BigDecimal valor) {
    if (valor == null || valor.signum() < 0) {
      throw new ValorInvalidoException(VALOR_NEGATIVO);
    }
    return Dinheiro.normalizar(valor);
  }

  /** Descrição em branco é gravada como nula, para a tela mostrar o traço de "sem descrição". */
  private static String descricao(String descricao) {
    return descricao == null || descricao.isBlank() ? null : descricao.trim();
  }
}
