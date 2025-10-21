package br.com.fiap.projectmgt.interfaces.exceptions;

import br.com.fiap.projectmgt.domain.exceptions.ResourceNotFoundException;
import jakarta.validation.ConstraintDefinitionException;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException e) {
        final ErrorResponse errorResponse = new ErrorResponse(e.getMessage());
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String,String>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {

        final Map<String, String> errors = new HashMap<>();
        e.getBindingResult()
                .getFieldErrors()
                .stream().parallel()
                .forEach( error -> errors.put(error.getField(), error.getDefaultMessage()));

        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(ConstraintDefinitionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String,String>> handleCpfNotValid(ConstraintDefinitionException e) {

        final Map<String, String> errors = new HashMap<>();
        //TODO: melhorar a verificação próxima aula
        if (e.getMessage().contains("Cpf")) {
            errors.put("cpf", "CPF inválido");
        }
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleResourceException(Exception e) {
        e.printStackTrace();
        final ErrorResponse errorResponse = new ErrorResponse("Ops.. ocorreu um error inesperado!");
        return ResponseEntity.internalServerError().body(errorResponse);
    }

    @Getter
    public static class ErrorResponse {

        private String message;

        public ErrorResponse(String message) {
            this.message = message;
        }
    }


}
