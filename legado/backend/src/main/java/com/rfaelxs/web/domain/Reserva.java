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
import java.util.UUID;
import org.hibernate.annotations.Nationalized;

/**
 * Meta de reserva financeira do usuário.
 *
 * <p>A Reserva de Emergência é marcada por {@link #emergencia}, e não mais por um id constante
 * como no CLI. O banco tem índice único filtrado garantindo no máximo uma por usuário.
 */
@Entity
@Table(name = "reserva")
public class Reserva {

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

  @Column(name = "meta_valor", nullable = false, precision = 19, scale = 2)
  private BigDecimal metaValor;

  @Column(name = "saldo_atual", nullable = false, precision = 19, scale = 2)
  private BigDecimal saldoAtual;

  @Column(nullable = false)
  private boolean emergencia;

  @Column(name = "criada_em", nullable = false, insertable = false, updatable = false)
  private Instant criadaEm;

  protected Reserva() {
    // exigido pelo JPA
  }

  public Reserva(Usuario usuario, String nome, BigDecimal metaValor, boolean emergencia) {
    this.publicId = UUID.randomUUID();
    this.usuario = usuario;
    this.nome = nome;
    this.metaValor = metaValor;
    this.saldoAtual = BigDecimal.ZERO;
    this.emergencia = emergencia;
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

  public BigDecimal getMetaValor() {
    return metaValor;
  }

  public void setMetaValor(BigDecimal metaValor) {
    this.metaValor = metaValor;
  }

  public BigDecimal getSaldoAtual() {
    return saldoAtual;
  }

  public void setSaldoAtual(BigDecimal saldoAtual) {
    this.saldoAtual = saldoAtual;
  }

  public boolean isEmergencia() {
    return emergencia;
  }

  public Instant getCriadaEm() {
    return criadaEm;
  }
}
