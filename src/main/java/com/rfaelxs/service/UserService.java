package com.rfaelxs.service;

import java.util.List;
import java.util.UUID;

import com.rfaelxs.model.User;
import com.rfaelxs.repository.UserRepository;

/**
 * Contém a lógica de negócio para cadastro e autenticação de usuários.
 * Mantém a lista de usuários em memória e delega a persistência ao {@link UserRepository}.
 */
public class UserService {
    private final UserRepository userRepository;
    private final List<User> users;

    /**
     * Inicializa o serviço carregando os usuários existentes do repositório.
     *
     * @param userRepository repositório responsável pela leitura e escrita em arquivo
     */
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.users = userRepository.carregarUsuarios();
    }

    /**
     * Cadastra um novo usuário com UUID gerado automaticamente.
     * Retorna {@code null} se o CPF já estiver em uso.
     *
     * @param nome nome do usuário
     * @param cpf  CPF do usuário, usado como identificador de login
     * @return o {@link User} criado, ou {@code null} se o CPF já existir
     */
    public User cadastrarUsuario(String nome, String cpf) {
        boolean jaExiste = users.stream()
            .anyMatch(u -> u.getCpfUsuario().equals(cpf));

        if (jaExiste) {
            return null;
        }

        User novoUser = new User(cpf, UUID.randomUUID(), nome);
        this.users.add(novoUser);
        userRepository.salvarUsuarios(this.users);
        return novoUser;
    }

    /**
     * Autentica um usuário pelo CPF.
     *
     * @param cpf CPF informado na tela de login
     * @return o {@link User} encontrado, ou {@code null} se o CPF não existir
     */
    public User login(String cpf) {
        return users.stream()
            .filter(u -> u.getCpfUsuario().equals(cpf))
            .findFirst()
            .orElse(null);
    }

}
