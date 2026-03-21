package br.com.fiap.entregasms.services;

import br.com.fiap.entregasms.models.Entrega;
import br.com.fiap.entregasms.repositories.EntregaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
final class EntregaServiceImpl implements EntregaService {

    private final EntregaRepository repository;

    EntregaServiceImpl(EntregaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Entrega> findAllByStatus(List<Entrega.Status> status) {
        return this.repository.findByStatusIn(status);
    }

    @Override
    public void evoluirStatus(UUID id) {
        Entrega entrega = this.repository.findById(id).orElseThrow();
        entrega.evoluirStatus();
        this.repository.save(entrega);
    }

    @Override
    public void save(Entrega entrega) {
        this.repository.save(entrega);
    }
}
