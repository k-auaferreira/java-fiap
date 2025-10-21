package br.com.fiap.projectmgt.exemplo_sala;

import br.com.fiap.projectmgt.exemplo_sala.validation.Cpf;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class TesteDto {

    @NotNull(message = "nome não pode ser nulo")
//    @NotEmpty
//    @NotBlank
    private String nome;

//    @Size(min = 1, max = 100)
    @Email
    private String email;

//    @Pattern(regexp = "^\\d{3}\\.\\d{3}\\.\\d{3}\\-\\d{2}$|^\\d{11}$", message = "CPF inválido")
    @Cpf
    private String cpf;

    @Past(message = "data de nascimento inválida")
//    @Future
//    @PastOrPresent
//    @FutureOrPresent
    private LocalDate dataNascimento;

//    @Min(10L)
//    @Max(100L)
//    @Positive
    @PositiveOrZero
//    @Negative
//    @NegativeOrZero
    private Double salario;
}
