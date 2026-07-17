package br.com.fiap.vendasms.jobs;

import br.com.fiap.vendasms.entities.OutboxEvent;
import br.com.fiap.vendasms.service.OutBoxService;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import jakarta.jms.JMSException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedRuntimeException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OutBoxJob {

    private final OutBoxService outBoxService;
    private final JmsTemplate jmsTemplate;
    private final Tracer tracer;
    private final Propagator propagator;
    private final Logger logger = LoggerFactory.getLogger(OutBoxJob.class);

    public OutBoxJob(OutBoxService outBoxService, JmsTemplate jmsTemplate,
                     Tracer tracer, Propagator propagator) {
        this.outBoxService = outBoxService;
        this.jmsTemplate = jmsTemplate;
        this.tracer = tracer;
        this.propagator = propagator;
    }

    @Scheduled(fixedRate = 10000)
    public void pedidosPendentes() {
        final List<OutboxEvent> pedentesToProcess = this.outBoxService.findPedentesToProcess();
        for (OutboxEvent outboxEvent : pedentesToProcess) {
            try {
                publish(outboxEvent);
                logger.info("Evento {} pendente processado com Sucesso", outboxEvent.getId());
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
    }

    @Transactional
    void publish(OutboxEvent outboxEvent) throws Exception {
        // Create a span for this JMS publish so it appears in Zipkin
        // and its context is propagated to entregas-ms as the parent span.
        Span span = tracer.nextSpan()
                .name("jms.outbox.publish")
                .tag("destination", outboxEvent.getDestination())
                .tag("outbox.event.id", outboxEvent.getId().toString())
                .start();

        try (var ignored = tracer.withSpan(span)) {
            outboxEvent.marcarComoEnviado();
            this.outBoxService.save(outboxEvent);

            // Use send() instead of convertAndSend() so we can access the raw
            // JMS Message and inject the B3 trace headers before delivering.
            this.jmsTemplate.send(outboxEvent.getDestination(), session -> {
                var message = session.createTextMessage(outboxEvent.getPayload());

                // Inject the current span's context as JMS message properties.
                // JMS property names cannot contain '-', so we replace with '_'.
                // entregas-ms will read these properties to create a child span.
                propagator.inject(span.context(), message, (msg, key, value) -> {
                    try {
                        msg.setStringProperty(key.replace("-", "_"), value);
                    } catch (JMSException e) {
                        logger.warn("Falha ao injetar header de trace: {}", key, e);
                    }
                });
                return message;
            });

        } catch (NestedRuntimeException e) {
            span.error(e);
            throw new Exception("Falha ao processar o pedido: " + outboxEvent.getId());
        } finally {
            span.end();
        }
    }
}
