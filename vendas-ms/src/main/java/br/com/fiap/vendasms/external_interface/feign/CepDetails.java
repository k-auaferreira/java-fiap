package br.com.fiap.vendasms.external_interface.feign;

public record CepDetails(String cep, String logradouro, String bairro, String localidade, String estado) {
}
