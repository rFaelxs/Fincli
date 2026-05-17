package com.rfaelxs.repository;

import com.google.gson.reflect.TypeToken;
import com.rfaelxs.config.GsonConfig;
import com.rfaelxs.model.DadosUsuario;
import com.rfaelxs.model.User;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Gerencia a persistência dos dados de cada usuário.
 * Cada usuário tem seu arquivo em {@code data/{uuid}.json}.
 * O índice de perfis (para login por CPF) fica em {@code data/perfis.json}.
 */
public class UsuarioRepository {

  private static final String DIR_DADOS = "data";
  private static final String ARQUIVO_PERFIS = DIR_DADOS + File.separator + "perfis.json";

  public UsuarioRepository() {
    new File(DIR_DADOS).mkdirs();
  }

  /**
   * Verifica se já existe um usuário com o CPF informado.
   *
   * @param cpf CPF a verificar
   * @return {@code true} se o CPF já está cadastrado
   */
  public boolean existeCpf(String cpf) {
    return carregarTodosPerfis().stream()
        .anyMatch(u -> u.getCpfUsuario().equals(cpf));
  }

  /**
   * Salva ou atualiza o perfil no índice {@code perfis.json}.
   *
   * @param usuario usuário a persistir
   */
  public void salvarPerfil(User usuario) {
    List<User> perfis = carregarTodosPerfis();
    perfis.removeIf(u -> u.getIdUsuario().equals(usuario.getIdUsuario()));
    perfis.add(usuario);
    escreverArquivo(ARQUIVO_PERFIS, GsonConfig.GSON.toJson(perfis));
  }

  /**
   * Busca um usuário pelo CPF no índice de perfis.
   *
   * @param cpf CPF de login
   * @return o {@link User} encontrado ou {@code null} se não existir
   */
  public User buscarPorCpf(String cpf) {
    return carregarTodosPerfis().stream()
        .filter(u -> u.getCpfUsuario().equals(cpf))
        .findFirst()
        .orElse(null);
  }

  /**
   * Carrega os dados completos de um usuário a partir do seu arquivo individual.
   *
   * @param idUsuario UUID do usuário
   * @return {@link DadosUsuario} ou {@code null} se o arquivo não existir
   */
  public DadosUsuario carregarDados(UUID idUsuario) {
    File arquivo = arquivoUsuario(idUsuario);
    if (!arquivo.exists()) {
      return null;
    }
    try (FileReader reader = new FileReader(arquivo)) {
      return GsonConfig.GSON.fromJson(reader, DadosUsuario.class);
    } catch (IOException e) {
      throw new RuntimeException("Erro ao carregar dados do usuário " + idUsuario, e);
    }
  }

  /**
   * Persiste os dados completos de um usuário em {@code data/{uuid}.json}.
   *
   * @param idUsuario UUID do usuário
   * @param dados     dados a persistir
   */
  public void salvarDados(UUID idUsuario, DadosUsuario dados) {
    escreverArquivo(arquivoUsuario(idUsuario).getPath(), GsonConfig.GSON.toJson(dados));
  }

  private List<User> carregarTodosPerfis() {
    File arquivo = new File(ARQUIVO_PERFIS);
    if (!arquivo.exists()) {
      return new ArrayList<>();
    }
    try (FileReader reader = new FileReader(arquivo)) {
      Type tipo = new TypeToken<List<User>>() {}.getType();
      List<User> perfis = GsonConfig.GSON.fromJson(reader, tipo);
      return perfis != null ? perfis : new ArrayList<>();
    } catch (IOException e) {
      throw new RuntimeException("Erro ao carregar perfis", e);
    }
  }

  private File arquivoUsuario(UUID idUsuario) {
    return new File(DIR_DADOS + File.separator + idUsuario + ".json");
  }

  private void escreverArquivo(String caminho, String conteudo) {
    try (FileWriter writer = new FileWriter(caminho)) {
      writer.write(conteudo);
    } catch (IOException e) {
      throw new RuntimeException("Erro ao salvar arquivo: " + caminho, e);
    }
  }
}
