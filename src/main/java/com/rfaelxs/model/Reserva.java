package com.rfaelxs.model;

/**
 * Representa uma reserva financeira do usuário.
 * A Reserva de Emergência possui id fixo "reserva-emergencia" e não pode ser excluída.
 */
public class Reserva {

  private String id;
  private String nome;
  private double metaValor;
  private double saldoAtual;
  private boolean emergencia;

  public Reserva() {
  }

  /**
   * Cria uma nova reserva financeira.
   *
   * @param id        identificador único (use "reserva-emergencia" para a reserva obrigatória)
   * @param nome      nome da reserva exibido ao usuário
   * @param metaValor valor alvo que o usuário deseja acumular
   * @param emergencia {@code true} se esta é a Reserva de Emergência
   */
  public Reserva(String id, String nome, double metaValor, boolean emergencia) {
    this.id = id;
    this.nome = nome;
    this.metaValor = metaValor;
    this.saldoAtual = 0.0;
    this.emergencia = emergencia;
  }

  public String getId() {
    return id;
  }

  public String getNome() {
    return nome;
  }

  public void setNome(String nome) {
    this.nome = nome;
  }

  public double getMetaValor() {
    return metaValor;
  }

  public void setMetaValor(double metaValor) {
    this.metaValor = metaValor;
  }

  public double getSaldoAtual() {
    return saldoAtual;
  }

  public void setSaldoAtual(double saldoAtual) {
    this.saldoAtual = saldoAtual;
  }

  public boolean isEmergencia() {
    return emergencia;
  }
}
