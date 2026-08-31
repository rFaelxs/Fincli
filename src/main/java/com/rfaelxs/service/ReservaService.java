package com.rfaelxs.service;

import com.rfaelxs.exception.ValorInvalidoException;
import com.rfaelxs.model.DadosUsuario;
import com.rfaelxs.model.MovimentacaoReserva;
import com.rfaelxs.model.Reserva;
import com.rfaelxs.model.TipoMovimentacaoReserva;
import com.rfaelxs.repository.UsuarioRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Regras de negócio para gestão de reservas financeiras do usuário logado.
 * A Reserva de Emergência (id="reserva-emergencia") não pode ser excluída.
 */
public class ReservaService {

  static final String ID_EMERGENCIA = "reserva-emergencia";

  private final UsuarioRepository repository;
  private final UUID idUsuario;
  private final DadosUsuario dados;

  /**
   * Inicializa o serviço com os dados já carregados do usuário.
   *
   * <p>A instância de {@link DadosUsuario} <strong>deve</strong> ser a mesma compartilhada com
   * {@link TransacaoService}. Como cada mutação regrava o documento inteiro, dois serviços
   * operando sobre cópias distintas sobrescrevem as alterações um do outro.
   *
   * @param repository repositório unificado de dados
   * @param idUsuario  UUID do usuário logado
   * @param dados      dados do usuário, compartilhados entre os serviços da sessão
   */
  public ReservaService(UsuarioRepository repository, UUID idUsuario, DadosUsuario dados) {
    this.repository = repository;
    this.idUsuario = idUsuario;
    this.dados = dados;
  }

  /**
   * Cria uma nova reserva financeira e persiste.
   *
   * @param nome      nome da reserva
   * @param metaValor valor alvo da reserva
   * @return a reserva criada
   */
  public Reserva criarReserva(String nome, double metaValor) {
    if (metaValor < 0) {
      throw new ValorInvalidoException("A meta da reserva não pode ser negativa.");
    }
    Reserva reserva = new Reserva(UUID.randomUUID().toString(), nome, metaValor, false);
    dados.getReservas().add(reserva);
    repository.salvarDados(idUsuario, dados);
    return reserva;
  }

  /** @return lista de todas as reservas do usuário */
  public List<Reserva> listarReservas() {
    return dados.getReservas();
  }

  /**
   * Aloca saldo disponível para uma reserva.
   *
   * @param idReserva       identificador da reserva de destino
   * @param valor           valor a alocar
   * @param saldoDisponivel saldo livre do usuário antes desta operação
   * @throws ValorInvalidoException se o valor for inválido ou o saldo for insuficiente
   */
  public void alocarSaldo(String idReserva, double valor, double saldoDisponivel) {
    if (valor <= 0) {
      throw new ValorInvalidoException("O valor de alocação deve ser positivo.");
    }
    if (valor > saldoDisponivel) {
      throw new ValorInvalidoException("Saldo insuficiente para esta alocação.");
    }
    Reserva reserva = buscarPorId(idReserva);
    reserva.setSaldoAtual(reserva.getSaldoAtual() + valor);
    dados.getMovimentacoesReserva().add(
        new MovimentacaoReserva(idReserva, reserva.getNome(), valor,
            TipoMovimentacaoReserva.ALOCACAO, LocalDate.now()));
    repository.salvarDados(idUsuario, dados);
  }

  /**
   * Realiza um saque de uma reserva, devolvendo o valor ao saldo disponível.
   *
   * @param idReserva identificador da reserva de origem
   * @param valor     valor a sacar
   * @throws ValorInvalidoException se o valor for inválido ou o saldo da reserva for insuficiente
   */
  public void sacarReserva(String idReserva, double valor) {
    if (valor <= 0) {
      throw new ValorInvalidoException("O valor do saque deve ser positivo.");
    }
    Reserva reserva = buscarPorId(idReserva);
    if (valor > reserva.getSaldoAtual()) {
      throw new ValorInvalidoException("Saldo da reserva insuficiente para este saque.");
    }
    reserva.setSaldoAtual(reserva.getSaldoAtual() - valor);
    dados.getMovimentacoesReserva().add(
        new MovimentacaoReserva(idReserva, reserva.getNome(), valor,
            TipoMovimentacaoReserva.SAQUE, LocalDate.now()));
    repository.salvarDados(idUsuario, dados);
  }

  /**
   * Atualiza a meta de valor da Reserva de Emergência.
   *
   * @param novoValor nova meta (deve ser >= 0)
   */
  public void atualizarMetaEmergencia(double novoValor) {
    if (novoValor < 0) {
      throw new ValorInvalidoException("A meta não pode ser negativa.");
    }
    buscarPorId(ID_EMERGENCIA).setMetaValor(novoValor);
    repository.salvarDados(idUsuario, dados);
  }

  /**
   * Exclui uma reserva pelo id. A Reserva de Emergência não pode ser excluída.
   *
   * @param idReserva identificador da reserva
   * @throws IllegalStateException se tentar excluir a Reserva de Emergência
   */
  public void excluirReserva(String idReserva) {
    if (ID_EMERGENCIA.equals(idReserva)) {
      throw new IllegalStateException("A Reserva de Emergência não pode ser excluída.");
    }
    dados.getReservas().removeIf(r -> r.getId().equals(idReserva));
    repository.salvarDados(idUsuario, dados);
  }

  /**
   * Retorna todas as movimentações de reserva do usuário, para exibição no extrato.
   *
   * @return lista de movimentações em ordem de inserção
   */
  public List<MovimentacaoReserva> listarMovimentacoes() {
    return dados.getMovimentacoesReserva();
  }

  private Reserva buscarPorId(String idReserva) {
    return dados.getReservas().stream()
        .filter(r -> r.getId().equals(idReserva))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Reserva não encontrada: " + idReserva));
  }
}
