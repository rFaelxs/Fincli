package com.rfaelxs.web.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.Nationalized;

/** Entrada ou saída lançada pelo usuário. */
@Entity
@Table(name = "transacao")
public class Transacao {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "public_id", nullable = false, updatable = false)
  private UUID publicId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id", nullable = false, updatable = false)
  private Usuario usuario;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal valor;

  @Nationalized
  @Column(nullable = false, length = 80)
  private String categoria;

  @Nationalized
  @Column(length = 255)
  private String descricao;

  @Column(name = "data_transacao", nullable = false)
  private LocalDate data;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private TipoTransacao tipo;

  @Column(nullable = false)
  private boolean essencial;

  protected Transacao() {
    // exigido pelo JPA
  }

  /**
   * Cria uma transação com um {@code publicId} novo. Os valores chegam já validados e
   * normalizados pelo serviço.
   */
  public Transacao(Usuario usuario, BigDecimal valor, String categoria, String descricao,
      LocalDate data, TipoTransacao tipo, boolean essencial) {
    this.publicId = UUID.randomUUID();
    this.usuario = usuario;
    alterar(valor, categoria, descricao, data, tipo, essencial);
  }

  /** Substitui todos os campos editáveis. O dono e o {@code publicId} não mudam. */
  public void alterar(BigDecimal valor, String categoria, String descricao, LocalDate data,
      TipoTransacao tipo, boolean essencial) {
    this.valor = valor;
    this.categoria = categoria;
    this.descricao = descricao;
    this.data = data;
    this.tipo = tipo;
    this.essencial = essencial;
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

  public BigDecimal getValor() {
    return valor;
  }

  public String getCategoria() {
    return categoria;
  }

  public String getDescricao() {
    return descricao;
  }

  public LocalDate getData() {
    return data;
  }

  public TipoTransacao getTipo() {
    return tipo;
  }

  public boolean isEssencial() {
    return essencial;
  }
}
