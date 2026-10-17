-- Entradas e saídas do usuário.

CREATE TABLE transacao (
    id              BIGINT            IDENTITY(1,1) NOT NULL,
    public_id       UNIQUEIDENTIFIER  NOT NULL,
    usuario_id      BIGINT            NOT NULL,
    valor           DECIMAL(19,2)     NOT NULL,
    categoria       NVARCHAR(80)      NOT NULL,
    descricao       NVARCHAR(255)     NULL,
    data_transacao  DATE              NOT NULL,
    tipo            VARCHAR(10)       NOT NULL,
    -- Só tem efeito em saída: separa o essencial do supérfluo nos totais da Gestão e do Relatório.
    essencial       BIT               NOT NULL CONSTRAINT df_transacao_essencial DEFAULT 0,
    CONSTRAINT pk_transacao          PRIMARY KEY CLUSTERED (id),
    CONSTRAINT uq_transacao_public   UNIQUE (public_id),
    CONSTRAINT fk_transacao_usuario  FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT ck_transacao_valor    CHECK (valor >= 0),
    CONSTRAINT ck_transacao_tipo     CHECK (tipo IN ('ENTRADA', 'SAIDA'))
);

-- A lista e as somas do mês sempre filtram pelo dono e ordenam ou recortam pela data.
CREATE INDEX ix_transacao_usuario_data ON transacao (usuario_id, data_transacao DESC);
