package br.com.fiap.entregasms.repositories;

import br.com.fiap.entregasms.models.Entrega;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface EntregaRepository extends JpaRepository<Entrega, UUID> {

    List<Entrega> findByStatusIn(Collection<Entrega.Status> statuses);
}
