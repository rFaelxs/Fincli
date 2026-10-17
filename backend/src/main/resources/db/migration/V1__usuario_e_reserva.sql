-- Usuário e reserva. A reserva nasce aqui porque o cadastro já cria a Reserva de Emergência.
--
-- Convenções (ADR 0008):
--   * PK clusterizada BIGINT IDENTITY. Um UNIQUEIDENTIFIER aleatório como chave clusterizada
--     fragmentaria o índice.
--   * public_id UNIQUEIDENTIFIER é o identificador exposto na API; o BIGINT nunca sai daqui.
--   * Dinheiro em DECIMAL(19,2). Nunca FLOAT/REAL, nunca o tipo MONEY.

CREATE TABLE usuario (
    id              BIGINT            IDENTITY(1,1) NOT NULL,
    public_id       UNIQUEIDENTIFIER  NOT NULL,
    nome            NVARCHAR(120)     NOT NULL,
    -- VARCHAR e não NVARCHAR: são só dígitos.
    cpf             VARCHAR(11)       NOT NULL,
    -- Opcional. Reservado para a recuperação de senha, que ainda não existe (ADR 0007).
    email           NVARCHAR(254)     NULL,
    senha_hash      VARCHAR(72)       NOT NULL,
    -- DATETIMEOFFSET, não DATETIME2: o campo é um Instant, e o Hibernate exige o tipo com
    -- deslocamento de fuso.
    criado_em       DATETIMEOFFSET(6) NOT NULL CONSTRAINT df_usuario_criado_em DEFAULT SYSDATETIMEOFFSET(),
    CONSTRAINT pk_usuario         PRIMARY KEY CLUSTERED (id),
    CONSTRAINT uq_usuario_public  UNIQUE (public_id),
    CONSTRAINT uq_usuario_cpf     UNIQUE (cpf)
);

CREATE TABLE reserva (
    id              BIGINT            IDENTITY(1,1) NOT NULL,
    public_id       UNIQUEIDENTIFIER  NOT NULL,
    usuario_id      BIGINT            NOT NULL,
    nome            NVARCHAR(120)     NOT NULL,
    meta_valor      DECIMAL(19,2)     NOT NULL CONSTRAINT df_reserva_meta   DEFAULT 0,
    saldo_atual     DECIMAL(19,2)     NOT NULL CONSTRAINT df_reserva_saldo  DEFAULT 0,
    emergencia      BIT               NOT NULL CONSTRAINT df_reserva_emerg  DEFAULT 0,
    criada_em       DATETIMEOFFSET(6) NOT NULL CONSTRAINT df_reserva_criada DEFAULT SYSDATETIMEOFFSET(),
    CONSTRAINT pk_reserva          PRIMARY KEY CLUSTERED (id),
    CONSTRAINT uq_reserva_public   UNIQUE (public_id),
    CONSTRAINT fk_reserva_usuario  FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT ck_reserva_meta     CHECK (meta_valor  >= 0),
    CONSTRAINT ck_reserva_saldo    CHECK (saldo_atual >= 0)
);

-- No máximo uma Reserva de Emergência por usuário, garantido pelo banco.
CREATE UNIQUE INDEX uq_reserva_emergencia_por_usuario
    ON reserva (usuario_id) WHERE emergencia = 1;

CREATE INDEX ix_reserva_usuario ON reserva (usuario_id);
