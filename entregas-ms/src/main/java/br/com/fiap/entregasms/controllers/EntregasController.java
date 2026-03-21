package br.com.fiap.entregasms.controllers;

import br.com.fiap.entregasms.dtos.EntregaDto;
import br.com.fiap.entregasms.models.Entrega;
import br.com.fiap.entregasms.services.EntregaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/status")
public class EntregasController extends CommonController {

    private final EntregaService entregaService;

    public EntregasController(EntregaService entregaService) {
        this.entregaService = entregaService;
    }

    @GetMapping("/pendentes")
    public String pendentes(Model model) {
        model.addAttribute("screen","pendentes");
        model.addAttribute("entregas", EntregaDto.from(entregaService.findAllByStatus(List.of(Entrega.Status.PENDENTE))));
        return "entregas";
    }

    @GetMapping("/em-curso")
    public String emCurso(Model model) {
        model.addAttribute("screen","em-curso");
        model.addAttribute("entregas", EntregaDto.from(entregaService.findAllByStatus(List.of(Entrega.Status.COLETADO,Entrega.Status.EM_ROTA_ENTREGA))));
        return "entregas";
    }

    @GetMapping("/finalizados")
    public String finalizadas(Model model) {
        model.addAttribute("screen","finalizados");
        model.addAttribute("entregas", EntregaDto.from(entregaService.findAllByStatus(List.of(Entrega.Status.ENTREGUE))));
        return "entregas";
    }

    @PostMapping("/evoluir-status")
    public String evoluirStatus(UUID id, String screen) {
        this.entregaService.evoluirStatus(id);
        return "redirect:/status/" + screen;
    }

}
