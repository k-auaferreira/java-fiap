CREATE TABLE pedido (
    id         BINARY(16)   NOT NULL,
    descricao  VARCHAR(255),
    status     VARCHAR(255),
    cliente_id VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT fk_pedido_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (cpf)
);