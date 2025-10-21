package br.com.fiap.projectmgtmvc.repositories;

import br.com.fiap.projectmgtmvc.entities.Project;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ProjectRepositoryTest {

    @Autowired
    ProjectRepository projectRepository;

    @Test
    void save_shouldPersistProjectWhenValidAndReturnWhenFindById() {
        Project project = new Project(null, "test",
                "description", LocalDate.now(),
                LocalDate.now(), Project.Status.REFINE);

        Project saved = this.projectRepository.save(project);
        assertThat(saved.getId()).isNotNull();

        Project found = this.projectRepository.findById(saved.getId()).orElse(null);
        assertThat(found.getName()).isEqualTo(project.getName());

    }

}