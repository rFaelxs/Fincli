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
import org.hibernate.annotations.Nationalized;

/** Registro de alocação ou saque em uma reserva, exibido no extrato. */
@Entity
@Table(name = "movimentacao_reserva")
public class MovimentacaoReserva {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id", nullable = false)
  private Usuario usuario;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "reserva_id", nullable = false)
  private Reserva reserva;

  /** Nome vigente na época do movimento — o extrato não deve mudar se a reserva for renomeada. */
  @Nationalized
  @Column(name = "reserva_nome", nullable = false, length = 120)
  private String reservaNome;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal valor;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private TipoMovimentacaoReserva tipo;

  @Column(name = "data_movimento", nullable = false)
  private LocalDate dataMovimento;

  protected MovimentacaoReserva() {
    // exigido pelo JPA
  }

  public MovimentacaoReserva(
      Usuario usuario,
      Reserva reserva,
      BigDecimal valor,
      TipoMovimentacaoReserva tipo,
      LocalDate dataMovimento) {
    this.usuario = usuario;
    this.reserva = reserva;
    this.reservaNome = reserva.getNome();
    this.valor = valor;
    this.tipo = tipo;
    this.dataMovimento = dataMovimento;
  }

  public Long getId() {
    return id;
  }

  public Usuario getUsuario() {
    return usuario;
  }

  public Reserva getReserva() {
    return reserva;
  }

  public String getReservaNome() {
    return reservaNome;
  }

  public BigDecimal getValor() {
    return valor;
  }

  public TipoMovimentacaoReserva getTipo() {
    return tipo;
  }

  public LocalDate getDataMovimento() {
    return dataMovimento;
  }
}
