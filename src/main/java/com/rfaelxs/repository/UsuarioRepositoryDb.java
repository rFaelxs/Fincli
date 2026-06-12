package com.rfaelxs.repository;

import com.rfaelxs.config.DatabaseConfig;
import com.rfaelxs.model.DadosUsuario;
import com.rfaelxs.model.MovimentacaoReserva;
import com.rfaelxs.model.Reserva;
import com.rfaelxs.model.TipoMovimentacaoReserva;
import com.rfaelxs.model.TipoTransacao;
import com.rfaelxs.model.Transacao;
import com.rfaelxs.model.User;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implementação de {@link IUsuarioRepository} com persistência em PostgreSQL (Supabase).
 * O método {@code salvarDados} executa delete-then-insert dentro de uma transação.
 */
public class UsuarioRepositoryDb implements IUsuarioRepository {

  @Override
  public boolean existeCpf(String cpf) {
    String sql = "SELECT COUNT(*) FROM usuarios WHERE cpf_usuario = ?";
    try (Connection conn = DatabaseConfig.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, cpf);
      ResultSet rs = ps.executeQuery();
      return rs.next() && rs.getInt(1) > 0;
    } catch (SQLException e) {
      throw new RuntimeException("Erro ao verificar CPF", e);
    }
  }

  @Override
  public void salvarPerfil(User usuario) {
    String sql = "INSERT INTO usuarios (id_usuario, nm_usuario, cpf_usuario) "
        + "VALUES (?, ?, ?) ON CONFLICT (id_usuario) DO UPDATE "
        + "SET nm_usuario = EXCLUDED.nm_usuario, cpf_usuario = EXCLUDED.cpf_usuario";
    try (Connection conn = DatabaseConfig.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, usuario.getIdUsuario().toString());
      ps.setString(2, usuario.getNmUsuario());
      ps.setString(3, usuario.getCpfUsuario());
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Erro ao salvar perfil", e);
    }
  }

  @Override
  public User buscarPorCpf(String cpf) {
    String sql = "SELECT id_usuario, nm_usuario, cpf_usuario "
        + "FROM usuarios WHERE cpf_usuario = ?";
    try (Connection conn = DatabaseConfig.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, cpf);
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        return new User(
            rs.getString("cpf_usuario"),
            UUID.fromString(rs.getString("id_usuario")),
            rs.getString("nm_usuario"));
      }
      return null;
    } catch (SQLException e) {
      throw new RuntimeException("Erro ao buscar usuário por CPF", e);
    }
  }

  @Override
  public DadosUsuario carregarDados(UUID idUsuario) {
    User perfil = buscarPorId(idUsuario);
    if (perfil == null) {
      return null;
    }
    DadosUsuario dados = new DadosUsuario(perfil);
    dados.getTransacoes().addAll(carregarTransacoes(idUsuario));
    dados.getReservas().addAll(carregarReservas(idUsuario));
    dados.getMovimentacoesReserva().addAll(carregarMovimentacoes(idUsuario));
    return dados;
  }

  @Override
  public void salvarDados(UUID idUsuario, DadosUsuario dados) {
    try (Connection conn = DatabaseConfig.getConnection()) {
      conn.setAutoCommit(false);
      try {
        excluirDadosUsuario(conn, idUsuario);
        inserirTransacoes(conn, idUsuario, dados.getTransacoes());
        inserirReservas(conn, idUsuario, dados.getReservas());
        inserirMovimentacoes(conn, idUsuario, dados.getMovimentacoesReserva());
        conn.commit();
      } catch (SQLException e) {
        conn.rollback();
        throw new RuntimeException("Erro ao salvar dados do usuário", e);
      }
    } catch (SQLException e) {
      throw new RuntimeException("Erro de conexão ao salvar dados", e);
    }
  }

  private User buscarPorId(UUID idUsuario) {
    String sql = "SELECT id_usuario, nm_usuario, cpf_usuario "
        + "FROM usuarios WHERE id_usuario = ?";
    try (Connection conn = DatabaseConfig.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, idUsuario.toString());
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        return new User(
            rs.getString("cpf_usuario"),
            UUID.fromString(rs.getString("id_usuario")),
            rs.getString("nm_usuario"));
      }
      return null;
    } catch (SQLException e) {
      throw new RuntimeException("Erro ao buscar usuário por ID", e);
    }
  }

  private List<Transacao> carregarTransacoes(UUID idUsuario) {
    String sql = "SELECT id, valor_transacao, categoria, desc_transacao, "
        + "data_transacao, tipo, essencial FROM transacoes WHERE id_usuario = ?";
    List<Transacao> lista = new ArrayList<>();
    try (Connection conn = DatabaseConfig.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, idUsuario.toString());
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        Transacao t = new Transacao();
        t.setId(UUID.fromString(rs.getString("id")));
        t.setValorTransacao(rs.getDouble("valor_transacao"));
        t.setCategoria(rs.getString("categoria"));
        t.setDescTransacao(rs.getString("desc_transacao"));
        t.setDataTransacao(rs.getDate("data_transacao").toLocalDate());
        t.setTipo(TipoTransacao.valueOf(rs.getString("tipo")));
        t.setEssencial(rs.getBoolean("essencial"));
        lista.add(t);
      }
    } catch (SQLException e) {
      throw new RuntimeException("Erro ao carregar transações", e);
    }
    return lista;
  }

  private List<Reserva> carregarReservas(UUID idUsuario) {
    String sql = "SELECT id, nome, meta_valor, saldo_atual, emergencia "
        + "FROM reservas WHERE id_usuario = ?";
    List<Reserva> lista = new ArrayList<>();
    try (Connection conn = DatabaseConfig.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, idUsuario.toString());
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        Reserva r = new Reserva(
            rs.getString("id"),
            rs.getString("nome"),
            rs.getDouble("meta_valor"),
            rs.getBoolean("emergencia"));
        r.setSaldoAtual(rs.getDouble("saldo_atual"));
        lista.add(r);
      }
    } catch (SQLException e) {
      throw new RuntimeException("Erro ao carregar reservas", e);
    }
    return lista;
  }

  private List<MovimentacaoReserva> carregarMovimentacoes(UUID idUsuario) {
    String sql = "SELECT id, id_reserva, nome_reserva, valor, tipo, data "
        + "FROM movimentacoes_reserva WHERE id_usuario = ?";
    List<MovimentacaoReserva> lista = new ArrayList<>();
    try (Connection conn = DatabaseConfig.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, idUsuario.toString());
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        MovimentacaoReserva m = new MovimentacaoReserva();
        m.setId(UUID.fromString(rs.getString("id")));
        m.setIdReserva(rs.getString("id_reserva"));
        m.setNomeReserva(rs.getString("nome_reserva"));
        m.setValor(rs.getDouble("valor"));
        m.setTipo(TipoMovimentacaoReserva.valueOf(rs.getString("tipo")));
        m.setData(rs.getDate("data").toLocalDate());
        lista.add(m);
      }
    } catch (SQLException e) {
      throw new RuntimeException("Erro ao carregar movimentações", e);
    }
    return lista;
  }

  private void excluirDadosUsuario(Connection conn, UUID idUsuario) throws SQLException {
    String[] sqls = {
        "DELETE FROM movimentacoes_reserva WHERE id_usuario = ?",
        "DELETE FROM transacoes WHERE id_usuario = ?",
        "DELETE FROM reservas WHERE id_usuario = ?"
    };
    for (String sql : sqls) {
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, idUsuario.toString());
        ps.executeUpdate();
      }
    }
  }

  private void inserirTransacoes(Connection conn, UUID idUsuario, List<Transacao> transacoes)
      throws SQLException {
    if (transacoes.isEmpty()) {
      return;
    }
    String sql = "INSERT INTO transacoes (id, id_usuario, valor_transacao, categoria, "
        + "desc_transacao, data_transacao, tipo, essencial) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      for (Transacao t : transacoes) {
        ps.setString(1, t.getId().toString());
        ps.setString(2, idUsuario.toString());
        ps.setDouble(3, t.getValorTransacao());
        ps.setString(4, t.getCategoria());
        ps.setString(5, t.getDescTransacao());
        ps.setDate(6, Date.valueOf(t.getDataTransacao()));
        ps.setString(7, t.getTipo().name());
        ps.setBoolean(8, t.isEssencial());
        ps.addBatch();
      }
      ps.executeBatch();
    }
  }

  private void inserirReservas(Connection conn, UUID idUsuario, List<Reserva> reservas)
      throws SQLException {
    if (reservas.isEmpty()) {
      return;
    }
    String sql = "INSERT INTO reservas (id, id_usuario, nome, meta_valor, saldo_atual, "
        + "emergencia) VALUES (?, ?, ?, ?, ?, ?)";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      for (Reserva r : reservas) {
        ps.setString(1, r.getId());
        ps.setString(2, idUsuario.toString());
        ps.setString(3, r.getNome());
        ps.setDouble(4, r.getMetaValor());
        ps.setDouble(5, r.getSaldoAtual());
        ps.setBoolean(6, r.isEmergencia());
        ps.addBatch();
      }
      ps.executeBatch();
    }
  }

  private void inserirMovimentacoes(Connection conn, UUID idUsuario,
      List<MovimentacaoReserva> movimentacoes) throws SQLException {
    if (movimentacoes.isEmpty()) {
      return;
    }
    String sql = "INSERT INTO movimentacoes_reserva "
        + "(id, id_usuario, id_reserva, nome_reserva, valor, tipo, data) "
        + "VALUES (?, ?, ?, ?, ?, ?, ?)";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      for (MovimentacaoReserva m : movimentacoes) {
        ps.setString(1, m.getId().toString());
        ps.setString(2, idUsuario.toString());
        ps.setString(3, m.getIdReserva());
        ps.setString(4, m.getNomeReserva());
        ps.setDouble(5, m.getValor());
        ps.setString(6, m.getTipo().name());
        ps.setDate(7, Date.valueOf(m.getData()));
        ps.addBatch();
      }
      ps.executeBatch();
    }
  }
}
