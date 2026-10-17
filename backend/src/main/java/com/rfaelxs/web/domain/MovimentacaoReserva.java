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

/** Alocação ou saque numa reserva. Aparece no extrato e nos lançamentos da Gestão. */
@Entity
@Table(name = "movimentacao_reserva")
public class MovimentacaoReserva {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "usuario_id", nullable = false, updatable = false)
  private Usuario usuario;

  /** Nula depois que a reserva é excluída; o banco faz isso pela chave estrangeira. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reserva_id", updatable = false)
  private Reserva reserva;

  /** Nome vigente na época do movimento: o extrato não muda se a reserva for renomeada. */
  @Nationalized
  @Column(name = "reserva_nome", nullable = false, length = 120, updatable = false)
  private String reservaNome;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal valor;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10, updatable = false)
  private TipoMovimentacaoReserva tipo;

  @Column(name = "data_movimento", nullable = false, updatable = false)
  private LocalDate data;

  protected MovimentacaoReserva() {
    // exigido pelo JPA
  }

  /** Registra um movimento, gravando o nome atual da reserva. */
  public MovimentacaoReserva(Usuario usuario, Reserva reserva, BigDecimal valor,
      TipoMovimentacaoReserva tipo, LocalDate data) {
    this.usuario = usuario;
    this.reserva = reserva;
    this.reservaNome = reserva.getNome();
    this.valor = valor;
    this.tipo = tipo;
    this.data = data;
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

  public LocalDate getData() {
    return data;
  }
}
