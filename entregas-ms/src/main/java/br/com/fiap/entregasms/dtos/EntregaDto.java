package br.com.fiap.entregasms.dtos;

import br.com.fiap.entregasms.models.Entrega;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public final class EntregaDto {

    private UUID id;

    private String destinatario, enderecoCompleto, status;

    public EntregaDto() {
    }

    public EntregaDto(UUID id, String destinatario, String enderecoCompleto, String status) {
        this.id = id;
        this.destinatario = destinatario;
        this.enderecoCompleto = enderecoCompleto;
        this.status = status;
    }

    public static EntregaDto from(Entrega entrega) {
        return new EntregaDto(entrega.getId(), entrega.getDestinatario(), entrega.getEnderecoCompleto(), entrega.getStatus().name());
    }

    public static List<EntregaDto> from(List<Entrega> entregas) {
        return entregas.stream().map(EntregaDto::from).collect(Collectors.toList());
    }

    public Entrega toEntity(){
        return new Entrega(this.getId(),this.destinatario, this.enderecoCompleto, Entrega.Status.valueOf(this.status));
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getDestinatario() {
        return destinatario;
    }

    public void setDestinatario(String destinatario) {
        this.destinatario = destinatario;
    }

    public String getEnderecoCompleto() {
        return enderecoCompleto;
    }

    public void setEnderecoCompleto(String enderecoCompleto) {
        this.enderecoCompleto = enderecoCompleto;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
