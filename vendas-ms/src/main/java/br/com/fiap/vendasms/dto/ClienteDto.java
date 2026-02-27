package br.com.fiap.vendasms.dto;

import br.com.fiap.vendasms.entities.Cliente;
import br.com.fiap.vendasms.external_interface.feign.CepDetails;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

/**
 * DTO for {@link br.com.fiap.vendasms.entities.Cliente}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ClienteDto(String cpf, String nome, String cep, String numero, String complemento,
                         String telefone, String logradouro, String bairro, String localidade,
                         String estado) implements Serializable {

    public static ClienteDto empty(String cpf) {
        return new ClienteDto(cpf, null, null, null, null, null, null, null, null, null);
    }

    public static ClienteDto from(Cliente cliente) {
        return new ClienteDto(cliente.getCpf(),
                cliente.getNome(),
                cliente.getNumero(),
                cliente.getCompleto(),
                cliente.getTelefone(),
                null,
                null,
                null,
                null,
                null);
    }

    public ClienteDto enrichWith(CepDetails cepDetails){
        if(cepDetails == null) {
            return this;
        }
        return new ClienteDto(cpf,nome,cep,numero,complemento, telefone,
                cepDetails.logradouro(), cepDetails.bairro(),
                cepDetails.localidade(),cepDetails.estado());
    }

    public Cliente toEntity() {
        return new Cliente(cpf,nome,cep,numero,complemento,telefone);
    }

}