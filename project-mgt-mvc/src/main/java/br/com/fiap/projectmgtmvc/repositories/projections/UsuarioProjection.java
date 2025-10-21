package br.com.fiap.projectmgtmvc.repositories.projections;

import org.springframework.beans.factory.annotation.Value;

public interface UsuarioProjection {

    @Value("#{target.primeiroNome} #{target.segundoNome}")
    String getNomeCompleto();

    @Value("#{target.contato.email?.toUpperCase}")
    String getEmail();

}
