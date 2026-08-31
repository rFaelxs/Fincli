package com.rfaelxs.web.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Nationalized;

/**
 * Usuário da aplicação. O login é feito por CPF + senha (escopo-web.md §1.1).
 *
 * <p>{@code id} é interno e nunca sai da camada de persistência; a API expõe {@link #publicId}.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "public_id", nullable = false, updatable = false)
  private UUID publicId;

  @Nationalized
  @Column(nullable = false, length = 120)
  private String nome;

  /** Somente dígitos, sem máscara. Índice único no banco. */
  @Column(nullable = false, length = 11)
  private String cpf;

  /**
   * Opcional, usado exclusivamente para redefinição de senha. Nulo enquanto a decisão sobre
   * recuperação de senha não for fechada (escopo-web.md §1.1).
   */
  @Nationalized
  @Column(length = 254)
  private String email;

  @Column(name = "senha_hash", nullable = false, length = 72)
  private String senhaHash;

  @Column(name = "criado_em", nullable = false, insertable = false, updatable = false)
  private Instant criadoEm;

  protected Usuario() {
    // exigido pelo JPA
  }

  public Usuario(String nome, String cpf, String senhaHash) {
    this.publicId = UUID.randomUUID();
    this.nome = nome;
    this.cpf = cpf;
    this.senhaHash = senhaHash;
  }

  public Long getId() {
    return id;
  }

  public UUID getPublicId() {
    return publicId;
  }

  public String getNome() {
    return nome;
  }

  public String getCpf() {
    return cpf;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getSenhaHash() {
    return senhaHash;
  }

  public void setSenhaHash(String senhaHash) {
    this.senhaHash = senhaHash;
  }

  public Instant getCriadoEm() {
    return criadoEm;
  }
}
