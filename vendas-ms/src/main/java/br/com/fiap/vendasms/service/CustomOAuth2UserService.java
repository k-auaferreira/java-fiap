package br.com.fiap.vendasms.service;

import br.com.fiap.vendasms.entities.Usuario;
import br.com.fiap.vendasms.repositories.UsuarioRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public final class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UsuarioRepository usuarioRepository;

    public CustomOAuth2UserService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String login = oAuth2User.getAttribute("login");

        Usuario usuario = this.usuarioRepository.findById(login).orElseGet(() -> this.usuarioRepository.save(new Usuario(login)));
        Set<GrantedAuthority> authorities = new HashSet<>();
        usuario.getRoles().forEach(role -> authorities.add(new SimpleGrantedAuthority(role)));

        return new DefaultOAuth2User(authorities, oAuth2User.getAttributes(), "login");
    }
}
