package br.com.fiap.projectmgtmvc.controller;

import br.com.fiap.projectmgtmvc.configs.GlobalExceptionHandler;
import br.com.fiap.projectmgtmvc.entities.Project;
import br.com.fiap.projectmgtmvc.services.ProjectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProjectController.class)
@Import(GlobalExceptionHandler.class)
class ProjectControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ProjectService projectService;


    @Test
    void getById_returnProjectWhenFound() throws Exception {
        Project project = new Project(1L, "test",
                "description", LocalDate.now(),
                LocalDate.now(), Project.Status.COMPLETED);

        when(this.projectService.findById(1L)).thenReturn(project);

        this.mockMvc.perform(get("/projects/1"))
                .andExpect(status().isOk())
                //TODO: validar os outros campos do json
                .andExpect(jsonPath("$.id", is(1)));

    }

}