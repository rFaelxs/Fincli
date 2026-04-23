package com.rfaelxs;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.rfaelxs.exception.ValorInvalidoException;
import com.rfaelxs.model.TipoTransacao;
import com.rfaelxs.model.Transacao;
import com.rfaelxs.repository.GastoRepository;
import com.rfaelxs.service.GastoService;

class AppTest {

    @Test
    void testePlaceholder() {
        assertTrue(true);
    }

    static class GastoRepositoryFake extends GastoRepository {

        private List<Transacao> transacoesSalvas = new ArrayList<>();

        @Override
        public void salvarTransacoes(List<Transacao> transacoes) {
            this.transacoesSalvas = transacoes;
        }

        @Override
        public List<Transacao> carregarTodos() {
            return transacoesSalvas;
        }
    }

    @Test
    void deveAdicionarTransacaoValida() throws ValorInvalidoException {
        GastoRepository fake = new GastoRepositoryFake();
        GastoService service = new GastoService(fake);

        service.adicionarTransacao(100.0, "Alimentação", "Mercado", LocalDate.of(2025, 1, 10), TipoTransacao.SAIDA, true);

        assertEquals(1, service.listarTransacoes().size());
    }

    @Test
    void deveRemoverTransacaoPorId() throws ValorInvalidoException {
        GastoRepository fake = new GastoRepositoryFake();
        GastoService service = new GastoService(fake);

        service.adicionarTransacao(100.0, "Alimentação", "Mercado", LocalDate.of(2025, 1, 10), TipoTransacao.SAIDA, true);

        UUID id = service.listarTransacoes().get(0).getId();
        service.removerTransacao(id);

        assertEquals(0, service.listarTransacoes().size());
    }

    @Test
    void naoDeveAdicionarTransacaoComValorNegativo() {
        GastoRepository fake = new GastoRepositoryFake();
        GastoService service = new GastoService(fake);

        assertThrows(ValorInvalidoException.class, () ->
            service.adicionarTransacao(-1.0, "Alimentação", "Mercado", LocalDate.of(2025, 1, 10), TipoTransacao.SAIDA, false)
        );
    }
}
