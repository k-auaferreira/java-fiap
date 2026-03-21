package br.com.fiap.entregasms.services;

import br.com.fiap.entregasms.models.Entrega;

import java.util.List;
import java.util.UUID;

public interface EntregaService {
    List<Entrega> findAllByStatus(List<Entrega.Status> status);

    void evoluirStatus(UUID id);

    void save(Entrega entrega);
}
