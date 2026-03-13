package br.com.fiap.vendasms.entities;


import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
public class Usuario {

    @Id
    private String login;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "usuarios_roles", joinColumns = @JoinColumn(name = "login"))
    @Column(name = "role")
    private Set<String> roles;

    public Usuario() {
    }

    public Usuario(String login) {
        this.login = login;
        this.roles = new HashSet<>();
    }

    public String getLogin() {
        return login;
    }

    public Set<String> getRoles() {
        return roles;
    }
}
