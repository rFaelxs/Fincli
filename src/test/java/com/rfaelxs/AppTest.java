package com.rfaelxs;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.rfaelxs.model.Gasto;
import com.rfaelxs.repository.GastoRepository;
import com.rfaelxs.service.GastoService;

class AppTest {

    @Test
    void testePlaceholder() {
        assertTrue(true);
    }

    static class GastoRepositoryFake extends GastoRepository {

        private List<Gasto> gastosSalvos = new ArrayList<>();

        @Override
        public void salvarGasto(List<Gasto> gastos) {
            this.gastosSalvos = gastos;
        }

        @Override
        public List<Gasto> carregarTodos() {
            return gastosSalvos;
        }

    }

    @Test
    void deveAdicionarGastoValido() {
        GastoRepository fake = new GastoRepositoryFake();
        GastoService service = new GastoService(fake);

        service.adicionarGasto(100.0,
                "Alimentação",
                "Mercado",
                LocalDate.of(2025, 1, 10));

        assertEquals(1, service.listarGastos().size());
    }

    @Test
    void deveRemoverGastoPorId() {
        GastoRepository fake = new GastoRepositoryFake();
        GastoService service = new GastoService(fake);

        service.adicionarGasto(100.0, "Alimentação", "Mercado", LocalDate.of(2025, 1, 10));

        UUID id = service.listarGastos().get(0).getId();
        service.removerGasto(id);

        assertEquals(0, service.listarGastos().size());

    }

    @Test
    void naoDeveAdicionarGastoComValorNegativo() {
        GastoRepository fake = new GastoRepositoryFake();
        GastoService service = new GastoService(fake);

        service.adicionarGasto(-1.0, "Alimentação", "Mercado", LocalDate.of(2025, 1, 10));

        assertEquals(0, service.listarGastos().size());
    }
}