package br.com.fiap.projectmgtmvc.repositories;

import br.com.fiap.projectmgtmvc.entities.Usuario;
import br.com.fiap.projectmgtmvc.repositories.projections.UsuarioProjection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    UsuarioProjection findByUsername(String username);

}