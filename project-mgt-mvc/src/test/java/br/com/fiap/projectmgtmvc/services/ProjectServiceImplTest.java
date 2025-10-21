package br.com.fiap.projectmgtmvc.services;

import br.com.fiap.projectmgtmvc.entities.Project;
import br.com.fiap.projectmgtmvc.exceptions.EntityNotFound;
import br.com.fiap.projectmgtmvc.repositories.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ProjectServiceImplTest {

    private ProjectService projectService;
    private ProjectRepository projectRepository;

    @BeforeEach
    void setUp() {
        this.projectRepository = mock(ProjectRepository.class);
        this.projectService = new ProjectServiceImpl(projectRepository);
    }

    @Test
    void findById_returnsThrowWhenNotFound() {
        when(this.projectRepository.findById(1L)).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> this.projectService.findById(1L)).isInstanceOf(EntityNotFound.class);
    }

    @Test
    void findById_returnsProjectWhenFound() {
        //o que
        Project project = new Project(1L,"test",
                "description",LocalDate.now(),
                LocalDate.now(), Project.Status.COMPLETED);
        //quando
        when(this.projectRepository.findById(1L)).thenReturn(Optional.of(project));

        //assert
        final Project found = this.projectService.findById(1L);
        verify(this.projectRepository,times(1)).findById(any());
        assertThat(found).isSameAs(project);
    }
}