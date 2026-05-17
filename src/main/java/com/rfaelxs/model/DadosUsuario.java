package com.rfaelxs.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Estrutura raiz do arquivo JSON de cada usuário ({uuid}.json).
 * Agrega perfil, transações, reservas e movimentações de reservas.
 */
public class DadosUsuario {

  private User perfil;
  private List<Transacao> transacoes;
  private List<Reserva> reservas;
  private List<MovimentacaoReserva> movimentacoesReserva;

  public DadosUsuario() {
    this.transacoes = new ArrayList<>();
    this.reservas = new ArrayList<>();
    this.movimentacoesReserva = new ArrayList<>();
  }

  /**
   * Cria os dados iniciais de um usuário recém-cadastrado.
   *
   * @param perfil o usuário dono destes dados
   */
  public DadosUsuario(User perfil) {
    this.perfil = perfil;
    this.transacoes = new ArrayList<>();
    this.reservas = new ArrayList<>();
    this.movimentacoesReserva = new ArrayList<>();
  }

  public User getPerfil() {
    return perfil;
  }

  public void setPerfil(User perfil) {
    this.perfil = perfil;
  }

  public List<Transacao> getTransacoes() {
    return transacoes;
  }

  public List<Reserva> getReservas() {
    return reservas;
  }

  public List<MovimentacaoReserva> getMovimentacoesReserva() {
    return movimentacoesReserva;
  }
}
