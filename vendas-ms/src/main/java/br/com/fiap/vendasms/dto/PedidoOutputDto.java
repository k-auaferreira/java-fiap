package br.com.fiap.vendasms.dto;

import br.com.fiap.vendasms.entities.Pedido;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * DTO for {@link Pedido}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PedidoOutputDto(UUID id, ClienteDto cliente, String status, String descricao) implements Serializable {

    // Converte entidade → DTO
    public static PedidoOutputDto from(Pedido pedido) {
        return new PedidoOutputDto(
                pedido.getId(),
                ClienteDto.from(pedido.getCliente()),
                pedido.getStatus().name(),
                pedido.getDescricao()
        );
    }

    // Converte lista de entidades → lista de DTOs
    public static List<PedidoOutputDto> from(List<Pedido> pedidos) {
        return pedidos.stream().map(PedidoOutputDto::from).toList();
    }

    public Pedido toEntity() {
        return new Pedido(id,
                cliente.toEntity(),
                status == null ? Pedido.Status.PENDENTE_ENVIO : Pedido.Status.valueOf(status),
                descricao);
    }
}