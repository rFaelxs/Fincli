package com.rfaelxs.web.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;
import org.hibernate.annotations.Nationalized;

/**
 * Conta que o usuário sabe que vai vencer — aluguel, energia, cartão.
 *
 * <p>É o que separa "sobra" de "dá pra gastar": o número do topo da tela Hoje já desconta as
 * contas ainda por vencer no mês corrente.
 *
 * <p>Uma conta recorrente vale para todo mês e tem {@code mesReferencia} nulo. Uma conta
 * avulsa vale só para o mês gravado em {@code mesReferencia}.
 */
@Entity
@Table(name = "conta_prevista")
public class ContaPrevista {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "public_id", nullable = false, updatable = false)
  private UUID publicId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id", nullable = false)
  private Usuario usuario;

  @Nationalized
  @Column(nullable = false, length = 120)
  private String nome;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal valor;

  @Column(name = "dia_vencimento", nullable = false)
  private int diaVencimento;

  @Column(nullable = false)
  private boolean recorrente;

  /** Mês em {@code yyyy-MM} para contas avulsas; nulo quando a conta é recorrente. */
  @Column(name = "mes_referencia", length = 7)
  private String mesReferencia;

  @Column(name = "criada_em", nullable = false, insertable = false, updatable = false)
  private Instant criadaEm;

  protected ContaPrevista() {
    // exigido pelo JPA
  }

  public ContaPrevista(
      Usuario usuario,
      String nome,
      BigDecimal valor,
      int diaVencimento,
      YearMonth mesReferencia) {
    this.publicId = UUID.randomUUID();
    this.usuario = usuario;
    this.nome = nome;
    this.valor = valor;
    this.diaVencimento = diaVencimento;
    this.recorrente = mesReferencia == null;
    this.mesReferencia = mesReferencia != null ? mesReferencia.toString() : null;
  }

  /**
   * Diz se a conta entra no mês informado.
   *
   * @param mes mês consultado
   * @return true para conta recorrente ou avulsa daquele mês
   */
  public boolean valeNoMes(YearMonth mes) {
    return recorrente || mes.toString().equals(mesReferencia);
  }

  /**
   * Data de vencimento dentro do mês informado.
   *
   * <p>Um dia 31 em fevereiro cai no último dia do mês, em vez de virar data inválida.
   *
   * @param mes mês consultado
   * @return o vencimento já ajustado ao tamanho do mês
   */
  public LocalDate vencimentoEm(YearMonth mes) {
    return mes.atDay(Math.min(diaVencimento, mes.lengthOfMonth()));
  }

  public Long getId() {
    return id;
  }

  public UUID getPublicId() {
    return publicId;
  }

  public Usuario getUsuario() {
    return usuario;
  }

  public String getNome() {
    return nome;
  }

  public void setNome(String nome) {
    this.nome = nome;
  }

  public BigDecimal getValor() {
    return valor;
  }

  public void setValor(BigDecimal valor) {
    this.valor = valor;
  }

  public int getDiaVencimento() {
    return diaVencimento;
  }

  public void setDiaVencimento(int diaVencimento) {
    this.diaVencimento = diaVencimento;
  }

  public boolean isRecorrente() {
    return recorrente;
  }

  public String getMesReferencia() {
    return mesReferencia;
  }

  public Instant getCriadaEm() {
    return criadaEm;
  }
}
