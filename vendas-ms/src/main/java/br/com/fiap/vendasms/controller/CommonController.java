package br.com.fiap.vendasms.controller;

import br.com.fiap.vendasms.utils.GitHubUserUtils;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;

public abstract class CommonController {

    @ModelAttribute
    public void preProcessor(Model model, OAuth2AuthenticationToken authentication) {
        model.addAttribute("username", GitHubUserUtils.getUsername(authentication));
        model.addAttribute("urlAvatar",GitHubUserUtils.getAvatar(authentication));
    }
}
