package br.com.fiap.projectmgtmvc.dto;

import java.time.LocalDate;

public record ProjectOutDto(Long id, String name, String description, LocalDate startDate, LocalDate endDate,
                            String status) {
}
