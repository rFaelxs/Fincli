-- Schema inicial do FinCLI Web.
--
-- Convenções (escopo-web.md §2.3):
--   * PK clusterizada BIGINT IDENTITY — evita fragmentação de página que um
--     UNIQUEIDENTIFIER aleatório como chave clusterizada provocaria.
--   * public_id UNIQUEIDENTIFIER é o identificador exposto na API; o BIGINT nunca sai daqui.
--   * Dinheiro em DECIMAL(19,2). Nunca FLOAT/REAL, nunca o tipo MONEY.

CREATE TABLE usuario (
    id              BIGINT           IDENTITY(1,1) NOT NULL,
    public_id       UNIQUEIDENTIFIER NOT NULL,
    nome            NVARCHAR(120)    NOT NULL,
    -- VARCHAR e não NVARCHAR: são só dígitos. Ver @Nationalized nas entidades para os
    -- campos de texto livre, que precisam de Unicode.
    cpf             VARCHAR(11)      NOT NULL,
    -- Opcional e usado exclusivamente para redefinição de senha (escopo-web.md §1.1).
    -- Nulo enquanto a decisão sobre recuperação de senha não for fechada.
    email           NVARCHAR(254)    NULL,
    senha_hash      VARCHAR(72)      NOT NULL,
    -- DATETIMEOFFSET, não DATETIME2: o campo é um Instant, e o Hibernate exige o tipo
    -- com deslocamento de fuso para TIMESTAMP_UTC.
    criado_em       DATETIMEOFFSET(6) NOT NULL CONSTRAINT df_usuario_criado_em DEFAULT SYSDATETIMEOFFSET(),
    CONSTRAINT pk_usuario         PRIMARY KEY CLUSTERED (id),
    CONSTRAINT uq_usuario_public  UNIQUE (public_id),
    CONSTRAINT uq_usuario_cpf     UNIQUE (cpf)
);

CREATE TABLE reserva (
    id              BIGINT           IDENTITY(1,1) NOT NULL,
    public_id       UNIQUEIDENTIFIER NOT NULL,
    usuario_id      BIGINT           NOT NULL,
    nome            NVARCHAR(120)    NOT NULL,
    meta_valor      DECIMAL(19,2)    NOT NULL CONSTRAINT df_reserva_meta   DEFAULT 0,
    saldo_atual     DECIMAL(19,2)    NOT NULL CONSTRAINT df_reserva_saldo  DEFAULT 0,
    emergencia      BIT              NOT NULL CONSTRAINT df_reserva_emerg  DEFAULT 0,
    criada_em       DATETIMEOFFSET(6) NOT NULL CONSTRAINT df_reserva_criada DEFAULT SYSDATETIMEOFFSET(),
    CONSTRAINT pk_reserva            PRIMARY KEY CLUSTERED (id),
    CONSTRAINT uq_reserva_public     UNIQUE (public_id),
    CONSTRAINT fk_reserva_usuario    FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT ck_reserva_meta       CHECK (meta_valor  >= 0),
    CONSTRAINT ck_reserva_saldo      CHECK (saldo_atual >= 0)
);

-- Garante no banco a regra de que existe no máximo uma Reserva de Emergência por usuário.
CREATE UNIQUE INDEX uq_reserva_emergencia_por_usuario
    ON reserva (usuario_id) WHERE emergencia = 1;

CREATE INDEX ix_reserva_usuario ON reserva (usuario_id);

CREATE TABLE transacao (
    id              BIGINT           IDENTITY(1,1) NOT NULL,
    public_id       UNIQUEIDENTIFIER NOT NULL,
    usuario_id      BIGINT           NOT NULL,
    valor           DECIMAL(19,2)    NOT NULL,
    categoria       NVARCHAR(80)     NOT NULL,
    descricao       NVARCHAR(255)    NULL,
    data_transacao  DATE             NOT NULL,
    tipo            VARCHAR(10)      NOT NULL,
    essencial       BIT              NOT NULL CONSTRAINT df_transacao_essencial DEFAULT 0,
    CONSTRAINT pk_transacao          PRIMARY KEY CLUSTERED (id),
    CONSTRAINT uq_transacao_public   UNIQUE (public_id),
    CONSTRAINT fk_transacao_usuario  FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT ck_transacao_valor    CHECK (valor >= 0),
    CONSTRAINT ck_transacao_tipo     CHECK (tipo IN ('ENTRADA', 'SAIDA'))
);

CREATE INDEX ix_transacao_usuario_data ON transacao (usuario_id, data_transacao DESC);

CREATE TABLE movimentacao_reserva (
    id              BIGINT           IDENTITY(1,1) NOT NULL,
    usuario_id      BIGINT           NOT NULL,
    reserva_id      BIGINT           NOT NULL,
    -- Preservado na própria linha: o extrato precisa mostrar o nome vigente na época
    -- do movimento, mesmo que a reserva seja renomeada ou excluída depois.
    reserva_nome    NVARCHAR(120)    NOT NULL,
    valor           DECIMAL(19,2)    NOT NULL,
    tipo            VARCHAR(10)      NOT NULL,
    data_movimento  DATE             NOT NULL,
    CONSTRAINT pk_movimentacao         PRIMARY KEY CLUSTERED (id),
    CONSTRAINT fk_movimentacao_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT fk_movimentacao_reserva FOREIGN KEY (reserva_id) REFERENCES reserva (id),
    CONSTRAINT ck_movimentacao_valor   CHECK (valor > 0),
    CONSTRAINT ck_movimentacao_tipo    CHECK (tipo IN ('ALOCACAO', 'SAQUE'))
);

CREATE INDEX ix_movimentacao_usuario ON movimentacao_reserva (usuario_id, data_movimento DESC);
