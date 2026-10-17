-- Contas que o usuário sabe que vão vencer no mês: aluguel, energia, cartão.
--
-- São o que separa "sobra" de "dá pra gastar": o número grande da Gestão desconta as contas
-- ainda por vencer no mês corrente.
--
-- Recorrência: `recorrente = 1` vale para todo mês, com `mes_referencia` nulo;
-- `recorrente = 0` vale só para o mês em `mes_referencia` ('yyyy-MM'). O CHECK impede a
-- terceira combinação, que não significa nada.

CREATE TABLE conta_prevista (
    id              BIGINT            IDENTITY(1,1) NOT NULL,
    public_id       UNIQUEIDENTIFIER  NOT NULL,
    usuario_id      BIGINT            NOT NULL,
    nome            NVARCHAR(120)     NOT NULL,
    valor           DECIMAL(19,2)     NOT NULL,
    dia_vencimento  INT               NOT NULL,
    recorrente      BIT               NOT NULL CONSTRAINT df_conta_prevista_rec DEFAULT 1,
    -- VARCHAR e não NVARCHAR: são só dígitos e um hífen.
    mes_referencia  VARCHAR(7)        NULL,
    criada_em       DATETIMEOFFSET(6) NOT NULL
        CONSTRAINT df_conta_prevista_criada DEFAULT SYSDATETIMEOFFSET(),
    CONSTRAINT pk_conta_prevista         PRIMARY KEY CLUSTERED (id),
    CONSTRAINT uq_conta_prevista_public  UNIQUE (public_id),
    CONSTRAINT fk_conta_prevista_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT ck_conta_prevista_valor   CHECK (valor > 0),
    CONSTRAINT ck_conta_prevista_dia     CHECK (dia_vencimento BETWEEN 1 AND 31),
    CONSTRAINT ck_conta_prevista_rec     CHECK (
        (recorrente = 1 AND mes_referencia IS NULL)
     OR (recorrente = 0 AND mes_referencia IS NOT NULL))
);

CREATE INDEX ix_conta_prevista_usuario ON conta_prevista (usuario_id, dia_vencimento);
