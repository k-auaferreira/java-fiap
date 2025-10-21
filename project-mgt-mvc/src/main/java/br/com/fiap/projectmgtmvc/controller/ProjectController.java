package br.com.fiap.projectmgtmvc.controller;

import br.com.fiap.projectmgtmvc.dto.ProjectInDto;
import br.com.fiap.projectmgtmvc.dto.ProjectOutDto;
import br.com.fiap.projectmgtmvc.entities.Project;
import br.com.fiap.projectmgtmvc.mappers.ProjectMapper;
import br.com.fiap.projectmgtmvc.services.ProjectService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public ResponseEntity<Page<ProjectOutDto>> findAll(@RequestParam(required = false, defaultValue = "0") Integer page,
                                                       @RequestParam(required = false, defaultValue = "10") Integer size,
                                                       @RequestParam(required = false) String status) {
        final Page<Project> projects = this.projectService.findAll(PageRequest.of(page, size),Project.Status.valueOf(status));
        final List<ProjectOutDto> result = projects.getContent().stream().map(
                p -> ProjectMapper.toOutDto(p)).toList();
        return ResponseEntity.ok(new PageImpl<>(result, projects.getPageable(), projects.getTotalElements()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectOutDto> findById(@PathVariable("id") Long id) {
        final Project project = this.projectService.findById(id);
        return ResponseEntity.ok(ProjectMapper.toOutDto(project));
    }

    @PostMapping
    public ResponseEntity<ProjectOutDto> save(@Validated @RequestBody ProjectInDto project) {
        final Project saved = this.projectService.saveOrUpdate(ProjectMapper.toEntity(project));
        return ResponseEntity.ok(ProjectMapper.toOutDto(saved));
    }

    @PutMapping
    public ResponseEntity<ProjectOutDto> update(@Validated @RequestBody ProjectInDto project) {
        final Project updated = this.projectService.saveOrUpdate(ProjectMapper.toEntity(project));
        return ResponseEntity.ok(ProjectMapper.toOutDto(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable("id") Long id) {
        this.projectService.deleteById(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
