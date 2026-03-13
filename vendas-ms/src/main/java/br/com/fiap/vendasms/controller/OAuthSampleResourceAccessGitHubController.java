package br.com.fiap.vendasms.controller;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;


@Controller
@RequestMapping("/dev")
public class OAuthSampleResourceAccessGitHubController {


    private final OAuth2AuthorizedClientService authorizedClientService;

    public OAuthSampleResourceAccessGitHubController(OAuth2AuthorizedClientService authorizedClientService) {
        this.authorizedClientService = authorizedClientService;
    }

    @GetMapping("/repos")
    public String printPublicRepos(OAuth2AuthenticationToken authentication) {

        OAuth2AuthorizedClient client = this.authorizedClientService.loadAuthorizedClient(
                authentication.getAuthorizedClientRegistrationId(),
                authentication.getName());

        String token = client.getAccessToken().getTokenValue();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("Accept", "application/vnd.github+json");

        RestTemplate restTemplate = new RestTemplate();
        List<Map<String,Object>> repos = restTemplate.exchange(
                "https://api.github.com/user/repos?type=public&per_page=100",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<List<Map<String,Object>>>() {}
        ).getBody();

        if (repos != null && !repos.isEmpty()) {
            System.out.printf("================ REPOS INICIO================");
            System.out.println();
            repos.forEach(r -> {
                System.out.println(r.get("name"));
            });
            System.out.printf("================ REPOS FIM ================");
        }

        return "index";
    }
}
