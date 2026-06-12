-- FinCLI - Schema PostgreSQL (Supabase)
-- Execute este script no SQL Editor do Supabase antes do primeiro deploy.

CREATE TABLE IF NOT EXISTS usuarios (
    id_usuario  VARCHAR(36)  PRIMARY KEY,
    nm_usuario  VARCHAR(255) NOT NULL,
    cpf_usuario VARCHAR(14)  NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS transacoes (
    id              VARCHAR(36)    PRIMARY KEY,
    id_usuario      VARCHAR(36)    NOT NULL REFERENCES usuarios(id_usuario),
    valor_transacao DECIMAL(15, 2) NOT NULL,
    categoria       VARCHAR(100),
    desc_transacao  VARCHAR(255),
    data_transacao  DATE           NOT NULL,
    tipo            VARCHAR(10)    NOT NULL,
    essencial       BOOLEAN        NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS reservas (
    id          VARCHAR(100)   NOT NULL,
    id_usuario  VARCHAR(36)    NOT NULL REFERENCES usuarios(id_usuario),
    nome        VARCHAR(255)   NOT NULL,
    meta_valor  DECIMAL(15, 2) NOT NULL DEFAULT 0,
    saldo_atual DECIMAL(15, 2) NOT NULL DEFAULT 0,
    emergencia  BOOLEAN        NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id, id_usuario)
);

CREATE TABLE IF NOT EXISTS movimentacoes_reserva (
    id           VARCHAR(36)    PRIMARY KEY,
    id_usuario   VARCHAR(36)    NOT NULL REFERENCES usuarios(id_usuario),
    id_reserva   VARCHAR(100)   NOT NULL,
    nome_reserva VARCHAR(255)   NOT NULL,
    valor        DECIMAL(15, 2) NOT NULL,
    tipo         VARCHAR(10)    NOT NULL,
    data         DATE           NOT NULL,
    FOREIGN KEY (id_reserva, id_usuario) REFERENCES reservas(id, id_usuario)
);
