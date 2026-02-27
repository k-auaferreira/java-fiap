package br.com.fiap.vendasms.service;

import br.com.fiap.vendasms.entities.Cliente;
import br.com.fiap.vendasms.repositories.ClienteRepository;
import org.springframework.stereotype.Service;

@Service
final class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteServiceImpl(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Override
    public Cliente findById(String cpf) {
        return this.clienteRepository.findById(cpf).orElseThrow();
    }

    @Override
    public void saveOrUpdate(Cliente cliente) {
        this.clienteRepository.save(cliente);
    }

}
