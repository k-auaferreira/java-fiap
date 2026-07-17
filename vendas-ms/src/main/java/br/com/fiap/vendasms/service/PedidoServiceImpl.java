package br.com.fiap.vendasms.service;

import br.com.fiap.vendasms.dto.ClienteOutput;
import br.com.fiap.vendasms.dto.PedidoMessageOutput;
import br.com.fiap.vendasms.entities.OutboxEvent;
import br.com.fiap.vendasms.entities.Pedido;
import br.com.fiap.vendasms.external_interface.feign.CepApi;
import br.com.fiap.vendasms.external_interface.feign.CepDetails;
import br.com.fiap.vendasms.repositories.OutboxEventRepository;
import br.com.fiap.vendasms.repositories.PedidoRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
class PedidoServiceImpl implements PedidoService {

    private static final Logger logger = LoggerFactory.getLogger(PedidoServiceImpl.class);

    private final PedidoRepository repository;

    private final OutboxEventRepository outboxEventRepository;

    private final CepApi cepApi;

    private final ObjectMapper mapper;

    private static String PEDIDO = "PEDIDO";

    private static String QUEUE = "pedido.queue";

    public PedidoServiceImpl(PedidoRepository repository, OutboxEventRepository outboxEventRepository, CepApi cepApi, ObjectMapper mapper) {
        this.repository = repository;
        this.outboxEventRepository = outboxEventRepository;
        this.cepApi = cepApi;
        this.mapper = mapper;
    }

    @Override
    public List<Pedido> findByClienteCpf(String cpf) {
        logger.debug("Buscando pedidos cpf={}", cpf);
        return this.repository.findByCliente_Cpf(cpf);
    }

    @Override
    @Transactional
    public void save(Pedido pedido) {
        logger.info("Salvando pedido cpf={}", pedido.getCliente().getCpf());
        final Pedido saved = this.repository.save(pedido);
        final CepDetails cepDetails = cepApi.get(pedido.getCliente().getCep());
        try {
            final String message = mapPedidoMessage(pedido, cepDetails);
            this.outboxEventRepository.save(new OutboxEvent(saved.getId().toString(), PEDIDO, QUEUE, message));
            logger.info("Evento outbox criado pedidoId={} queue={}", saved.getId(), QUEUE);
        } catch (JsonProcessingException e) {
            logger.error("Erro ao serializar pedido para outbox pedidoId={}", saved.getId(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Pedido> findDistinctByStatus(Pedido.Status status, Pageable pageable) {
        return this.repository.findDistinctByStatus(status, pageable);
    }

    @Override
    public Optional<Pedido> findById(UUID id) {
        return this.repository.findById(id);
    }

    @Override
    @Transactional
    public void updateStatus(UUID id, Pedido.Status newStatus) {
        this.repository.findById(id).ifPresent(pedido -> {
            logger.info("Atualizando status pedidoId={} de={} para={}", id, pedido.getStatus(), newStatus);
            pedido.setStatus(newStatus);
            this.repository.save(pedido);
        });
    }

    private String mapPedidoMessage(Pedido p, CepDetails cepDetails) throws JsonProcessingException {
        PedidoMessageOutput message = new PedidoMessageOutput(p.getId(),
                new ClienteOutput(p.getCliente().getNome(), p.getCliente().getCep(), p.getCliente().getNumero(), cepDetails.logradouro(), cepDetails.bairro(),
                        cepDetails.localidade(), cepDetails.estado(), p.getCliente().getComplemento()));

        return this.mapper.writeValueAsString(message);
    }

}
