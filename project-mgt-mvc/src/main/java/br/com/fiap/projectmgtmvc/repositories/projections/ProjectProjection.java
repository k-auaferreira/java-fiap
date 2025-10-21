package br.com.fiap.projectmgtmvc.repositories.projections;

import br.com.fiap.projectmgtmvc.entities.Project;

public interface ProjectProjection {

    Long getId();

    String getName();

    Project.Status getStatus();
}


//
// public class ProjectProjection_tmp implements ProjectProjection {
//
// }
//