package com.rfaelxs.model;

import java.util.UUID;

/**
 * Representa um usuário do sistema.
 * Cada usuário possui um UUID único gerado no momento do cadastro,
 * um nome e um CPF usado como identificador de login.
 */
public class User {

    protected UUID idUsuario;
    protected String nmUsuario;
    protected String cpfUsuario;

    /**
     * Cria um novo usuário com os dados fornecidos.
     *
     * @param cpfUsuario CPF do usuário, usado para autenticação
     * @param idUsuario  UUID único gerado no cadastro
     * @param nmUsuario  Nome do usuário
     */
    public User(String cpfUsuario, UUID idUsuario, String nmUsuario) {
        this.cpfUsuario = cpfUsuario;
        this.idUsuario = idUsuario;
        this.nmUsuario = nmUsuario;
    }

    /** @return UUID único do usuário */
    public UUID getIdUsuario() {
        return idUsuario;
    }

    /** @param idUsuario novo UUID do usuário */
    public void setIdUsuario(UUID idUsuario) {
        this.idUsuario = idUsuario;
    }

    /** @return nome do usuário */
    public String getNmUsuario() {
        return nmUsuario;
    }

    /** @param nmUsuario novo nome do usuário */
    public void setNmUsuario(String nmUsuario) {
        this.nmUsuario = nmUsuario;
    }

    /** @return CPF do usuário */
    public String getCpfUsuario() {
        return cpfUsuario;
    }

    /** @param cpfUsuario novo CPF do usuário */
    public void setCpfUsuario(String cpfUsuario) {
        this.cpfUsuario = cpfUsuario;
    }

}
