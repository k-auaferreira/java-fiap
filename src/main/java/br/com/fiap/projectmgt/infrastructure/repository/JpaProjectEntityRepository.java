package br.com.fiap.projectmgt.infrastructure.repository;

import br.com.fiap.projectmgt.infrastructure.entity.JpaProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaProjectEntityRepository extends JpaRepository<JpaProjectEntity, Long> {
}
