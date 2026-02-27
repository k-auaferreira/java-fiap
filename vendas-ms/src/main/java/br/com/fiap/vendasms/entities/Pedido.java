package br.com.fiap.vendasms.entities;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name="cliente_id")
    private Cliente cliente;

    @Enumerated(EnumType.STRING)
    private Status status;

    private String descricao;


    public enum Status {
        PENDENTE_ENVIO,
        ENVIO_EM_PROCESSAMENTO,
        FINALIZADO
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
}
