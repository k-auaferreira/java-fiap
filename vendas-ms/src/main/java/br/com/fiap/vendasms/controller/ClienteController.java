package br.com.fiap.vendasms.controller;

import br.com.fiap.vendasms.dto.ClienteDto;
import br.com.fiap.vendasms.external_interface.feign.CepApi;
import br.com.fiap.vendasms.external_interface.feign.CepDetails;
import br.com.fiap.vendasms.service.ClienteService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@Controller
@RequestMapping("/clientes")
public class ClienteController extends CommonController{

    private final ClienteService clienteService;
    private final CepApi cepApi;

    public ClienteController(ClienteService clienteService, CepApi cepApi) {
        this.clienteService = clienteService;
        this.cepApi = cepApi;
    }

    @GetMapping
    public String index(){
        return "cliente";
    }

    @GetMapping("/detalhe")
    public String detalhe(@ModelAttribute ClienteDto form, Model model){
        ClienteDto clienteDto;
        try {
            var cliente = this.clienteService.findById(form.cpf());
            clienteDto = ClienteDto.from(cliente);
        } catch (NoSuchElementException e){
            clienteDto = ClienteDto.empty(form.cpf());
        }

        if(clienteDto.cep() != null && clienteDto.cep().isBlank()){
            var cepDetails = this.cepApi.get(clienteDto.cep());
            clienteDto.enrichWith(cepDetails);
        }

        model.addAttribute("cliente",clienteDto);
        return "detalhe-cliente";
    }

    @GetMapping("/detalhe/{cpf}")
    public String detalhe(@PathVariable("cpf") String cpf, Model model){
        return detalhe(ClienteDto.empty(cpf),model);
    }

    @PostMapping("/save")
    public String save(@ModelAttribute ClienteDto cliente){
        this.clienteService.saveOrUpdate(cliente.toEntity());
        return "redirect:/pedidos/detalhe/"+cliente.cpf();
    }



}
