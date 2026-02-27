package br.com.fiap.vendasms.repositories;

import br.com.fiap.vendasms.entities.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PedidoRepository extends JpaRepository<Pedido, UUID> {

    List<Pedido> findByCliente_Cpf(String cpf);
}