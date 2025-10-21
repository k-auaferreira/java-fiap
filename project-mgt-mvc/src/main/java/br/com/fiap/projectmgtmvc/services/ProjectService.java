package br.com.fiap.projectmgtmvc.services;

import br.com.fiap.projectmgtmvc.entities.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProjectService {
    Page<Project> findAll(Pageable pageable, Project.Status status);

    Project findById(Long id);

    Project saveOrUpdate(Project project);

    void deleteById(Long id);
}
