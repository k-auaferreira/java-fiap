package br.com.fiap.projectmgtmvc.repositories;

import br.com.fiap.projectmgtmvc.entities.Project;
import br.com.fiap.projectmgtmvc.entities.Tarefa;
import br.com.fiap.projectmgtmvc.repositories.projections.ProjectProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    Page<Project> findAllByStatus(Pageable pageable, Project.Status status);

    Page<ProjectProjection> findByStatusNot(Project.Status status, Pageable pageable);

    @Query("select new br.com.fiap.projectmgtmvc.entities.Project(p.id,p.name,p.status) from Project p where p.status <> ?1")
    Page<Project> findByStatusNotDeOutroJeito(Project.Status status, Pageable pageable);

    @Transactional(readOnly = true)
    Page<Long> findIdByTarefas_Priority(Pageable pageable, Tarefa.Priority priority);

    Project findByStartDateGreaterThanEqualAndEndDateLessThanEqualOrderByEndDateDesc(LocalDate startDate, LocalDate endDate);

    @Query("""
            select p from Project p join p.tarefas t
            where t.nome= ?3 and p.startDate >= ?1 and p.endDate <= ?2 order by p.endDate DESC""")
    Project qualquerNome(LocalDate startDate, LocalDate endDate, String nomeTarefa);

    @Query(nativeQuery = true, value = """
            SELECT ID FROM JAVA_ADVANCED_PROJECT AS P
                        INNER JOIN TAREFA AS T ON T.PROJECT_ID = P.ID
                                    WHERE P.STATUS = 'WIP' AND T.PRIORITY = 'HIGH'""")
    Long qualquerNome2();

}
