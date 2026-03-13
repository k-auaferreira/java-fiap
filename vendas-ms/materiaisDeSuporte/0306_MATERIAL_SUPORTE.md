# Material de Suporte — Aula 05-06/03

**Objetivo:** Implementar controle de acesso por papel (role) usando banco de dados, proteger a área de pedidos com a role `ROLE_PEDIDO`, criar uma página de erro 403 seguindo o layout existente, e corrigir dois bugs no fluxo de criação de pedidos.

---

## Resumo: o protocolo OAuth2

**OAuth2** (Open Authorization 2.0) é um protocolo de **autorização** publicado em 2012 (RFC 6749). Ele permite que uma aplicação acesse recursos de um usuário em outro serviço **sem que o usuário entregue sua senha** para a aplicação.

### Problema que ele resolve

Antes do OAuth, a forma comum era pedir ao usuário que digitasse seu login e senha do serviço externo diretamente na aplicação. A aplicação então usava essas credenciais para agir em nome do usuário. Isso é perigoso: a aplicação tem acesso total à conta, as credenciais trafegam pela rede e o usuário não tem como revogar o acesso sem trocar a senha.

### Os quatro atores

| Ator | Quem é no nosso caso |
|---|---|
| **Resource Owner** | O usuário (você) |
| **Client** | A aplicação `vendas-ms` |
| **Authorization Server** | GitHub — verifica a identidade e emite tokens |
| **Resource Server** | API do GitHub (`api.github.com`) — fornece os dados do usuário |

### O fluxo Authorization Code Grant

É o fluxo mais seguro e o usado pelo Spring Security com GitHub:

```
[1] Usuário clica em "Entrar com GitHub"
[2] Client redireciona para o Authorization Server (GitHub)
         GET github.com/login/oauth/authorize?client_id=...&redirect_uri=...
[3] Usuário faz login no GitHub e aprova a aplicação
[4] Authorization Server devolve um "code" de uso único para o Client
         GET /login/oauth2/code/github?code=ABC123
[5] Client troca o code por um Access Token (comunicação servidor-a-servidor)
         POST github.com/login/oauth/access_token
[6] Client usa o Access Token para buscar dados do usuário
         GET api.github.com/user   (Authorization: Bearer <token>)
[7] Usuário está autenticado — a aplicação conhece sua identidade
```

O `code` do passo [4] expira em segundos e só pode ser usado uma vez. O `Access Token` do passo [5] é trocado diretamente entre os servidores, sem passar pelo browser — por isso é mais seguro do que abordagens anteriores.

### OAuth2 é autorização, não autenticação

Tecnicamente, OAuth2 foi criado para **autorizar acesso a recursos** (ex: "permitir que a aplicação leia seus repositórios"), não para autenticar usuários. Para autenticação, existe o **OpenID Connect (OIDC)**, uma camada construída sobre o OAuth2 que adiciona um `id_token` com dados de identidade. O Spring Security suporta os dois — quando usamos o GitHub como provedor, estamos usando OAuth2 para obter dados de identidade do endpoint `/user`, o que cumpre o mesmo papel na prática.

---

## Parte 1 — Controle de acesso por role

### O que é controle de acesso por role?

Até agora a aplicação exigia apenas que o usuário estivesse autenticado (login via GitHub). A partir daqui, diferentes usuários terão diferentes **permissões**: um usuário pode estar logado mas não ter acesso à área de pedidos — apenas usuários com a role `ROLE_PEDIDO` podem acessá-la.

O Spring Security implementa esse modelo através do conceito de **`GrantedAuthority`**: cada authority é uma string que representa uma permissão. O método `hasRole("PEDIDO")` verifica se o usuário possui a authority `ROLE_PEDIDO` (o prefixo `ROLE_` é adicionado automaticamente).

**Fluxo geral:**

```
Usuário faz login via GitHub
         ↓
CustomOAuth2UserService é chamado
         ↓
Busca o usuário no banco pela coluna login (GitHub login)
         ↓
Se não existir → salva com roles vazia (acesso básico)
         ↓
Carrega as roles do banco → converte para GrantedAuthority
         ↓
Spring Security usa essas authorities para autorizar requisições
```

---

### 1.1 Entidade `Usuario`

**Arquivo:** `src/main/java/br/com/fiap/vendasms/entities/Usuario.java` *(criar)*

```java
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    private String login;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "usuario_roles", joinColumns = @JoinColumn(name = "login"))
    @Column(name = "role")
    private Set<String> roles = new HashSet<>();

    public Usuario() {}

    public Usuario(String login) {
        this.login = login;
    }

    public String getLogin() { return login; }
    public Set<String> getRoles() { return roles; }
}
```

**Conceitos usados:**

| Anotação | O que faz |
|---|---|
| `@Entity` / `@Table` | Mapeia a classe para a tabela `usuarios` no banco |
| `@Id` | Define `login` (GitHub username) como chave primária — não precisa de auto-increment |
| `@ElementCollection` | Mapeia uma coleção de valores simples sem criar uma entidade separada |
| `@CollectionTable` | Define a tabela secundária `usuario_roles` que armazena as roles |
| `FetchType.EAGER` | As roles são carregadas junto com o usuário, na mesma consulta |

**Tabelas geradas pelo Hibernate:**

```sql
-- Tabela principal
CREATE TABLE usuarios (login VARCHAR(255) PRIMARY KEY);

-- Tabela de roles (one-to-many simples)
CREATE TABLE usuario_roles (
    login VARCHAR(255),
    role  VARCHAR(255),
    FOREIGN KEY (login) REFERENCES usuarios(login)
);
```

**Por que `@ElementCollection` e não `@OneToMany`?**

`@OneToMany` exige que o elemento seja uma entidade com `@Id` próprio. Para strings simples (como nomes de roles), `@ElementCollection` é a escolha certa: cria a tabela auxiliar sem precisar de uma classe `Role` separada.

---

### 1.2 Repositório `UsuarioRepository`

**Arquivo:** `src/main/java/br/com/fiap/vendasms/repositories/UsuarioRepository.java` *(criar)*

```java
public interface UsuarioRepository extends JpaRepository<Usuario, String> {
}
```

Nenhum método customizado é necessário: `findById(login)` e `save(usuario)` são suficientes e já vêm do `JpaRepository`. O tipo da chave primária é `String` (o login do GitHub).

---

### 1.3 Serviço `CustomOAuth2UserService`

**Arquivo:** `src/main/java/br/com/fiap/vendasms/service/CustomOAuth2UserService.java` *(criar)*

```java
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UsuarioRepository usuarioRepository;

    public CustomOAuth2UserService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);  // (1)

        String login = oAuth2User.getAttribute("login");      // (2)

        Usuario usuario = usuarioRepository.findById(login)   // (3)
                .orElseGet(() -> usuarioRepository.save(new Usuario(login)));

        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        usuario.getRoles().stream()
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);                   // (4)

        return new DefaultOAuth2User(authorities, oAuth2User.getAttributes(), "login"); // (5)
    }
}
```

**O que acontece em cada etapa:**

| Passo | O que faz |
|---|---|
| (1) `super.loadUser()` | Chama a API do GitHub e retorna os dados do usuário (login, avatar, etc.) |
| (2) `getAttribute("login")` | Extrai o GitHub username do mapa de atributos |
| (3) `findById().orElseGet()` | Busca no banco; se não existir, cria e salva com roles vazia |
| (4) `getRoles().stream()` | Converte as strings do banco (`"ROLE_PEDIDO"`) em `GrantedAuthority` |
| (5) `new DefaultOAuth2User(...)` | Retorna o usuário com as authorities do banco, não as padrão do OAuth2 |

**Por que estender `DefaultOAuth2UserService`?**

O Spring Security usa `DefaultOAuth2UserService` por padrão para buscar dados do usuário na API do GitHub após o login. Ao estender essa classe, interceptamos esse processo para adicionar nossa lógica de banco de dados — sem precisar reimplementar a chamada HTTP ao GitHub.

**Registrar novos usuários automaticamente:**

Quando um usuário faz login pela primeira vez, `findById(login)` retorna vazio e o `orElseGet` cria um `Usuario` com `roles` vazia. Isso garante que todo usuário que fizer login estará na tabela `usuarios`, pronto para receber roles via SQL direto.

---

### 1.4 Atualizar `SecurityConfig`

**Arquivo:** `src/main/java/br/com/fiap/vendasms/configs/SecurityConfig.java` *(modificar)*

```java
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
                        .requestMatchers("/pedidos/**").hasRole("PEDIDO")  // (1)
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)      // (2)
                        )
                )
                .exceptionHandling(ex -> ex
                        .accessDeniedPage("/403")                          // (3)
                );
        return http.build();
    }
}
```

**O que cada parte faz:**

| Passo | O que faz |
|---|---|
| (1) `hasRole("PEDIDO")` | Restringe `/pedidos/**` a usuários com `ROLE_PEDIDO`. A ordem importa: regras mais específicas devem vir antes de `anyRequest()` |
| (2) `userService(...)` | Substitui o serviço padrão pelo nosso `CustomOAuth2UserService` |
| (3) `accessDeniedPage("/403")` | Redireciona para a página customizada em vez do erro padrão do Spring |

**Por que a ordem das regras importa?**

O Spring Security avalia as regras de cima para baixo e para na primeira que casar. Se `anyRequest().authenticated()` viesse primeiro, `/pedidos/**` nunca chegaria na regra `hasRole("PEDIDO")`.

**Como gerenciar roles diretamente no banco:**

```sql
-- Conceder acesso à área de pedidos
INSERT INTO usuario_roles (login, role) VALUES ('seu-login-github', 'ROLE_PEDIDO');

-- Revogar acesso
DELETE FROM usuario_roles WHERE login = 'seu-login-github' AND role = 'ROLE_PEDIDO';

-- Ver todos os usuários e suas roles
SELECT u.login, r.role FROM usuarios u LEFT JOIN usuario_roles r ON u.login = r.login;
```

> **Importante:** As roles são carregadas no momento do login. Após alterar o banco, o usuário precisa fazer logout e login novamente para que as mudanças tenham efeito.

---

## Parte 2 — Página de erro 403

### O que é um erro 403?

O código HTTP **403 Forbidden** significa que o servidor entendeu a requisição, o usuário está autenticado, mas **não tem permissão** para acessar o recurso. É diferente do 401 (não autenticado) e do 404 (não encontrado).

Sem configuração adicional, o Spring Security retornaria uma página de erro genérica do framework. Vamos substituí-la por uma página que segue o layout da aplicação.

---

### 2.1 Endpoint no `HomeController`

**Arquivo:** `src/main/java/br/com/fiap/vendasms/controller/HomeController.java` *(modificar)*

```java
@GetMapping("/403")
public String accessDenied() {
    return "403";
}
```

**Por que precisamos de um endpoint e não apenas um arquivo HTML?**

O fragment `navbar` usa `${username}` e `${urlAvatar}`, que são populados pelo `@ModelAttribute` do `CommonController`. Sem passar pelo controller, o Thymeleaf não teria esses dados e a navbar quebraria. O endpoint garante que o pré-processador do `CommonController` rode antes de renderizar o template.

---

### 2.2 Template `403.html`

**Arquivo:** `src/main/resources/templates/403.html` *(criar)*

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title th:text="#{title.vendas}">Vendas</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="bg-gray-100 text-gray-900">
<div class="max-w-4xl mx-auto p-4">

    <div th:replace="~{fragments.html :: navbar('')}"></div>

    <div class="bg-white p-4 mt-4 shadow-md rounded-md">
        <main class="flex flex-col items-center py-12">
            <p class="text-6xl font-bold text-gray-300 mb-4">403</p>
            <p class="text-xl font-semibold text-gray-700 mb-2">Acesso negado</p>
            <p class="text-gray-500 mb-6">Você não tem permissão para acessar esta página.</p>
            <a th:href="@{/}" class="px-4 py-2 bg-gray-300 rounded hover:bg-gray-400">Voltar para início</a>
        </main>
    </div>

</div>
</body>
</html>
```

O fragment `navbar('')` recebe uma string vazia como `activePage` para que nenhum item fique destacado — o usuário está em uma página de erro, não em uma seção específica.

---

## Parte 3 — Correções no fluxo de novo pedido

### 3.1 Bug: `novo-pedido.html` — campo `cpf` com caminho inválido

**Arquivo:** `src/main/resources/templates/novo-pedido.html`

O template usava `*{cliente.cpf}` e `${pedido.cliente.cpf}`, mas o `PedidoInputDto` não tem um campo `cliente` — ele tem um campo `cpf` direto.

```html
<!-- ERRADO — PedidoInputDto não tem campo "cliente" -->
<input type="hidden" th:field="*{cliente.cpf}" th:value="${pedido.cliente.cpf}">

<!-- CORRETO — acessa o campo "cpf" diretamente no DTO -->
<input type="hidden" th:field="*{cpf}">
```

**Por que `th:field` é suficiente?**

`th:field="*{cpf}"` gera automaticamente `id="cpf"`, `name="cpf"` e `value="<valor atual>"` a partir do objeto em `th:object`. O `th:value` extra era redundante e a navegação `cliente.cpf` inexistente causava um `SpelEvaluationException` ao renderizar a página.

---

### 3.2 Bug: `PedidoController.salvar` — status nulo

**Arquivo:** `src/main/java/br/com/fiap/vendasms/controller/PedidoController.java`

O formulário não tinha um campo `status`, então `pedido.getStatus()` chegava `null` no controller. `Enum.valueOf(null)` lança `NullPointerException`.

```java
// ANTES — status vinha null do form, causando NPE
Pedido.Status.valueOf(pedido.getStatus())

// DEPOIS — status padrão definido diretamente
Pedido.Status.PENDENTE_ENVIO
```

> O status padrão `PENDENTE_ENVIO` faz sentido semanticamente: todo pedido novo começa como pendente de envio. Se no futuro o formulário precisar permitir escolha de status, basta adicionar o campo e reverter essa linha.

---

## Mapa de implementação

| Arquivo | Ação | O que faz |
|---|---|---|
| `entities/Usuario.java` | **Criar** | Entidade com `login` (PK) e `roles` em tabela auxiliar |
| `repositories/UsuarioRepository.java` | **Criar** | Repositório JPA para buscar e salvar usuários |
| `service/CustomOAuth2UserService.java` | **Criar** | Intercepta o login OAuth2, salva usuário no banco e carrega suas roles |
| `configs/SecurityConfig.java` | **Modificar** | Injeta o serviço customizado, restringe `/pedidos/**` e define página de erro 403 |
| `controller/HomeController.java` | **Modificar** | Adiciona endpoint `GET /403` para renderizar a página de acesso negado |
| `templates/403.html` | **Criar** | Página de erro seguindo o layout padrão da aplicação |
| `templates/novo-pedido.html` | **Corrigir** | Troca `*{cliente.cpf}` por `*{cpf}` |
| `controller/PedidoController.java` | **Corrigir** | Usa `PENDENTE_ENVIO` como status padrão em vez de ler campo nulo |

---

## Checklist de aprendizado

**Controle de acesso por role**
- [ ] Entendo a diferença entre autenticação (quem é você?) e autorização (o que você pode fazer?)
- [ ] Sei criar uma entidade com `@ElementCollection` para armazenar uma coleção de strings sem criar uma entidade separada
- [ ] Entendo por que `FetchType.EAGER` é necessário para que as roles estejam disponíveis na requisição
- [ ] Sei estender `DefaultOAuth2UserService` para interceptar o login e adicionar lógica customizada
- [ ] Entendo como `GrantedAuthority` e `SimpleGrantedAuthority` funcionam no Spring Security
- [ ] Sei a diferença entre `hasRole("PEDIDO")` e `hasAuthority("ROLE_PEDIDO")` (hasRole adiciona o prefixo automaticamente)
- [ ] Entendo por que a ordem das regras em `authorizeHttpRequests` importa
- [ ] Sei gerenciar roles diretamente no banco com `INSERT` e `DELETE` em `usuario_roles`
- [ ] Entendo por que o usuário precisa fazer logout/login após uma mudança de roles no banco

**Página de erro 403**
- [ ] Sei a diferença entre HTTP 401 (não autenticado) e 403 (sem permissão)
- [ ] Entendo por que precisamos de um endpoint controller para a página de erro (e não apenas um HTML estático)
- [ ] Sei configurar `.accessDeniedPage()` no `SecurityConfig`

**Correções de bugs**
- [ ] Entendo por que `*{cliente.cpf}` falha quando o DTO tem apenas `cpf` diretamente
- [ ] Sei usar `th:field="*{campo}"` e entendo o que ele gera automaticamente (id, name, value)
- [ ] Entendo por que `Enum.valueOf(null)` lança `NullPointerException` e como evitar
