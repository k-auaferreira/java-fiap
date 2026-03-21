package br.com.fiap.entregasms.utils;

import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

import java.util.Objects;

public final class GithubUserUtils {

    private GithubUserUtils() {
    }

    public static String getUsername(OAuth2AuthenticationToken authentication) {
        return authentication.getPrincipal().getAttribute("login");
    }

    public static String getAvatar(OAuth2AuthenticationToken authentication){
        return "https://avatars.githubusercontent.com/u/".concat(String.valueOf(((Integer) Objects.requireNonNull(authentication.getPrincipal().getAttribute("id"))).longValue())).concat("?v=4");
    }
}
