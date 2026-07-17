package br.com.fiap.vendasms.dto;

public record ClienteOutput(String nome, String cep,
                     String numero, String logradouro,
                     String bairro, String localidade,
                     String estado, String complemento) {
}
