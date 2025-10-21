package br.com.fiap.projectmgtmvc.mappers;

import br.com.fiap.projectmgtmvc.dto.ProjectInDto;
import br.com.fiap.projectmgtmvc.dto.ProjectOutDto;
import br.com.fiap.projectmgtmvc.entities.Project;

public final class ProjectMapper {

    private ProjectMapper() {
    }

    public static ProjectInDto toInDto(Project project) {
        return new ProjectInDto(project.getId(), project.getName(), project.getDescription(), project.getStartDate(), project.getEndDate(), project.getStatus().name());
    }

    public static Project toEntity(ProjectInDto projectDto) {
        return new Project(projectDto.getId(), projectDto.getName(), projectDto.getDescription(), projectDto.getStartDate(), projectDto.getEndDate(), Project.Status.valueOf(projectDto.getStatus()));
    }

    public static ProjectOutDto toOutDto(Project project) {
        return new ProjectOutDto(project.getId(),  project.getName(), project.getDescription(), project.getStartDate(), project.getEndDate(), project.getStatus().name());
    }
}
