package br.com.fiap.vendasms.controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;

public abstract class CommonController {

    @ModelAttribute
    public void preProcessor(Model model){
        model.addAttribute("username","aluno-fiap");
        model.addAttribute("urlAvatar","https://github.com/identicons/fiap.png");
    }
}
