package br.com.fiap.vendasms.repositories;

import br.com.fiap.vendasms.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, String> {
}