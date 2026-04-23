package com.rfaelxs.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.rfaelxs.exception.ValorInvalidoException;
import com.rfaelxs.model.TipoTransacao;
import com.rfaelxs.model.Transacao;
import com.rfaelxs.repository.GastoRepository;

/**
 * Contém a lógica de negócio para gerenciamento de transações financeiras.
 * Mantém a lista de transações em memória e delega a persistência ao {@link GastoRepository}.
 */
public class GastoService {

    private final GastoRepository repository;
    private final List<Transacao> transacoes;

    /**
     * Inicializa o serviço carregando as transações existentes do repositório.
     *
     * @param repository repositório responsável pela leitura e escrita em arquivo
     */
    public GastoService(GastoRepository repository) {
        this.repository = repository;
        this.transacoes = repository.carregarTodos();
    }

    /**
     * Cria e adiciona uma nova transação à lista, persistindo em seguida.
     *
     * @param valor     valor monetário da transação (deve ser >= 0)
     * @param categoria categoria da transação (ex: "Alimentação")
     * @param descricao descrição livre da transação
     * @param data      data em que a transação ocorreu
     * @param tipo      {@link TipoTransacao#ENTRADA} ou {@link TipoTransacao#SAIDA}
     * @param essencial {@code true} se a transação é considerada essencial
     * @throws ValorInvalidoException se o valor for negativo
     */
    public void adicionarTransacao(double valor, String categoria, String descricao, LocalDate data, TipoTransacao tipo, boolean essencial) {
        if (valor >= 0) {
            Transacao transacao = new Transacao(valor, categoria, descricao, data, tipo, essencial);
            transacoes.add(transacao);
            repository.salvarTransacoes(transacoes);
        } else {
            throw new ValorInvalidoException("O valor inserido é inválido!");
        }
    }

    /**
     * Retorna todas as transações carregadas em memória.
     *
     * @return lista de transações do usuário
     */
    public List<Transacao> listarTransacoes() {
        return transacoes;
    }

    /**
     * Remove a transação com o ID informado e persiste a lista atualizada.
     *
     * @param id UUID da transação a ser removida
     */
    public void removerTransacao(UUID id) {
        this.transacoes.removeIf(t -> t.getId().equals(id));
        this.repository.salvarTransacoes(transacoes);
    }

    /**
     * Agrupa as transações por categoria e soma os valores de cada grupo.
     *
     * @return mapa onde a chave é o nome da categoria e o valor é o total acumulado
     */
    public Map<String, Double> resumoPorCategoria() {
        return transacoes.stream().collect(
            Collectors.groupingBy(Transacao::getCategoria, Collectors.summingDouble(Transacao::getValorTransacao))
        );
    }
}
