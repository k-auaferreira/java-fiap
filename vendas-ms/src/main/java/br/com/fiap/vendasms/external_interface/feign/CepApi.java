package br.com.fiap.vendasms.external_interface.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "cep-api", url = "https://viacep.com.br/ws")
public interface CepApi {

    @GetMapping("/{cep}/json")
    CepDetails get(@PathVariable("cep") String cep);
}
