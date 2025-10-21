package br.com.fiap.projectmgtmvc.dto;

import br.com.fiap.projectmgtmvc.entities.Project;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProjectInDto {

    private Long id;

    @NotEmpty
    private String name;

    private String description;

    private LocalDate startDate;

    private LocalDate endDate;

    public String status;
}
