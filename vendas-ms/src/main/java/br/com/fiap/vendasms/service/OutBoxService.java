package br.com.fiap.vendasms.service;

import br.com.fiap.vendasms.entities.OutboxEvent;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OutBoxService {

    List<OutboxEvent> findPedentesToProcess();

    OutboxEvent save(OutboxEvent outboxEvent);
}
