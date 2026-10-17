-- Alocações e saques de reserva. Alimentam o extrato e os lançamentos da Gestão.

CREATE TABLE movimentacao_reserva (
    id              BIGINT            IDENTITY(1,1) NOT NULL,
    usuario_id      BIGINT            NOT NULL,
    -- Nulo depois que a reserva é excluída: o movimento continua no extrato, com o nome abaixo.
    reserva_id      BIGINT            NULL,
    -- Nome vigente na época do movimento. O extrato não muda se a reserva for renomeada ou
    -- excluída depois.
    reserva_nome    NVARCHAR(120)     NOT NULL,
    valor           DECIMAL(19,2)     NOT NULL,
    tipo            VARCHAR(10)       NOT NULL,
    data_movimento  DATE              NOT NULL,
    CONSTRAINT pk_movimentacao         PRIMARY KEY CLUSTERED (id),
    CONSTRAINT fk_movimentacao_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT fk_movimentacao_reserva FOREIGN KEY (reserva_id) REFERENCES reserva (id)
        ON DELETE SET NULL,
    CONSTRAINT ck_movimentacao_valor   CHECK (valor > 0),
    CONSTRAINT ck_movimentacao_tipo    CHECK (tipo IN ('ALOCACAO', 'SAQUE'))
);

CREATE INDEX ix_movimentacao_usuario_data ON movimentacao_reserva (usuario_id, data_movimento DESC);
CREATE INDEX ix_movimentacao_reserva ON movimentacao_reserva (reserva_id);
