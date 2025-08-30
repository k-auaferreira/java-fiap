package br.com.fiap.projectmgt.interfaces.controller;

import br.com.fiap.projectmgt.domain.entity.Project;
import br.com.fiap.projectmgt.domain.exceptions.ResourceNotFoundException;
import br.com.fiap.projectmgt.infrastructure.entity.JpaProjectEntity;
import br.com.fiap.projectmgt.infrastructure.repository.JpaProjectEntityRepository;
import br.com.fiap.projectmgt.interfaces.dto.ProjectLazyOutDto;
import br.com.fiap.projectmgt.interfaces.dto.ProjectOutDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;

// Não é uma boa pratica expor a entidade na camada controller. Refletir no porque e pensar em uma solução.
@RestController
@RequestMapping("/project")
public class ProjectController {

    //Não é um boa pratica injetar repositories na camada controller. Apenas para exemplo!!
//    @Autowired uma maneira de fazer injecao de dependencia. Sempre dar preferencia para o construtor.
    private final JpaProjectEntityRepository repository;

    public ProjectController(JpaProjectEntityRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ResponseEntity<Page<ProjectLazyOutDto>> getProjects(@RequestParam(name = "pageSize", required = false,defaultValue = "10") Integer pageSize,
                                                               @RequestParam(name = "pageNumber", required = false,defaultValue = "0") Integer pageNumber) {

        //Não é uma boa pratica usar o repository diretamente na camada
        // controller nem a classe utilizada no mapeamento do banco. Apenas para exemplo!!
        Page<JpaProjectEntity> pageOfProjects = this.repository.findAll(
                Pageable
                        .ofSize(pageSize)
                        .withPage(pageNumber));

        final List<ProjectLazyOutDto> allProjects = pageOfProjects.getContent().stream()
                //sem garantia de ordem
                .parallel()
                .map(
                        p -> new ProjectLazyOutDto(
                                p.getId(),
                                p.getName(),
                                p.getStartDate(),
                                p.getEndDate())
                ).toList();

        return ResponseEntity.ok(new PageImpl<>(allProjects, pageOfProjects.getPageable(), pageOfProjects.getTotalElements()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectOutDto> getProject(@PathVariable("id") Long id) {
        try {
            final JpaProjectEntity p = this.repository.findById(id).orElseThrow();
            final ProjectOutDto projectOutDto = new ProjectOutDto(
                    p.getId(), p.getName(),
                    p.getDescription(),
                    p.getStartDate(),
                    p.getEndDate()
            );

            return ResponseEntity.ok(projectOutDto);
        } catch (NoSuchElementException e) {
            throw new ResourceNotFoundException("Não foi possível localizar o id = "+id);
        }
    }

    @PostMapping
    //Depois fazer o dto In
    public ResponseEntity<Project> createProject(@RequestBody Project project) {
        JpaProjectEntity entity = new JpaProjectEntity(
                project.getName(), project.getDescription(), project.getStartDate(), project.getEndDate());
        this.repository.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(project);
    }

    @PutMapping
    public ResponseEntity<Project> updateProject(@RequestBody Project project) {
        //Implementar em casa o update
        return null;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable("id") Long id) throws Exception {
        throw new Exception();
    }

    @PatchMapping
    public ResponseEntity<Project> patchProject(@RequestBody Project project) {
        //Implementar em casa o patch

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(project);
    }


}