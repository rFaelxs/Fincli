package com.rfaelxs.repository;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.rfaelxs.model.User;

/**
 * Responsável por persistir e recuperar usuários em arquivo JSON.
 * Utiliza Gson com adaptador customizado para serializar {@link LocalDate}.
 */
public class UserRepository {

    private static final String ARQUIVOUSER = "user.json";
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new TypeAdapter<LocalDate>() {
                @Override
                public void write(JsonWriter out, LocalDate value) throws IOException {
                    out.value(value.toString());
                }

                @Override
                public LocalDate read(JsonReader in) throws IOException {
                    return LocalDate.parse(in.nextString());
                }
            })
            .create();

    /**
     * Serializa a lista de usuários e grava no arquivo JSON.
     *
     * @param usuarios lista completa de usuários a ser persistida
     */
    public void salvarUsuarios(List<User> usuarios) {
        String json = gson.toJson(usuarios);
        try (FileWriter write = new FileWriter(ARQUIVOUSER)) {
            write.write(json);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao salvar usuários", e);
        }
    }

    /**
     * Carrega todos os usuários do arquivo JSON.
     * Retorna lista vazia se o arquivo ainda não existir.
     *
     * @return lista de usuários cadastrados
     */
    public List<User> carregarUsuarios() {
        File arquivo = new File(ARQUIVOUSER);
        if (!arquivo.exists()) {
            return new ArrayList<>();
        }

        try (FileReader reader = new FileReader(ARQUIVOUSER)) {
            Type tipo = new TypeToken<List<User>>() {}.getType();
            return gson.fromJson(reader, tipo);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao carregar usuários", e);
        }
    }

    /**
     * Verifica se já existe um usuário cadastrado com o CPF informado.
     *
     * @param cpf CPF a ser verificado
     * @return {@code true} se o CPF já estiver cadastrado
     */
    public boolean verificarCpfUsuario(String cpf) {
        List<User> users = carregarUsuarios();
        return users.stream()
            .anyMatch(user -> user.getCpfUsuario().equals(cpf));
    }
}
