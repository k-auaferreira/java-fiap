package br.com.fiap.projectmgtmvc.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Usuario {

    @Id
    private String username;

    private String primeiroNome;

    private String segundoNome;

    @OneToOne
    private Contato contato;



}
