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
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Traduz exceções em respostas {@code application/problem+json} (RFC 7807).
 *
 * <p>O front lê sempre o mesmo formato: mostra o {@code detail} e, na validação, as mensagens do
 * objeto {@code campos}. As exceções de domínio entram aqui junto com o módulo que as cria.
 */
@RestControllerAdvice
public class TratadorDeErros {

  private static final Logger log = LoggerFactory.getLogger(TratadorDeErros.class);

  @ExceptionHandler(ValorInvalidoException.class)
  public ProblemDetail valorInvalido(ValorInvalidoException e) {
    return problema(HttpStatus.BAD_REQUEST, "Requisição inválida", e.getMessage());
  }

  @ExceptionHandler(ConflitoException.class)
  public ProblemDetail conflito(ConflitoException e) {
    return problema(HttpStatus.CONFLICT, "Conflito", e.getMessage());
  }

  @ExceptionHandler(RecursoNaoEncontradoException.class)
  public ProblemDetail naoEncontrado(RecursoNaoEncontradoException e) {
    return problema(HttpStatus.NOT_FOUND, "Não encontrado", e.getMessage());
  }

  /**
   * Parâmetro que não converte para o tipo esperado. Sem este handler, o erro cairia na rede de
   * segurança e viraria 500.
   *
   * <p>Um id malformado no caminho, como {@code /api/transacoes/abc}, é só um recurso que não
   * existe: 404. Um parâmetro de consulta malformado, como {@code ?mes=abc}, é erro de quem
   * chamou: 400.
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ProblemDetail parametroMalformado(MethodArgumentTypeMismatchException e) {
    if (e.getParameter().hasParameterAnnotation(PathVariable.class)) {
      return problema(HttpStatus.NOT_FOUND, "Não encontrado", "Recurso não encontrado.");
    }
    return problema(
        HttpStatus.BAD_REQUEST, "Requisição inválida", "Parâmetro inválido: " + e.getName() + ".");
  }

  /**
   * Falha de autenticação no login.
   *
   * <p>A mensagem é a mesma para CPF inexistente e senha errada: distinguir os dois permitiria
   * descobrir quem tem conta testando CPFs (ADR 0007).
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

  /** Corpo ausente, JSON malformado ou campo com tipo errado. */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ProblemDetail corpoIlegivel(HttpMessageNotReadableException e) {
    return problema(
        HttpStatus.BAD_REQUEST, "Requisição inválida", "O corpo da requisição não pôde ser lido.");
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ProblemDetail metodoNaoSuportado(HttpRequestMethodNotSupportedException e) {
    return problema(
        HttpStatus.METHOD_NOT_ALLOWED, "Método não permitido", "Método não suportado nesta rota.");
  }

  /**
   * Rota ou arquivo que não existe.
   *
   * <p>Sem este handler a exceção cai na rede de segurança abaixo e vira 500.
   */
  @ExceptionHandler(NoResourceFoundException.class)
  public ProblemDetail recursoAusente(NoResourceFoundException e) {
    return problema(HttpStatus.NOT_FOUND, "Não encontrado", "Recurso não encontrado.");
  }

  /**
   * Rede de segurança. Registra a causa no log e devolve uma mensagem genérica: a exceção
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

  static ProblemDetail problema(HttpStatus status, String titulo, String detalhe) {
    ProblemDetail problema = ProblemDetail.forStatus(status);
    problema.setTitle(titulo);
    problema.setDetail(detalhe);
    return problema;
  }
}
