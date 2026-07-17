package br.com.fiap.vendasms.service;

import br.com.fiap.vendasms.entities.OutboxEvent;
import br.com.fiap.vendasms.repositories.OutboxEventRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OutBoxServiceImpl implements OutBoxService {
    private final OutboxEventRepository outboxEventRepository;

    public OutBoxServiceImpl(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }


    @Override
    public List<OutboxEvent> findPedentesToProcess() {
        return this.outboxEventRepository.findDistinctPendentes();
    }

    @Override
    public OutboxEvent save(OutboxEvent outboxEvent) {
        return this.outboxEventRepository.save(outboxEvent);
    }
}
