CREATE TABLE outbox_event (
    id         CHAR(36)     NOT NULL PRIMARY KEY,
    reference_id  CHAR(36)     NOT NULL,
    class TEXT NOT NULL,
    destination TEXT NOT NULL,
    payload    TEXT         NOT NULL,
    status     VARCHAR(20)  NOT NULL DEFAULT 'PENDENTE',
    criado_em  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    enviado_em DATETIME
);