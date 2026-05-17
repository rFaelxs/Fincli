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
  void deveObterTaxaSelicDaApiDoBancoCentral() {
    SelicRepository repository = new SelicRepository();

    Double taxa = repository.obterTaxaAtual();

    assumeTrue(taxa != null, "API do BCB indisponível — teste ignorado");
    assertTrue(taxa > 0, "A taxa Selic deve ser um valor positivo");
  }
}
