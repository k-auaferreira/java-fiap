package br.com.fiap.projectmgt.exemplo_sala.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CpfValidator implements ConstraintValidator<Cpf,String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value.matches("^\\d{3}\\.\\d{3}\\.\\d{3}\\-\\d{2}$|^\\d{11}$");
    }
}
