package br.com.fiap.entregasms.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class HomeController extends CommonController {

    @GetMapping
    public String index(Model model) {
        model.addAttribute("screen","home");
        return "index";
    }

}
