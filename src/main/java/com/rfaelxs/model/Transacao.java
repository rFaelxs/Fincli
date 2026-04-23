package com.rfaelxs.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Representa uma transação financeira do usuário.
 * Pode ser uma entrada (receita) ou saída (despesa),
 * com categoria, descrição, data e flag de essencialidade.
 * O ID é gerado automaticamente via UUID.
 */
public class Transacao {

    UUID id;
    double valorTransacao;
    String categoria;
    String descTransacao;
    LocalDate dataTransacao;
    TipoTransacao tipo;
    boolean essencial;

    /**
     * Cria uma nova transação com UUID gerado automaticamente.
     *
     * @param valorTransacao valor monetário da transação (deve ser >= 0)
     * @param categoria      categoria da transação (ex: "Alimentação")
     * @param descTransacao  descrição livre da transação
     * @param dataTransacao  data em que a transação ocorreu
     * @param tipo           {@link TipoTransacao#ENTRADA} ou {@link TipoTransacao#SAIDA}
     * @param essencial      {@code true} se a transação é considerada essencial
     */
    public Transacao(double valorTransacao, String categoria, String descTransacao, LocalDate dataTransacao, TipoTransacao tipo, boolean essencial) {
        this.id = UUID.randomUUID();
        this.valorTransacao = valorTransacao;
        this.categoria = categoria;
        this.descTransacao = descTransacao;
        this.dataTransacao = dataTransacao;
        this.tipo = tipo;
        this.essencial = essencial;
    }

    /** @return UUID único da transação */
    public UUID getId() {
        return id;
    }

    /** @return valor monetário da transação */
    public double getValorTransacao() {
        return valorTransacao;
    }

    /** @param valorTransacao novo valor monetário */
    public void setValorTransacao(double valorTransacao) {
        this.valorTransacao = valorTransacao;
    }

    /** @return categoria da transação */
    public String getCategoria() {
        return categoria;
    }

    /** @param categoria nova categoria */
    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    /** @return data da transação */
    public LocalDate getDataTransacao() {
        return dataTransacao;
    }

    /** @param dataTransacao nova data */
    public void setDataTransacao(LocalDate dataTransacao) {
        this.dataTransacao = dataTransacao;
    }

    /** @return descrição da transação */
    public String getDescTransacao() {
        return descTransacao;
    }

    /** @param descTransacao nova descrição */
    public void setDescTransacao(String descTransacao) {
        this.descTransacao = descTransacao;
    }

    /** @return tipo da transação (ENTRADA ou SAIDA) */
    public TipoTransacao getTipo() {
        return tipo;
    }

    /** @param tipo novo tipo da transação */
    public void setTipo(TipoTransacao tipo) {
        this.tipo = tipo;
    }

    /** @return {@code true} se a transação é essencial */
    public boolean isEssencial() {
        return essencial;
    }

    /** @param essencial {@code true} para marcar como essencial */
    public void setEssencial(boolean essencial) {
        this.essencial = essencial;
    }

    @Override
    public String toString() {
        String dataFormatada = dataTransacao.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        return "\n┌─────────────────────────────────────────" +
               "\n│ ID:        " + id +
               "\n│ Tipo:      " + tipo +
               "\n│ Valor:     R$ " + String.format("%.2f", valorTransacao) +
               "\n│ Categoria: " + categoria +
               "\n│ Descrição: " + descTransacao +
               "\n│ Data:      " + dataFormatada +
               "\n│ Essencial: " + (essencial ? "Sim" : "Não") +
               "\n└─────────────────────────────────────────";
    }
}
