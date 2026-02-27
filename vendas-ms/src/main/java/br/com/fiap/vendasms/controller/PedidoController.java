package br.com.fiap.vendasms.controller;

import br.com.fiap.vendasms.dto.ClienteDto;
import br.com.fiap.vendasms.dto.PedidoDto;
import br.com.fiap.vendasms.entities.Cliente;
import br.com.fiap.vendasms.entities.Pedido;
import br.com.fiap.vendasms.service.ClienteService;
import br.com.fiap.vendasms.service.PedidoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/pedidos")
public class PedidoController extends CommonController {

    private final PedidoService pedidoService;
    private final ClienteService clienteService;

    public PedidoController(PedidoService pedidoService, ClienteService clienteService) {
        this.pedidoService = pedidoService;
        this.clienteService = clienteService;
    }

    @GetMapping
    public String index() {
        return "pedidos";
    }


    @GetMapping("/detalhe/{cpf}")
    public String detalhes(@PathVariable("cpf") String cpf, Model model) {
        final Cliente cliente = this.clienteService.findById(cpf);
        if (cliente.getNome() != null) {
            model.addAttribute("cliente", ClienteDto.from(cliente));

            final List<Pedido> pedidos = this.pedidoService.findByClienteCpf(cpf);
            model.addAttribute("pedidos", PedidoDto.from(pedidos));

            return "detalhe-pedidos";
        }
        return "redirect:/cliente/detalhes/" + cpf;


    }
}
