package com.rfaelxs.repository;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Test;

/**
 * Teste de integração: valida a comunicação real com a API do Banco Central.
 * Se a rede estiver indisponível no ambiente de execução, o teste é ignorado (assumeTrue).
 */
class SelicRepositoryIntegrationTest {

  @Test
  void deveObterMetaSelicAnualDaApiDoBancoCentral() {
    SelicRepository repository = new SelicRepository();

    Double taxa = repository.obterTaxaAtual();

    assumeTrue(taxa != null, "API do BCB indisponível — teste ignorado");
    assertTrue(taxa > 0, "A meta Selic deve ser um valor positivo");
    // Faixa de sanidade: garante que a série consultada é a meta anual (% a.a.) e não a
    // Selic efetiva diária (SGS 11), cujo valor fica na casa de 0,05% ao dia.
    assertTrue(taxa >= 1.0 && taxa <= 50.0,
        "A meta Selic deve estar em % ao ano; valor recebido: " + taxa);
  }
}
