package br.com.fiap.projectmgtmvc.controller;


import br.com.fiap.projectmgtmvc.entities.Project;
import br.com.fiap.projectmgtmvc.repositories.ProjectRepository;
import br.com.fiap.projectmgtmvc.repositories.UsuarioRepository;
import br.com.fiap.projectmgtmvc.repositories.projections.ProjectProjection;
import br.com.fiap.projectmgtmvc.repositories.projections.UsuarioProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/teste")
public class ControllerApenasParaAulaQueries {
    // nao eh boa pratica apenas exemplo
    private final ProjectRepository projectRepository;

    private final UsuarioRepository usuarioRepository;

    public ControllerApenasParaAulaQueries(ProjectRepository projectRepository, UsuarioRepository usuarioRepository) {
        this.projectRepository = projectRepository;
        this.usuarioRepository = usuarioRepository;
    }


    @GetMapping("/project/{status-not}")
    public Page<ProjectProjection> getNotStatus(@PathVariable("status-not")String statusString){
        Project.Status status = Project.Status.valueOf(statusString);
        return this.projectRepository.findByStatusNot(status,PageRequest.of(0,10));
    }

    @GetMapping("/usuario/{username}")
    public UsuarioProjection getUsuarioByid(@PathVariable("username") String username) {
        return this.usuarioRepository.findByUsername(username);
    }
}
