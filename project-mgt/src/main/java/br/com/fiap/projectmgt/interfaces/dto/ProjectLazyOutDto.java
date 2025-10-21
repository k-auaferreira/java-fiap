package br.com.fiap.projectmgt.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
// Também poder ser um record, apenas para demonstração
public class ProjectLazyOutDto {

    private Long id;

    private String name;

    private LocalDate startDate;

    private LocalDate endDate;
}
