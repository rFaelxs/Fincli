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
 * Reserva financeira do usuário, com meta e saldo próprios.
 *
 * <p>A Reserva de Emergência é marcada por {@link #emergencia}. O banco garante no máximo uma por
 * usuário, por índice único filtrado.
 */
@Entity
@Table(name = "reserva")
public class Reserva {

  /** Nome da reserva que o cadastro cria para todo usuário. */
  public static final String NOME_EMERGENCIA = "Reserva de Emergência";

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

  /**
   * Cria uma reserva com saldo zero e um {@code publicId} novo.
   *
   * @param usuario    dono da reserva
   * @param nome       nome de exibição
   * @param metaValor  valor-alvo; zero quando não há meta
   * @param emergencia se é a Reserva de Emergência
   */
  public Reserva(Usuario usuario, String nome, BigDecimal metaValor, boolean emergencia) {
    this.publicId = UUID.randomUUID();
    this.usuario = usuario;
    this.nome = nome;
    this.metaValor = metaValor;
    this.saldoAtual = BigDecimal.ZERO;
    this.emergencia = emergencia;
  }

  /** A Reserva de Emergência que nasce com a conta, com meta zerada. */
  public static Reserva emergenciaDe(Usuario usuario) {
    return new Reserva(usuario, NOME_EMERGENCIA, BigDecimal.ZERO, true);
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

  public BigDecimal getMetaValor() {
    return metaValor;
  }

  public BigDecimal getSaldoAtual() {
    return saldoAtual;
  }

  public boolean isEmergencia() {
    return emergencia;
  }

  public Instant getCriadaEm() {
    return criadaEm;
  }
}
