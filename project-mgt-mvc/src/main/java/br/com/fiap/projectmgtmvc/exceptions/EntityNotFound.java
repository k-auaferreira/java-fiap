package br.com.fiap.projectmgtmvc.exceptions;

public class EntityNotFound extends RuntimeException {

    public EntityNotFound(String message) {
        super(message);
    }
}
