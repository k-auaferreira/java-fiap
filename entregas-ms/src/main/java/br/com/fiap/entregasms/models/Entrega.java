package br.com.fiap.entregasms.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static br.com.fiap.entregasms.models.Entrega.Status.*;

@Entity
public class Entrega {

    @Id
    private UUID id;

    private String destinatario;

    @Column(columnDefinition = "text")
    private String enderecoCompleto;

    private Status status;

    public Entrega() {
        super();
    }

    public Entrega(UUID id, String destinatario, String enderecoCompleto, Status status) {
        this.id = id;
        this.destinatario = destinatario;
        this.enderecoCompleto = enderecoCompleto;
        this.status = status;
    }

    public Entrega(UUID id, String nome, String enderecoCompleto) {
        this.id = id;
        this.destinatario = destinatario;
        this.enderecoCompleto = enderecoCompleto;
        this.status = PENDENTE;
    }

    public UUID getId() {
        return id;
    }

    public String getDestinatario() {
        return destinatario;
    }

    public String getEnderecoCompleto() {
        return enderecoCompleto;
    }

    public Status getStatus() {
        return status;
    }

    public void evoluirStatus() {
        if (this.status.equals(PENDENTE)) {
            this.status = COLETADO;
        } else if (this.status.equals(COLETADO)) {
            this.status = EM_ROTA_ENTREGA;
        } else if (this.status.equals(EM_ROTA_ENTREGA)) {
            this.status = Status.ENTREGUE;
        }
    }

    public enum Status {
        PENDENTE,
        COLETADO,
        EM_ROTA_ENTREGA,
        ENTREGUE;
    }
}
