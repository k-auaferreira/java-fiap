package br.com.fiap.vendasms.utils;

import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

import java.util.Objects;

public class GitHubUserUtils {

    private GitHubUserUtils() {
    }

    public static String getUsername(OAuth2AuthenticationToken authentication) {
        return authentication.getPrincipal().getAttribute("login");
    }

    public static String getAvatar(OAuth2AuthenticationToken authentication) {
        return authentication.getPrincipal().getAttribute("avatar_url");
    }
}
