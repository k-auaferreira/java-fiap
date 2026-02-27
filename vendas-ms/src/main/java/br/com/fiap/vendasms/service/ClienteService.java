package br.com.fiap.vendasms.service;

import br.com.fiap.vendasms.entities.Cliente;

public interface ClienteService {

    Cliente findById(String cpf);

    void saveOrUpdate(Cliente cliente);
}
