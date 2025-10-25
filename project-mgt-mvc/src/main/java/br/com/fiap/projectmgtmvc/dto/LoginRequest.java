package br.com.fiap.projectmgtmvc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Username é obrigatório")
    @Size(min = 3, max = 100)
    private String username;

    @NotBlank(message = "Password é obrigatório")
    @Size(min = 3)
    private String password;
}
