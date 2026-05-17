package com.rfaelxs.model;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Registra uma alocação ou saque em uma reserva financeira.
 * Cada movimentação aparece no extrato do usuário.
 */
public class MovimentacaoReserva {

  private UUID id;
  private String idReserva;
  private String nomeReserva;
  private double valor;
  private TipoMovimentacaoReserva tipo;
  private LocalDate data;

  public MovimentacaoReserva() {
  }

  /**
   * Cria um registro de movimentação em uma reserva.
   *
   * @param idReserva   identificador da reserva afetada
   * @param nomeReserva nome da reserva, para exibição no extrato
   * @param valor       valor movimentado (sempre positivo)
   * @param tipo        {@link TipoMovimentacaoReserva#ALOCACAO} ou {@link TipoMovimentacaoReserva#SAQUE}
   * @param data        data da movimentação
   */
  public MovimentacaoReserva(
      String idReserva,
      String nomeReserva,
      double valor,
      TipoMovimentacaoReserva tipo,
      LocalDate data) {
    this.id = UUID.randomUUID();
    this.idReserva = idReserva;
    this.nomeReserva = nomeReserva;
    this.valor = valor;
    this.tipo = tipo;
    this.data = data;
  }

  public UUID getId() {
    return id;
  }

  public String getIdReserva() {
    return idReserva;
  }

  public String getNomeReserva() {
    return nomeReserva;
  }

  public double getValor() {
    return valor;
  }

  public TipoMovimentacaoReserva getTipo() {
    return tipo;
  }

  public LocalDate getData() {
    return data;
  }
}
