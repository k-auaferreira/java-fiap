package br.com.fiap.projectmgt.interfaces.dto;

import java.time.LocalDate;

public record ProjectOutDto(Long id,
                            String name,
                            String description,
                            LocalDate startDate, LocalDate endDate
) {
}
