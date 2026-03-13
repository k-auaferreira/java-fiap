package br.com.fiap.vendasms.configs;

import br.com.fiap.vendasms.service.CustomOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;

    public SecurityConfig(CustomOAuth2UserService customOAuth2UserService) {
        this.customOAuth2UserService = customOAuth2UserService;
    }


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/clientes/**").hasRole("CLIENTE_EDIT")
                        .requestMatchers("/pedidos/**").hasRole("PEDIDO")
                        .anyRequest().authenticated()
                )
                .oauth2Login(
                        oauth2 -> oauth2
                                .userInfoEndpoint(
                                        userInfo ->
                                                userInfo.userService(customOAuth2UserService))
                )
                .exceptionHandling(ex
                        -> ex.accessDeniedPage("/403"));
        return http.build();
    }
}