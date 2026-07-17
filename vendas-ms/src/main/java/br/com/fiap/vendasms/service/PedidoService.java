package br.com.fiap.vendasms.service;

import br.com.fiap.vendasms.entities.Pedido;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PedidoService {
    List<Pedido> findByClienteCpf(String cpf);

    void save(Pedido pedido) throws JsonProcessingException;

    List<Pedido> findDistinctByStatus(Pedido.Status status, Pageable pageable);

    Optional<Pedido> findById(UUID id);

    void updateStatus(UUID id, Pedido.Status newStatus);
}
