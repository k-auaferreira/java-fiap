package br.com.fiap.vendasms.service;

import br.com.fiap.vendasms.entities.Pedido;

import java.util.List;

public interface PedidoService {
    List<Pedido> findByClienteCpf(String cpf);

    void save(Pedido pedido);
}
