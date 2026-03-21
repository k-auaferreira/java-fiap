package br.com.fiap.entregasms.controllers;

import br.com.fiap.entregasms.utils.GithubUserUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.ModelAttribute;

class CommonController {

    @ModelAttribute
    private void preProcess(HttpServletRequest request, OAuth2AuthenticationToken authentication) {
        this.loadSession(request, authentication);
    }

    private void loadSession(HttpServletRequest request, OAuth2AuthenticationToken authentication) {
        request.getSession().setAttribute("username", GithubUserUtils.getUsername(authentication));
        request.getSession().setAttribute("urlAvatar", GithubUserUtils.getAvatar(authentication));
    }

}
