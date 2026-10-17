-- Contas que ainda vão vencer no mês (design_handoff_fincli_dashboard/README.md).
--
-- Justificam o número principal da tela Hoje: "dá pra gastar" é a sobra do mês já
-- descontadas as contas previstas. Sem esta tabela, `livre` seria igual a `sobra`.
--
-- Recorrência: `recorrente = 1` vale para todo mês e `mes_referencia` fica nulo;
-- `recorrente = 0` vale só para o mês em `mes_referencia` ('yyyy-MM'). O CHECK abaixo
-- impede a terceira combinação, que não significa nada.

CREATE TABLE conta_prevista (
    id              BIGINT           IDENTITY(1,1) NOT NULL,
    public_id       UNIQUEIDENTIFIER NOT NULL,
    usuario_id      BIGINT           NOT NULL,
    nome            NVARCHAR(120)    NOT NULL,
    valor           DECIMAL(19,2)    NOT NULL,
    dia_vencimento  INT              NOT NULL,
    recorrente      BIT              NOT NULL CONSTRAINT df_conta_prevista_rec DEFAULT 1,
    -- VARCHAR e não NVARCHAR: são só dígitos e um hífen.
    mes_referencia  VARCHAR(7)       NULL,
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
