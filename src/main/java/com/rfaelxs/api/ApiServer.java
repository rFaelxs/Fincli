package com.rfaelxs.api;

import com.rfaelxs.config.GsonConfig;
import com.rfaelxs.model.Reserva;
import com.rfaelxs.model.TipoTransacao;
import com.rfaelxs.model.Transacao;
import com.rfaelxs.model.User;
import com.rfaelxs.repository.IUsuarioRepository;
import com.rfaelxs.repository.SelicRepository;
import com.rfaelxs.service.DashboardService;
import com.rfaelxs.service.ReservaService;
import com.rfaelxs.service.TransacaoService;
import com.rfaelxs.service.UserService;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Servidor HTTP do FinCLI. Expõe os serviços via REST usando Javalin.
 * Ativado quando a variável de ambiente {@code API_MODE=true}.
 */
public class ApiServer {

  private final IUsuarioRepository repository;

  /**
   * Cria o servidor com o repositório fornecido.
   *
   * @param repository repositório de dados de usuário
   */
  public ApiServer(IUsuarioRepository repository) {
    this.repository = repository;
  }

  /**
   * Inicia o servidor Javalin na porta definida por {@code PORT} (padrão: 8080).
   */
  public void start() {
    int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
    Javalin app = Javalin.create().start(port);

    app.get("/health", ctx ->
        ctx.result("{\"status\":\"ok\"}").contentType("application/json"));

    app.post("/api/auth/login", this::login);
    app.post("/api/auth/cadastro", this::cadastro);

    app.get("/api/transacoes/{userId}", this::listarTransacoes);
    app.post("/api/transacoes/{userId}", this::adicionarTransacao);
    app.put("/api/transacoes/{userId}/{id}", this::editarTransacao);
    app.delete("/api/transacoes/{userId}/{id}", this::removerTransacao);

    app.get("/api/reservas/{userId}", this::listarReservas);
    app.post("/api/reservas/{userId}", this::criarReserva);
    app.post("/api/reservas/{userId}/{id}/alocar", this::alocarSaldo);
    app.post("/api/reservas/{userId}/{id}/sacar", this::sacarReserva);
    app.delete("/api/reservas/{userId}/{id}", this::excluirReserva);

    app.get("/api/dashboard/{userId}", this::dashboard);

    app.exception(IllegalArgumentException.class, (e, ctx) -> ctx
        .status(400)
        .result(GsonConfig.GSON.toJson(Map.of("erro", e.getMessage())))
        .contentType("application/json"));

    app.exception(Exception.class, (e, ctx) -> ctx
        .status(500)
        .result(GsonConfig.GSON.toJson(Map.of("erro", e.getMessage())))
        .contentType("application/json"));
  }

  private void login(Context ctx) {
    Map<?, ?> body = GsonConfig.GSON.fromJson(ctx.body(), Map.class);
    User user = new UserService(repository).login((String) body.get("cpf"));
    if (user == null) {
      ctx.status(401)
          .result(GsonConfig.GSON.toJson(Map.of("erro", "CPF não encontrado")))
          .contentType("application/json");
      return;
    }
    ctx.result(GsonConfig.GSON.toJson(user)).contentType("application/json");
  }

  private void cadastro(Context ctx) {
    Map<?, ?> body = GsonConfig.GSON.fromJson(ctx.body(), Map.class);
    User user = new UserService(repository)
        .cadastrar((String) body.get("nome"), (String) body.get("cpf"));
    if (user == null) {
      ctx.status(409)
          .result(GsonConfig.GSON.toJson(Map.of("erro", "CPF já cadastrado")))
          .contentType("application/json");
      return;
    }
    ctx.status(201).result(GsonConfig.GSON.toJson(user)).contentType("application/json");
  }

  private void listarTransacoes(Context ctx) {
    UUID userId = UUID.fromString(ctx.pathParam("userId"));
    List<Transacao> lista = new TransacaoService(repository, userId).listarTransacoes();
    ctx.result(GsonConfig.GSON.toJson(lista)).contentType("application/json");
  }

  private void adicionarTransacao(Context ctx) {
    UUID userId = UUID.fromString(ctx.pathParam("userId"));
    TransacaoDto dto = GsonConfig.GSON.fromJson(ctx.body(), TransacaoDto.class);
    new TransacaoService(repository, userId).adicionarTransacao(
        dto.valor, dto.categoria, dto.descricao,
        LocalDate.parse(dto.data), TipoTransacao.valueOf(dto.tipo), dto.essencial);
    ctx.status(201).result("{\"ok\":true}").contentType("application/json");
  }

  private void editarTransacao(Context ctx) {
    UUID userId = UUID.fromString(ctx.pathParam("userId"));
    UUID id = UUID.fromString(ctx.pathParam("id"));
    TransacaoDto dto = GsonConfig.GSON.fromJson(ctx.body(), TransacaoDto.class);
    new TransacaoService(repository, userId).editarTransacao(
        id, dto.valor, dto.categoria, dto.descricao,
        LocalDate.parse(dto.data), TipoTransacao.valueOf(dto.tipo), dto.essencial);
    ctx.result("{\"ok\":true}").contentType("application/json");
  }

  private void removerTransacao(Context ctx) {
    UUID userId = UUID.fromString(ctx.pathParam("userId"));
    new TransacaoService(repository, userId)
        .removerTransacao(UUID.fromString(ctx.pathParam("id")));
    ctx.status(204);
  }

  private void listarReservas(Context ctx) {
    UUID userId = UUID.fromString(ctx.pathParam("userId"));
    List<Reserva> lista = new ReservaService(repository, userId).listarReservas();
    ctx.result(GsonConfig.GSON.toJson(lista)).contentType("application/json");
  }

  private void criarReserva(Context ctx) {
    UUID userId = UUID.fromString(ctx.pathParam("userId"));
    ReservaDto dto = GsonConfig.GSON.fromJson(ctx.body(), ReservaDto.class);
    Reserva criada = new ReservaService(repository, userId).criarReserva(dto.nome, dto.metaValor);
    ctx.status(201).result(GsonConfig.GSON.toJson(criada)).contentType("application/json");
  }

  private void alocarSaldo(Context ctx) {
    UUID userId = UUID.fromString(ctx.pathParam("userId"));
    Map<?, ?> body = GsonConfig.GSON.fromJson(ctx.body(), Map.class);
    double valor = ((Number) body.get("valor")).doubleValue();
    double saldoDisponivel = ((Number) body.get("saldoDisponivel")).doubleValue();
    new ReservaService(repository, userId)
        .alocarSaldo(ctx.pathParam("id"), valor, saldoDisponivel);
    ctx.result("{\"ok\":true}").contentType("application/json");
  }

  private void sacarReserva(Context ctx) {
    UUID userId = UUID.fromString(ctx.pathParam("userId"));
    Map<?, ?> body = GsonConfig.GSON.fromJson(ctx.body(), Map.class);
    double valor = ((Number) body.get("valor")).doubleValue();
    new ReservaService(repository, userId).sacarReserva(ctx.pathParam("id"), valor);
    ctx.result("{\"ok\":true}").contentType("application/json");
  }

  private void excluirReserva(Context ctx) {
    UUID userId = UUID.fromString(ctx.pathParam("userId"));
    new ReservaService(repository, userId).excluirReserva(ctx.pathParam("id"));
    ctx.status(204);
  }

  private void dashboard(Context ctx) {
    UUID userId = UUID.fromString(ctx.pathParam("userId"));
    TransacaoService ts = new TransacaoService(repository, userId);
    ReservaService rs = new ReservaService(repository, userId);
    DashboardService ds = new DashboardService(ts, rs, new SelicRepository());
    YearMonth mes = YearMonth.now();
    Map<String, Object> response = new HashMap<>();
    response.put("saldo", ds.obterSaldo());
    response.put("totalEntradasMes", ds.totalEntradasMes(mes));
    response.put("totalSaidasMes", ds.totalSaidasMes(mes));
    response.put("progressoEmergencia", ds.progressoEmergencia());
    response.put("selic", ds.obterSelic() != null ? ds.obterSelic() : 0.0);
    response.put("reservas", ds.obterReservas());
    ctx.result(GsonConfig.GSON.toJson(response)).contentType("application/json");
  }

  private static class TransacaoDto {
    double valor;
    String categoria;
    String descricao;
    String data;
    String tipo;
    boolean essencial;
  }

  private static class ReservaDto {
    String nome;
    double metaValor;
  }
}
