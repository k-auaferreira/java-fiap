package br.com.fiap.projectmgtmvc.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Entity
@Table(name = "java_advanced_project")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    private String name;

    @Setter
    private String description;

    @Setter
    private LocalDate startDate;

    @Setter
    private LocalDate endDate;

    @Setter
    @Enumerated(EnumType.STRING)
    private Status status;

    @OneToMany(mappedBy = "projeto",
            fetch = FetchType.LAZY, //ou FetchType.EAGER
            cascade = {CascadeType.PERSIST, CascadeType.MERGE},
            orphanRemoval = true)
    @Getter
    private List<Tarefa> tarefas;

    public Project() {
    }

    public Project(Long id, String name, String description, LocalDate startDate, LocalDate endDate, Status status) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    public Project(Long id, String name, Status status) {
        this.id = id;
        this.name = name;
        this.status = status;
    }

    public enum Status {
        REFINE,
        WIP,
        REVIEW,
        COMPLETED
    }

    public void addTarefa(Tarefa tarefa) {
        this.tarefas.add(tarefa);
    }

    public void removeTarefa(Tarefa tarefa) {
        this.tarefas.remove(tarefa);
        tarefa.setProjeto(null); //remove the bidirectional relationship
    }
}
