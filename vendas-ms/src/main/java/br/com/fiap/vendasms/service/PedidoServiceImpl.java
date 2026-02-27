package br.com.fiap.vendasms.service;

import br.com.fiap.vendasms.entities.Pedido;
import br.com.fiap.vendasms.repositories.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
class PedidoServiceImpl implements PedidoService {

    private final PedidoRepository repository;

    public PedidoServiceImpl(PedidoRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Pedido> findByClienteCpf(String cpf) {
        return this.repository.findByCliente_Cpf(cpf);
    }

    @Override
    @Transactional
    public void save(Pedido pedido) {
        this.repository.save(pedido);
    }
}
