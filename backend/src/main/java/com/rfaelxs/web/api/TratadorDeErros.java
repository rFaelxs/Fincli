package com.rfaelxs.web.api;

import com.rfaelxs.web.service.ConflitoException;
import com.rfaelxs.web.service.RecursoNaoEncontradoException;
import com.rfaelxs.web.service.ValorInvalidoException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduz exceções de domínio em respostas {@code application/problem+json} (RFC 7807).
 *
 * <p>O SPA precisa de um corpo de erro previsível para exibir mensagem ao usuário
 * (escopo-web.md §2.2).
 */
@RestControllerAdvice
public class TratadorDeErros {

  private static final Logger log = LoggerFactory.getLogger(TratadorDeErros.class);

  @ExceptionHandler(ValorInvalidoException.class)
  public ProblemDetail valorInvalido(ValorInvalidoException e) {
    return problema(HttpStatus.BAD_REQUEST, "Requisição inválida", e.getMessage());
  }

  @ExceptionHandler(RecursoNaoEncontradoException.class)
  public ProblemDetail naoEncontrado(RecursoNaoEncontradoException e) {
    return problema(HttpStatus.NOT_FOUND, "Não encontrado", e.getMessage());
  }

  @ExceptionHandler(ConflitoException.class)
  public ProblemDetail conflito(ConflitoException e) {
    return problema(HttpStatus.CONFLICT, "Conflito", e.getMessage());
  }

  /**
   * Falha de autenticação.
   *
   * <p>A mensagem é sempre a mesma para CPF inexistente e senha errada — distinguir os dois
   * permitiria descobrir quem tem conta testando CPFs (escopo-web.md §1.1).
   */
  @ExceptionHandler(AuthenticationException.class)
  public ProblemDetail naoAutenticado(AuthenticationException e) {
    return problema(HttpStatus.UNAUTHORIZED, "Não autorizado", "CPF ou senha inválidos.");
  }

  /** Erros de anotação de validação, campo a campo. */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail validacao(MethodArgumentNotValidException e) {
    Map<String, String> campos = new LinkedHashMap<>();
    e.getBindingResult().getFieldErrors()
        .forEach(erro -> campos.putIfAbsent(erro.getField(), erro.getDefaultMessage()));

    ProblemDetail detalhe =
        problema(HttpStatus.BAD_REQUEST, "Requisição inválida", "Verifique os campos enviados.");
    detalhe.setProperty("campos", campos);
    return detalhe;
  }

  /**
   * Rede de segurança. Registra a causa no log e devolve uma mensagem genérica — a exceção
   * original pode conter dado do usuário e não deve vazar na resposta.
   */
  @ExceptionHandler(Exception.class)
  public ProblemDetail inesperado(Exception e) {
    log.error("Erro não tratado", e);
    return problema(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Erro interno",
        "Não foi possível concluir a operação. Tente novamente.");
  }

  private ProblemDetail problema(HttpStatus status, String titulo, String detalhe) {
    ProblemDetail problema = ProblemDetail.forStatus(status);
    problema.setTitle(titulo);
    problema.setDetail(detalhe);
    return problema;
  }
}
