package br.com.fiap.projectmgt.domain.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

// Anotação para geração de construtores
@AllArgsConstructor
//@NoArgsConstructor

@EqualsAndHashCode

// Podemos usar Getter e Setter no nível de classe
//@Setter
//@Getter

//@Data
@Getter
@Setter
public class Project {

    private Long id;

    private String name;

    private String description;

    private LocalDate startDate;

    private LocalDate endDate;

    private List<Tarefa> tarefas;

}
