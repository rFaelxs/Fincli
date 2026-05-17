package com.rfaelxs.service;

import com.rfaelxs.exception.ValorInvalidoException;
import com.rfaelxs.model.DadosUsuario;
import com.rfaelxs.model.Reserva;
import com.rfaelxs.model.TipoTransacao;
import com.rfaelxs.model.Transacao;
import com.rfaelxs.repository.UsuarioRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * Regras de negócio para gestão de transações financeiras do usuário logado.
 * Persiste via {@link UsuarioRepository} após cada mutação.
 */
public class TransacaoService {

  private final UsuarioRepository repository;
  private final UUID idUsuario;
  private final DadosUsuario dados;

  /**
   * Inicializa o serviço carregando os dados do usuário a partir do repositório.
   *
   * @param repository repositório unificado de dados
   * @param idUsuario  UUID do usuário logado
   */
  public TransacaoService(UsuarioRepository repository, UUID idUsuario) {
    this.repository = repository;
    this.idUsuario = idUsuario;
    this.dados = repository.carregarDados(idUsuario);
  }

  /**
   * Cria e persiste uma nova transação.
   *
   * @param valor     valor monetário (deve ser >= 0)
   * @param categoria categoria da transação
   * @param descricao descrição livre
   * @param data      data da transação
   * @param tipo      {@link TipoTransacao#ENTRADA} ou {@link TipoTransacao#SAIDA}
   * @param essencial {@code true} se a despesa é essencial (ignorado para ENTRADA)
   * @throws ValorInvalidoException se o valor for negativo
   */
  public void adicionarTransacao(
      double valor,
      String categoria,
      String descricao,
      LocalDate data,
      TipoTransacao tipo,
      boolean essencial) {
    if (valor < 0) {
      throw new ValorInvalidoException("O valor inserido é inválido!");
    }
    dados.getTransacoes().add(new Transacao(valor, categoria, descricao, data, tipo, essencial));
    repository.salvarDados(idUsuario, dados);
  }

  /**
   * Edita os campos de uma transação existente.
   *
   * @param id        UUID da transação a editar
   * @param valor     novo valor (deve ser >= 0)
   * @param categoria nova categoria
   * @param descricao nova descrição
   * @param data      nova data
   * @param tipo      novo tipo
   * @param essencial nova flag de essencialidade
   * @throws ValorInvalidoException se o valor for negativo
   */
  public void editarTransacao(
      UUID id,
      double valor,
      String categoria,
      String descricao,
      LocalDate data,
      TipoTransacao tipo,
      boolean essencial) {
    if (valor < 0) {
      throw new ValorInvalidoException("O valor inserido é inválido!");
    }
    dados.getTransacoes().stream()
        .filter(t -> t.getId().equals(id))
        .findFirst()
        .ifPresent(t -> {
          t.setValorTransacao(valor);
          t.setCategoria(categoria);
          t.setDescTransacao(descricao);
          t.setDataTransacao(data);
          t.setTipo(tipo);
          t.setEssencial(essencial);
        });
    repository.salvarDados(idUsuario, dados);
  }

  /**
   * Remove a transação com o ID informado e persiste.
   *
   * @param id UUID da transação a remover
   */
  public void removerTransacao(UUID id) {
    dados.getTransacoes().removeIf(t -> t.getId().equals(id));
    repository.salvarDados(idUsuario, dados);
  }

  /** @return lista de todas as transações do usuário */
  public List<Transacao> listarTransacoes() {
    return dados.getTransacoes();
  }

  /**
   * Calcula o saldo disponível do usuário.
   * Fórmula: sum(ENTRADA) - sum(SAIDA) - sum(saldoAtual das reservas)
   *
   * @param reservas lista de reservas do usuário
   * @return saldo calculado sob demanda (nunca persistido)
   */
  public double calcularSaldo(List<Reserva> reservas) {
    double entradas = dados.getTransacoes().stream()
        .filter(t -> t.getTipo() == TipoTransacao.ENTRADA)
        .mapToDouble(Transacao::getValorTransacao)
        .sum();
    double saidas = dados.getTransacoes().stream()
        .filter(t -> t.getTipo() == TipoTransacao.SAIDA)
        .mapToDouble(Transacao::getValorTransacao)
        .sum();
    double alocado = reservas.stream()
        .mapToDouble(Reserva::getSaldoAtual)
        .sum();
    return entradas - saidas - alocado;
  }

  /**
   * Soma todas as entradas do mês informado.
   *
   * @param mes mês de referência
   * @return total de entradas no mês
   */
  public double totalEntradasMes(YearMonth mes) {
    return dados.getTransacoes().stream()
        .filter(t -> t.getTipo() == TipoTransacao.ENTRADA)
        .filter(t -> YearMonth.from(t.getDataTransacao()).equals(mes))
        .mapToDouble(Transacao::getValorTransacao)
        .sum();
  }

  /**
   * Soma todas as saídas do mês informado.
   *
   * @param mes mês de referência
   * @return total de saídas no mês
   */
  public double totalSaidasMes(YearMonth mes) {
    return dados.getTransacoes().stream()
        .filter(t -> t.getTipo() == TipoTransacao.SAIDA)
        .filter(t -> YearMonth.from(t.getDataTransacao()).equals(mes))
        .mapToDouble(Transacao::getValorTransacao)
        .sum();
  }
}
