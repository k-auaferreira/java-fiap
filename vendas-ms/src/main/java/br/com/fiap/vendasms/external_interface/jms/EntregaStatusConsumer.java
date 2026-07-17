package br.com.fiap.vendasms.external_interface.jms;

import br.com.fiap.vendasms.entities.Pedido;
import br.com.fiap.vendasms.service.PedidoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class EntregaStatusConsumer {

    private static final Logger logger = LoggerFactory.getLogger(EntregaStatusConsumer.class);

    private final PedidoService pedidoService;
    private final ObjectMapper objectMapper;

    public EntregaStatusConsumer(PedidoService pedidoService, ObjectMapper objectMapper) {
        this.pedidoService = pedidoService;
        this.objectMapper = objectMapper;
    }

    private record EntregaStatusOutput(UUID id, Entrega.Status status) {}

    @JmsListener(destination = "entrega.queue")
    public void consume(String message) {
        try {
            EntregaStatusOutput entregaStatus = objectMapper.readValue(message, EntregaStatusOutput.class);

            pedidoService.findById(entregaStatus.id()).ifPresentOrElse(pedido -> {
                if (pedido.getStatus() == Pedido.Status.PENDENTE_ENVIO
                        && entregaStatus.status() == Entrega.Status.PENDENTE) {
                    pedidoService.updateStatus(pedido.getId(), Pedido.Status.ENVIO_EM_PROCESSAMENTO);
                    logger.info("Pedido {} atualizado para ENVIO_EM_PROCESSAMENTO", pedido.getId());
                } else {
                    logger.debug("Mensagem ignorada: pedido {} status={}, entrega status={}",
                            entregaStatus.id(), pedido.getStatus(), entregaStatus.status());
                }
            }, () -> logger.warn("Pedido não encontrado para id={}", entregaStatus.id()));

        } catch (Exception e) {
            logger.error("Erro ao processar mensagem de entrega.queue: {}", e.getMessage(), e);
        }
    }
}
