package br.com.fiap.vendasms.controller;

import br.com.fiap.vendasms.dto.ClienteDto;
import br.com.fiap.vendasms.dto.PedidoInputDto;
import br.com.fiap.vendasms.dto.PedidoOutputDto;
import br.com.fiap.vendasms.entities.Cliente;
import br.com.fiap.vendasms.entities.Pedido;
import br.com.fiap.vendasms.external_interface.feign.CepApi;
import br.com.fiap.vendasms.external_interface.feign.CepDetails;
import br.com.fiap.vendasms.service.ClienteService;
import br.com.fiap.vendasms.service.PedidoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/pedidos")
public class PedidoController extends CommonController {

    private final PedidoService pedidoService;
    private final ClienteService clienteService;
    private final CepApi cepApi;

    public PedidoController(PedidoService pedidoService, ClienteService clienteService, CepApi cepApi) {
        this.pedidoService = pedidoService;
        this.clienteService = clienteService;
        this.cepApi = cepApi;
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
            model.addAttribute("pedidos", PedidoOutputDto.from(pedidos));

            return "detalhe-pedidos";
        }
        return "redirect:/cliente/detalhes/" + cpf;
    }

    @PostMapping("/novo")
    public String novo(Model model, String cpf) {
        final Cliente cliente = this.clienteService.findById(cpf);
        final CepDetails cepDetails = this.cepApi.get(cliente.getCep());

        final ClienteDto client = ClienteDto.from(cliente, cepDetails);
        model.addAttribute("cliente", client);

        PedidoInputDto pedido = new PedidoInputDto();
        pedido.setCpf(client.cpf());

        model.addAttribute("pedido", pedido);

        //depois migrar para nao usar do dominio
        model.addAttribute("status", Pedido.Status.values());

        return "novo-pedido";
    }

    @PostMapping("/novo/salvar")
    public String salvar(@ModelAttribute PedidoInputDto pedido) {

        final Pedido pedidoEntity = new Pedido(null,
                new Cliente(pedido.getCpf()),
                Pedido.Status.valueOf(pedido.getStatus()),
                pedido.getDescricao());

        this.pedidoService.save(pedidoEntity);
        return "redirect:/";
    }
}
