CREATE TABLE usuario (
    login VARCHAR(255) NOT NULL,
    PRIMARY KEY (login)
);

CREATE TABLE usuarios_roles (
    login VARCHAR(255) NOT NULL,
    role  VARCHAR(255),
    CONSTRAINT fk_roles_usuario FOREIGN KEY (login) REFERENCES usuario (login)
);