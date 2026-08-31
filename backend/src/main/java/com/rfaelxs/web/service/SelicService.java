package com.rfaelxs.web.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Meta Selic vigente, via API pública do Banco Central.
 *
 * <p>Usa a série SGS 432 (meta definida pelo Copom, em % ao ano). A SGS 11, usada antes da
 * correção, é a Selic efetiva <em>diária</em> — exibi-la como "taxa Selic atual" mostrava
 * 0,05% no lugar de 14% (docs/spec-selic.md).
 *
 * <p>O CLI consultava uma vez por sessão. Aqui o cache é da aplicação inteira, com TTL: a meta
 * muda a cada ~45 dias, então uma chamada por sessão de usuário seria desperdício
 * (escopo-web.md §5).
 */
@Service
public class SelicService {

  private static final Logger log = LoggerFactory.getLogger(SelicService.class);

  private final RestClient restClient;
  private final String url;
  private final Duration ttl;
  private final AtomicReference<Cache> cache = new AtomicReference<>(null);

  public SelicService(
      @Value("${fincli.selic.url}") String url,
      @Value("${fincli.selic.ttl}") Duration ttl,
      @Value("${fincli.selic.timeout}") Duration timeout) {
    this.url = url;
    this.ttl = ttl;

    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout((int) timeout.toMillis());
    factory.setReadTimeout((int) timeout.toMillis());
    this.restClient = RestClient.builder().requestFactory(factory).build();
  }

  /**
   * Retorna a meta Selic anual vigente.
   *
   * <p>Nunca lança: falha de rede devolve {@code null} e o dashboard exibe "Indisponível", como
   * no CLI. Se houver valor em cache, mesmo vencido, ele é preferido a devolver nada.
   *
   * @return meta Selic em % ao ano, ou {@code null} se a API estiver indisponível
   */
  public BigDecimal obterMetaAnual() {
    Cache atual = cache.get();
    if (atual != null && atual.fresco(ttl)) {
      return atual.valor();
    }

    try {
      JsonNode resposta = restClient.get().uri(url).retrieve().body(JsonNode.class);
      if (resposta == null || !resposta.isArray() || resposta.isEmpty()) {
        return valorVencidoOuNulo(atual);
      }
      BigDecimal valor = new BigDecimal(resposta.get(0).get("valor").asText());
      cache.set(new Cache(valor, Instant.now()));
      return valor;
    } catch (RuntimeException e) {
      log.warn("Consulta da meta Selic ao BCB falhou: {}", e.getMessage());
      return valorVencidoOuNulo(atual);
    }
  }

  /** Serve o último valor conhecido mesmo vencido — melhor que "Indisponível" na tela. */
  private BigDecimal valorVencidoOuNulo(Cache atual) {
    return atual != null ? atual.valor() : null;
  }

  private record Cache(BigDecimal valor, Instant obtidoEm) {
    boolean fresco(Duration ttl) {
      return Instant.now().isBefore(obtidoEm.plus(ttl));
    }
  }
}
