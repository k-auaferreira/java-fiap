package br.com.fiap.projectmgtmvc.dto;

import java.util.List;


public record AuthResponse(String token,
                           String username, String email, List<String> roles) {

    private final static String TYPE = "Bearer";

    public String type() {
        return TYPE;
    }
}
