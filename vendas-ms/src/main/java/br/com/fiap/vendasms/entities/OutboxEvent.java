package br.com.fiap.vendasms.entities;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "reference_id", nullable = false)
    private String referenceId;      // ID do pedido que originou o evento

    @Column(name = "class", nullable = false, columnDefinition = "TEXT")
    private String type;             // ex: "PEDIDO" — identifica o tipo do agregado

    @Column(nullable = false, columnDefinition = "TEXT")
    private String destination;      // ex: "pedido.queue" — fila de destino

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;          // conteúdo JSON da mensagem

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "enviado_em")
    private LocalDateTime enviadoEm;

    public OutboxEvent() {
    }

    public OutboxEvent(String referenceId, String type, String destination, String payload) {
        this.referenceId = referenceId;
        this.type = type;
        this.destination = destination;
        this.payload = payload;
        this.criadoEm = LocalDateTime.now();
        this.status = Status.PENDENTE;
    }

    public void marcarComoEnviado() {
        this.status = Status.ENVIADO;
        this.enviadoEm = LocalDateTime.now();
    }

    public String getDestination() {
        return destination;
    }

    public String getPayload() {
        return payload;
    }

    public UUID getId() {
        return id;
    }


    public enum Status {
        PENDENTE,
        ENVIADO
    }
}
