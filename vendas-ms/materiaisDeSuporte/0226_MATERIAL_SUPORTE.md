# Material de Suporte — Aulas 26 e 27/02

**Objetivo:** Corrigir os bugs e completar os métodos pendentes da aula 20/02, converter os templates HTML estáticos para Thymeleaf dinâmico, e implementar dois recursos novos: internacionalização (i18n) e autenticação OAuth2 com GitHub.

> **Nota sobre sequência:** Faça a atividade (Parte 1) antes de iniciar os novos conteúdos. A aplicação precisa estar funcionando antes de adicionar segurança.

---

## Parte 1 — Atividade: corrigir e completar a aula anterior

### O que ficou incompleto

Você vai corrigir **5 bugs** e implementar **10 itens** que ficaram pendentes. Cada item abaixo indica exatamente o arquivo e o que precisa mudar.

---

### 1.1 Bug: `ClienteDto.from()` — campos fora de ordem

**Arquivo:** `src/main/java/br/com/fiap/vendasms/dto/ClienteDto.java`

O record `ClienteDto` declara seus campos nesta ordem:

```
cpf → nome → cep → numero → complemento → telefone → logradouro → bairro → localidade → estado
```

Mas o método `from()` passa os campos fora de ordem, pulando o `cep`:

```java
// ERRADO — cep está ausente, todos os demais ficam deslocados
public static ClienteDto from(Cliente cliente) {
    return new ClienteDto(cliente.getCpf(),
            cliente.getNome(),
            cliente.getNumero(),   // ← posição 3 = cep, mas passando numero
            cliente.getCompleto(), // ← posição 4 = numero, mas passando completo
            cliente.getTelefone(), // ← posição 5 = complemento, mas passando telefone
            null,                  // ← posição 6 = telefone, mas passando null
            null, null, null, null);
}
```

```java
// CORRETO — cada getter mapeado para a posição certa
public static ClienteDto from(Cliente cliente) {
    return new ClienteDto(
            cliente.getCpf(),
            cliente.getNome(),
            cliente.getCep(),
            cliente.getNumero(),
            cliente.getCompleto(),
            cliente.getTelefone(),
            null, null, null, null
    );
}
```

**Por que isso acontece?** Records exigem que os argumentos do construtor sejam passados na mesma ordem em que os campos foram declarados. Não há nomes de parâmetro para guiar — só posição. Um campo a menos desloca todos os subsequentes em silêncio.

---

### 1.2 Bug: `ClienteController` — três erros de uma vez

**Arquivo:** `src/main/java/br/com/fiap/vendasms/controller/ClienteController.java`

**Erro 1 — Classe não herda `CommonController`**

Sem a herança, o `@ModelAttribute` do `CommonController` nunca é executado para este controller. O Thymeleaf receberá `${username}` e `${urlAvatar}` como `null` e a navbar quebrará.

```java
// ERRADO
public class ClienteController {

// CORRETO
public class ClienteController extends CommonController {
```

**Erro 2 — Método HTTP errado**

O formulário em `cliente.html` usa `method="post"`. Um `@GetMapping` nunca receberá essa requisição — o Spring retornará `405 Method Not Allowed`.

```java
// ERRADO
@GetMapping("/detalhe")
public String detalhe(@ModelAttribute ClienteDto form, Model model) {

// CORRETO
@PostMapping("/detalhe")
public String detalhe(@ModelAttribute ClienteDto form, Model model) {
```

**Erro 3 — Condição do CEP invertida**

`isBlank()` retorna `true` quando a string está vazia. A lógica atual chama a API justamente quando o CEP **está** em branco — o oposto do que deveria fazer.

```java
// ERRADO — chama a API quando o CEP está em branco
if (clienteDto.cep() != null && clienteDto.cep().isBlank()) {

// CORRETO — chama a API quando o CEP NÃO está em branco
if (clienteDto.cep() != null && !clienteDto.cep().isBlank()) {
```

---

### 1.3 Bug: `enrichWith()` — resultado ignorado

**Arquivo:** `src/main/java/br/com/fiap/vendasms/controller/ClienteController.java`

Ainda no método `detalhe()`, mesmo após corrigir a condição do CEP, o resultado de `enrichWith()` é descartado:

```java
// ERRADO — enrichWith retorna um NOVO record; o original não muda
clienteDto.enrichWith(cepDetails);

// CORRETO — atribuir o retorno à variável
clienteDto = clienteDto.enrichWith(cepDetails);
```

**Por que isso acontece?** Records são imutáveis. `enrichWith()` não altera o objeto existente — ele cria e **retorna** um novo record com os campos preenchidos. Ignorar o retorno é como escrever `"hello".toUpperCase()` sem guardar o resultado: a string original não muda.

Este é o mesmo princípio ensinado na aula anterior: *"records são imutáveis; o método retorna um novo record, não modifica o atual"*.

---

### 1.4 Implementar: construtor de conveniência em `Pedido`

**Arquivo:** `src/main/java/br/com/fiap/vendasms/entities/Pedido.java`

O `PedidoController` precisará criar um `Pedido` com três campos. Sem um construtor de conveniência, o código ficaria verboso (criar objeto + três setters). Adicione:

```java
public Pedido(Cliente cliente, Status status, String descricao) {
    this.cliente = cliente;
    this.status = status;
    this.descricao = descricao;
}
```

Posição sugerida: logo após o `enum Status`, antes dos getters.

---

### 1.5 Implementar: `ClienteController.save()`

**Arquivo:** `src/main/java/br/com/fiap/vendasms/controller/ClienteController.java`

O formulário de `detalhe-cliente.html` envia um POST para `/clientes/save`, mas esse endpoint não existe. Adicione após o método `detalhe()`:

```java
@PostMapping("/save")
public String save(@ModelAttribute ClienteDto form) {
    clienteService.saveOrUpdate(form.toEntity());
    return "redirect:/pedidos/detalhe/" + form.cpf();
}
```

O `redirect:` aplica o padrão **PRG (Post/Redirect/Get)**: após salvar, o browser faz um novo `GET` para a listagem de pedidos. Sem isso, recarregar a página repetiria o POST e salvaria o cliente duas vezes.

---

### 1.6 Implementar: `PedidoController` — três endpoints faltando

**Arquivo:** `src/main/java/br/com/fiap/vendasms/controller/PedidoController.java`

O controller atual tem apenas o `GET /pedidos`. Adicione as dependências e os três métodos:

```java
@Controller
@RequestMapping("/pedidos")
public class PedidoController extends CommonController {

    private final PedidoService pedidoService;
    private final ClienteService clienteService;

    public PedidoController(PedidoService pedidoService, ClienteService clienteService) {
        this.pedidoService = pedidoService;
        this.clienteService = clienteService;
    }

    @GetMapping
    public String index() {
        return "pedidos";
    }

    // Exibe pedidos de um cliente identificado pelo CPF na URL
    @GetMapping("/detalhe/{cpf}")
    public String detalhe(@PathVariable String cpf, Model model) {
        var pedidos = pedidoService.findByClienteCpf(cpf);
        model.addAttribute("pedidoDtos", PedidoDto.from(pedidos));
        model.addAttribute("cpf", cpf);
        return "detalhe-pedidos";
    }

    // Exibe o formulário de novo pedido para o cliente informado
    @PostMapping("/novo")
    public String novo(@RequestParam String cpf, Model model) {
        model.addAttribute("cpf", cpf);
        model.addAttribute("statuses", Pedido.Status.values());
        return "novo-pedido";
    }

    // Persiste o novo pedido e redireciona para a listagem
    @PostMapping("/novo/salvar")
    public String salvar(@RequestParam String cpf,
                         @RequestParam String descricao,
                         @RequestParam Pedido.Status status) {
        var cliente = clienteService.findById(cpf);
        pedidoService.save(new Pedido(cliente, status, descricao));
        return "redirect:/pedidos/detalhe/" + cpf;
    }
}
```

**Como cada método captura os dados da requisição:**

| Anotação | De onde lê | Exemplo neste projeto |
|---|---|---|
| `@PathVariable` | Segmento da URL | `GET /pedidos/detalhe/12345678900` → `cpf = "12345678900"` |
| `@RequestParam` | Campo do formulário ou query string | `<input name="cpf">` → `cpf = valor_digitado` |
| `@ModelAttribute` | Vários campos do form → objeto | Formulário com N campos → preenche um DTO inteiro |

---

### 1.7 Converter: `index.html`

**Arquivo:** `src/main/resources/templates/index.html`

Este é o único template que ainda está 100% estático (mock). Faça as três alterações:

**1. Adicionar o namespace Thymeleaf na tag `<html>`:**

```html
<!-- ANTES -->
<html lang="pt-BR">

<!-- DEPOIS -->
<html lang="pt-BR" xmlns:th="http://www.thymeleaf.org">
```

Sem isso, os atributos `th:*` são tratados como atributos HTML inválidos e ignorados pelo motor de templates.

**2. Substituir o bloco `<nav>` inteiro pelo fragment:**

```html
<!-- REMOVER todo o bloco <nav>...</nav> e colocar no lugar: -->
<div th:replace="~{fragments.html :: navbar('home')}"></div>
```

**3. Substituir o nome fixo e o placeholder Lottie:**

```html
<!-- ANTES -->
<h1 class="text-2xl font-bold text-gray-800 mb-4 text-center">
    Bem-vindo, <span>aluno-fiap</span>!
</h1>
<div class="w-80 h-80 flex flex-col items-center justify-center mx-auto ...">
    <!-- svg placeholder -->
</div>

<!-- DEPOIS -->
<h1 class="text-2xl font-bold text-gray-800 mb-4 text-center">
    Bem-vindo, <span th:text="${username}">aluno-fiap</span>!
</h1>
<div id="lottie-container" class="w-80 h-80 mx-auto"></div>
<script src="https://cdnjs.cloudflare.com/ajax/libs/lottie-web/5.12.2/lottie.min.js"></script>
<script src="/js/home.js"></script>
```

---

### 1.8 Converter: `detalhe-cliente.html`

**Arquivo:** `src/main/resources/templates/detalhe-cliente.html`

A estrutura da página já está correta (o `th:replace` da navbar e o `th:action`/`th:object` do form já existem). O que falta:

**1. Adicionar o namespace Thymeleaf na tag `<html>`:**

```html
<html lang="pt-BR" xmlns:th="http://www.thymeleaf.org">
```

**2. Substituir `value="..."` por `th:field="*{campo}"` em cada input:**

```html
<!-- ANTES -->
<input id="cpf" type="text" name="cpf" value="12345678900" .../>
<input id="nome" type="text" name="nome" value="Maria Oliveira" .../>
<input id="cep" type="text" name="cep" value="01310100" .../>

<!-- DEPOIS -->
<input type="text" th:field="*{cpf}" .../>
<input type="text" th:field="*{nome}" .../>
<input type="text" th:field="*{cep}" .../>
```

`th:field="*{campo}"` é um atalho que gera `id`, `name` e `value` automaticamente a partir do objeto declarado em `th:object`. Com ele, `id="cpf"` e `name="cpf"` não precisam mais ser escritos manualmente.

**3. Envolver o bloco de endereço com `th:unless`:**

```html
<!-- ANTES: sempre visível -->
<div class="pl-32 text-sm text-gray-500 ...">
    <p>Rua: <strong>Avenida Paulista</strong></p>
    ...
</div>

<!-- DEPOIS: visível somente quando a API ViaCEP retornou dados -->
<div th:unless="${cliente.logradouro == null}" class="pl-32 text-sm text-gray-500 ...">
    <p>Rua: <strong th:text="*{logradouro}">-</strong></p>
    <p>Bairro: <strong th:text="*{bairro}">-</strong></p>
    <p th:text="*{localidade} + ' / ' + *{estado}">-</p>
</div>
```

**4. Fazer o mesmo com os demais inputs (`numero`, `complemento`, `telefone`):**

```html
<input type="text" th:field="*{numero}" .../>
<input type="text" th:field="*{complemento}" .../>
<input type="text" th:field="*{telefone}" .../>
```

---

### 1.9 Converter: `detalhe-pedidos.html`

**Arquivo:** `src/main/resources/templates/detalhe-pedidos.html`

**1. Adicionar namespace e substituir a navbar:**

```html
<html lang="pt-BR" xmlns:th="http://www.thymeleaf.org">
...
<div th:replace="~{fragments.html :: navbar('pedidos')}"></div>
```

**2. Substituir o bloco estático de dados do cliente pelo fragment:**

```html
<!-- REMOVER todo o <div> com dados fixos de Maria Oliveira -->
<!-- COLOCAR no lugar: -->
<div th:replace="~{fragments.html :: dadosCliente('Pedidos', true)}"></div>
```

O fragment `dadosCliente` lê `${cliente}` do model. Atenção: o `PedidoController.detalhe()` ainda **não** está adicionando o cliente ao model — você precisará adicionar isso:

```java
// No PedidoController.detalhe(), após buscar os pedidos, adicione:
var cliente = clienteService.findById(cpf);
var cepDetails = cepApi.get(cliente.getCep());        // buscar endereço
var clienteDto = ClienteDto.from(cliente).enrichWith(cepDetails);
model.addAttribute("cliente", clienteDto);
```

Isso exige que `PedidoController` receba também o `CepApi` como dependência.

**3. Substituir as linhas fixas da tabela por `th:each`:**

```html
<!-- REMOVER as três linhas <tr> fixas e o bloco comentado de "linha vazia" -->
<!-- COLOCAR no lugar: -->

<tr th:if="${#lists.isEmpty(pedidoDtos)}">
    <td colspan="3" class="py-2 text-center text-gray-500">Nenhum pedido registrado</td>
</tr>

<tr th:each="pedido : ${pedidoDtos}" class="border-t">
    <td class="py-2 text-center text-xs text-gray-400" th:text="${pedido.id}">-</td>
    <td class="py-2 text-center" th:text="${pedido.descricao}">-</td>
    <td class="py-2 text-center" th:text="${pedido.status}">-</td>
</tr>
```

> **`#lists.isEmpty()`** é um utilitário de expressão do Thymeleaf para verificar se uma coleção está vazia sem chamar `.size() == 0`. Outros utilitários úteis: `#strings`, `#dates`, `#numbers`.

---

### 1.10 Converter: `novo-pedido.html`

**Arquivo:** `src/main/resources/templates/novo-pedido.html`

**1. Adicionar namespace e substituir a navbar:**

```html
<html lang="pt-BR" xmlns:th="http://www.thymeleaf.org">
...
<div th:replace="~{fragments.html :: navbar('pedidos')}"></div>
```

**2. Substituir o bloco de dados do cliente pelo fragment (sem botões):**

```html
<!-- REMOVER o <div> com dados fixos -->
<!-- COLOCAR: -->
<div th:replace="~{fragments.html :: dadosCliente('Novo Pedido', false)}"></div>
```

**3. Adicionar `th:action` no form e `th:value` no campo oculto:**

```html
<form th:action="@{/pedidos/novo/salvar}" method="post" class="space-y-4">
    <input type="hidden" name="cpf" th:value="${cpf}"/>
```

**4. Substituir as `<option>` fixas por `th:each`:**

```html
<!-- ANTES: opções escritas manualmente -->
<select id="status" name="status" ...>
    <option value="PENDENTE_ENVIO" selected>PENDENTE_ENVIO</option>
    <option value="ENVIO_EM_PROCESSAMENTO">ENVIO_EM_PROCESSAMENTO</option>
    <option value="FINALIZADO">FINALIZADO</option>
</select>

<!-- DEPOIS: opções geradas a partir do enum Java -->
<select name="status" class="border rounded px-2 py-1">
    <option th:each="s : ${statuses}"
            th:value="${s.name()}"
            th:text="${s.name()}">STATUS</option>
</select>
```

`${statuses}` vem de `model.addAttribute("statuses", Pedido.Status.values())` no `PedidoController.novo()`. `th:each` funciona em **qualquer** elemento HTML, não apenas em `<tr>` ou `<li>`.

---

### 1.11 Converter: `pedidos.html`

**Arquivo:** `src/main/resources/templates/pedidos.html`

Esta view tem uma particularidade: ela usa JavaScript puro para navegar, porque a rota alvo é `GET /pedidos/detalhe/{cpf}` (path variable, não parâmetro de formulário). Não é possível usar um `<form>` direto para isso.

**1. Adicionar namespace e substituir a navbar:**

```html
<html lang="pt-BR" xmlns:th="http://www.thymeleaf.org">
...
<div th:replace="~{fragments.html :: navbar('pedidos')}"></div>
```

**2. Substituir o JS inline pelo arquivo externo:**

```html
<!-- REMOVER o bloco <script> inline com a função mock -->
<!-- COLOCAR no lugar: -->
<script src="/js/pedidos.js"></script>
```

O arquivo `pedidos.js` já existe em `src/main/resources/static/js/` com a implementação real (sem o `alert` do mock).

---

### 1.12 Corrigir: `fragments.html` — `${session.username}` vs `${username}`

**Arquivo:** `src/main/resources/templates/fragments.html`

O `CommonController` adiciona dados ao `Model` do Spring MVC, não à sessão HTTP. Em Thymeleaf, `${session.xxx}` acessa a sessão HTTP (objeto `HttpSession`), enquanto `${xxx}` acessa o model.

```html
<!-- ERRADO — acessa sessão, que não tem esse dado -->
<span th:text="${session.username}">devUser</span>
<img th:src="${session.urlAvatar}" .../>

<!-- CORRETO — acessa o model preenchido pelo CommonController -->
<span th:text="${username}">devUser</span>
<img th:src="${urlAvatar}" .../>
```

---

## Parte 2 — Internacionalização (i18n)

### O que é internacionalização?

Internacionalização (abreviada **i18n** — 18 letras entre o "i" e o "n") é a prática de separar os textos visíveis ao usuário do código-fonte, permitindo que a mesma aplicação exiba conteúdo em diferentes idiomas sem alterar o código Java ou HTML.

O princípio central é: **nunca escreva texto visível ao usuário diretamente no código**. Em vez de `"Localizar"`, use uma chave como `cliente.busca.botao`, e guarde a tradução em um arquivo separado por idioma.

### Por que não usar apenas um arquivo `.properties`?

Sem i18n, cada mudança de texto exige alterar o código-fonte e recompilar. Com i18n:
- O mesmo template HTML funciona em qualquer idioma.
- Adicionar um novo idioma é só criar um novo arquivo de tradução.
- Equipes de design e tradução trabalham sem tocar no código Java.

---

### 2.1 Arquivos de mensagens

Crie os dois arquivos em `src/main/resources/`:

**`messages.properties`** (português — idioma padrão):

```properties
# Navbar
nav.inicio=Início
nav.clientes=Clientes
nav.pedidos=Pedidos

# Página de busca de cliente
cliente.busca.titulo=Localizar cliente pelo CPF
cliente.busca.placeholder=Apenas números (11 dígitos)
cliente.busca.botao=Localizar

# Cadastro de cliente
cliente.cadastro.titulo=Cadastro de cliente
cliente.campo.cpf=CPF
cliente.campo.nome=Nome
cliente.campo.cep=CEP
cliente.campo.numero=Número
cliente.campo.complemento=Complemento
cliente.campo.telefone=Telefone
cliente.botao.salvar=Salvar
cliente.cep.encontrado=Endereço encontrado via API ViaCEP

# Pedidos
pedido.busca.titulo=Localizar pedidos por cliente
pedido.tabela.id=Id
pedido.tabela.descricao=Descrição
pedido.tabela.status=Status
pedido.vazio=Nenhum pedido registrado
pedido.novo.titulo=Novo Pedido
pedido.campo.descricao=Descrição
pedido.campo.status=Status inicial
pedido.botao.salvar=Salvar Pedido
pedido.botao.nova=Nova venda

# Página inicial
home.benvindo=Bem-vindo,
```

**`messages_en.properties`** (inglês):

```properties
# Navbar
nav.inicio=Home
nav.clientes=Customers
nav.pedidos=Orders

# Customer search
cliente.busca.titulo=Find customer by tax ID
cliente.busca.placeholder=Numbers only (11 digits)
cliente.busca.botao=Search

# Customer form
cliente.cadastro.titulo=Customer registration
cliente.campo.cpf=Tax ID
cliente.campo.nome=Name
cliente.campo.cep=Zip Code
cliente.campo.numero=Number
cliente.campo.complemento=Complement
cliente.campo.telefone=Phone
cliente.botao.salvar=Save
cliente.cep.encontrado=Address found via ViaCEP API

# Orders
pedido.busca.titulo=Find orders by customer
pedido.tabela.id=Id
pedido.tabela.descricao=Description
pedido.tabela.status=Status
pedido.vazio=No orders found
pedido.novo.titulo=New Order
pedido.campo.descricao=Description
pedido.campo.status=Initial status
pedido.botao.salvar=Save Order
pedido.botao.nova=New order

# Home page
home.benvindo=Welcome,
```

> **Convenção de nomes:** use o padrão `contexto.elemento.tipo`, por exemplo: `cliente.campo.cpf`, `pedido.botao.salvar`. Nomes descritivos evitam colisões e facilitam manutenção.

---

### 2.2 Configuração

Crie `src/main/java/br/com/fiap/vendasms/configurations/Internationalization.java`:

```java
@Configuration
public class Internationalization implements WebMvcConfigurer {

    // Define como o idioma atual é armazenado entre requisições
    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver resolver = new SessionLocaleResolver();
        resolver.setDefaultLocale(new Locale("pt", "BR")); // idioma padrão
        return resolver;
    }

    // Intercepta todas as requisições e muda o idioma quando ?lang=xx está na URL
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang"); // parâmetro da URL: ?lang=en
        return interceptor;
    }

    // Registra o interceptor no ciclo de vida do Spring MVC
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }
}
```

**Como o fluxo funciona:**

```
Usuário clica em "EN"
     ↓
GET /clientes?lang=en
     ↓
LocaleChangeInterceptor lê ?lang=en
     ↓
SessionLocaleResolver salva "en" na sessão do usuário
     ↓
Thymeleaf usa #{chave} com o bundle messages_en.properties
     ↓
Página renderiza em inglês
```

---

### 2.3 Usando `#{chave}` no Thymeleaf

Troque os textos fixos nos templates pela sintaxe `#{chave}`:

```html
<!-- ANTES: texto fixo em português -->
<p class="text-left mb-2">Localizar cliente pelo CPF</p>
<input placeholder="Apenas números (11 dígitos)" .../>
<button type="submit">Localizar</button>

<!-- DEPOIS: chaves i18n -->
<p class="text-left mb-2" th:text="#{cliente.busca.titulo}">Localizar cliente pelo CPF</p>
<input th:placeholder="#{cliente.busca.placeholder}" .../>
<button type="submit" th:text="#{cliente.busca.botao}">Localizar</button>
```

> **Dica:** Mantenha o texto fixo no HTML como fallback visual — ele aparece ao abrir o arquivo diretamente no browser (sem servidor). Isso é o conceito de **Natural Templates** do Thymeleaf.

**Diferença entre `${}` e `#{}`:**

| Sintaxe | O que faz | Exemplo |
|---|---|---|
| `${expr}` | Avalia expressão — lê do model ou objeto | `${username}` → valor do model |
| `#{chave}` | Lê do arquivo de mensagens do idioma atual | `#{nav.clientes}` → "Clientes" ou "Customers" |
| `@{/rota}` | Gera URL respeitando o context-path | `@{/clientes}` → `/clientes` |
| `*{campo}` | Atalho para campo do objeto em `th:object` | `*{cpf}` → `${cliente.cpf}` |

---

### 2.4 Botão de troca de idioma no `fragments.html`

Adicione os botões na navbar, dentro do fragment `navbar`:

```html
<!-- Botões de idioma — adicionar ao lado do menu do usuário -->
<div class="flex space-x-1 mr-4">
    <a th:href="@{''(lang=pt)}"
       class="px-2 py-1 text-xs rounded bg-gray-200 hover:bg-gray-300">PT</a>
    <a th:href="@{''(lang=en)}"
       class="px-2 py-1 text-xs rounded bg-gray-200 hover:bg-gray-300">EN</a>
</div>
```

`@{''(lang=pt)}` é a sintaxe do Thymeleaf para adicionar um query parameter à URL atual: `@{'' (param=valor)}`. O resultado é a mesma página com `?lang=pt` adicionado.

---

## Parte 3 — Autenticação OAuth2 com GitHub

### O que é OAuth2?

**OAuth2** é um protocolo de autorização que permite que uma aplicação acesse recursos de um usuário em outro serviço, **sem que o usuário entregue sua senha** para a aplicação.

No contexto desta aplicação:
- Em vez de criar e manter um sistema próprio de usuários e senhas, delegamos a autenticação ao GitHub.
- O usuário clica em "Entrar com GitHub", é redirecionado para o GitHub, faz login lá, e volta para a aplicação já autenticado.
- Nossa aplicação **nunca vê a senha do GitHub**.

### O fluxo OAuth2 (Authorization Code Grant)

```
[1] Usuário acessa /clientes
[2] Spring Security redireciona para GitHub: GET https://github.com/login/oauth/authorize?...
[3] Usuário faz login no GitHub e autoriza a aplicação
[4] GitHub redireciona de volta: GET /login/oauth2/code/github?code=ABC
[5] Spring Security troca o code por um access token com o GitHub
[6] Spring Security busca os dados do usuário: GET https://api.github.com/user
[7] Usuário está autenticado — Spring cria a sessão com o OAuth2AuthenticationToken
```

### Comparação com outras formas de autenticação

| Abordagem | Vantagem | Desvantagem |
|---|---|---|
| **OAuth2 (GitHub/Google)** | Sem cadastro, sem senha para manter | Depende do provedor externo |
| **Form login (usuário/senha)** | Controle total | Responsável por segurança e recuperação de senha |
| **JWT** | Stateless, bom para APIs | Requer gerenciamento de tokens |
| **SSO corporativo (SAML)** | Integra com AD/LDAP da empresa | Complexo de configurar |

---

### 3.1 Criar o GitHub OAuth App

1. Acesse: **GitHub → Settings → Developer Settings → OAuth Apps → New OAuth App**

2. Preencha:
   - **Application name:** `vendas-ms`
   - **Homepage URL:** `http://localhost:8080`
   - **Authorization callback URL:** `http://localhost:8080/login/oauth2/code/github`

   > A callback URL precisa coincidir **exatamente** com o que o Spring Security espera. O Spring registra automaticamente a rota `/login/oauth2/code/github` para aplicações OAuth2 com GitHub.

3. Clique em **Register application** e anote o **Client ID** e o **Client Secret**.

---

### 3.2 Adicionar as dependências

**Arquivo:** `pom.xml`

Adicione dentro de `<dependencies>`:

```xml
<!-- Spring Security: protege as rotas e gerencia sessões -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- OAuth2 Client: integração com GitHub, Google, etc. -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>
```

> **Atenção:** ao adicionar `spring-boot-starter-security`, o Spring protege **todas** as rotas automaticamente com uma tela de login padrão. Na próxima seção você vai configurar o login via GitHub.

---

### 3.3 Configurar o Spring Security

Crie `src/main/java/br/com/fiap/vendasms/configurations/SecurityConfig.java`:

```java
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated()  // todas as rotas exigem login
            )
            .oauth2Login(Customizer.withDefaults()); // login via OAuth2 (GitHub)
        return http.build();
    }
}
```

**O que cada parte faz:**

- `authorizeHttpRequests`: define quais rotas precisam de autenticação.
- `anyRequest().authenticated()`: qualquer rota não autenticada redireciona para login.
- `oauth2Login(Customizer.withDefaults())`: habilita o fluxo OAuth2 com as configurações do `application.properties`.

---

### 3.4 Configurar as credenciais

**Arquivo:** `src/main/resources/application.properties`

Adicione:

```properties
spring.security.oauth2.client.registration.github.client-id=SEU_CLIENT_ID_AQUI
spring.security.oauth2.client.registration.github.client-secret=SEU_CLIENT_SECRET_AQUI
```

> **Importante — segurança das credenciais:** nunca suba o `client-secret` para o GitHub em texto plano. Uma abordagem segura é usar variáveis de ambiente:
>
> ```properties
> spring.security.oauth2.client.registration.github.client-id=${GITHUB_CLIENT_ID}
> spring.security.oauth2.client.registration.github.client-secret=${GITHUB_CLIENT_SECRET}
> ```
>
> E definir as variáveis no ambiente local antes de rodar a aplicação:
> ```bash
> export GITHUB_CLIENT_ID=seu_id
> export GITHUB_CLIENT_SECRET=seu_secret
> ```

---

### 3.5 Extrair dados do usuário GitHub

Crie `src/main/java/br/com/fiap/vendasms/utils/GithubUserUtils.java`:

```java
public final class GithubUserUtils {

    // Retorna o @login do GitHub (ex: "maria-dev")
    public static String getUsername(OAuth2AuthenticationToken authentication) {
        return authentication.getPrincipal().getAttribute("login");
    }

    // Constrói a URL do avatar a partir do ID numérico do usuário
    public static String getAvatar(OAuth2AuthenticationToken authentication) {
        Integer id = authentication.getPrincipal().getAttribute("id");
        return "https://avatars.githubusercontent.com/u/" + id + "?v=4";
    }
}
```

**O `OAuth2AuthenticationToken`** é o objeto que o Spring Security injeta automaticamente após a autenticação. Ele contém um `OAuth2User` (o `getPrincipal()`) com os atributos retornados pela API do GitHub — incluindo `login`, `id`, `name`, `email`, `avatar_url`, etc.

---

### 3.6 Atualizar o `CommonController`

**Arquivo:** `src/main/java/br/com/fiap/vendasms/controller/CommonController.java`

Substitua os valores fixos pela leitura do token OAuth2:

```java
public abstract class CommonController {

    @ModelAttribute
    public void preProcessor(Model model, OAuth2AuthenticationToken authentication) {
        // authentication é null se o usuário não estiver logado
        if (authentication != null) {
            model.addAttribute("username", GithubUserUtils.getUsername(authentication));
            model.addAttribute("urlAvatar", GithubUserUtils.getAvatar(authentication));
        }
    }
}
```

O Spring injeta o `OAuth2AuthenticationToken` automaticamente como parâmetro do método — o mesmo mecanismo de injeção que funciona com `Model`, `HttpServletRequest`, etc.

**Antes vs depois:**

| | Antes | Depois |
|---|---|---|
| `username` | `"aluno-fiap"` (fixo) | login real do GitHub |
| `urlAvatar` | URL de identicon genérico | foto de perfil real do GitHub |
| Requer login? | Não | Sim (redireciona para GitHub) |

---

### 3.7 Logout

O Spring Security expõe automaticamente o endpoint `POST /logout`. Para criar um botão de logout no template, adicione ao fragment `navbar` em `fragments.html`:

```html
<!-- Botão de logout — dentro do menu do usuário -->
<form th:action="@{/logout}" method="post">
    <button type="submit"
            class="block w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-100">
        Sair
    </button>
</form>
```

> O `method="post"` é obrigatório — o Spring Security rejeita `GET /logout` por segurança (evita que links externos desloguem o usuário sem intenção). O CSRF token é incluído automaticamente pelo Thymeleaf quando o Spring Security está no classpath.

---

## Mapa de implementação

Visão geral do que fazer em cada arquivo:

| Arquivo | O que fazer |
|---|---|
| `dto/ClienteDto.java` | Corrigir ordem dos campos em `from()` |
| `entities/Pedido.java` | Adicionar construtor de conveniência |
| `controller/ClienteController.java` | 3 bugs + adicionar `save()` |
| `controller/PedidoController.java` | Adicionar 3 endpoints + dependências |
| `controller/CommonController.java` | Usar `OAuth2AuthenticationToken` |
| `utils/GithubUserUtils.java` | **Criar** — extrai dados do usuário GitHub |
| `configurations/SecurityConfig.java` | **Criar** — configura Spring Security + OAuth2 |
| `configurations/Internationalization.java` | **Criar** — configura i18n |
| `templates/fragments.html` | Corrigir `${session.username}` + botões de idioma + logout |
| `templates/index.html` | Converter namespace + navbar + username + Lottie |
| `templates/detalhe-cliente.html` | Adicionar namespace + `th:field` + `th:unless` CEP |
| `templates/detalhe-pedidos.html` | Converter namespace + navbar + fragment cliente + `th:each` |
| `templates/novo-pedido.html` | Converter namespace + navbar + fragment cliente + `th:each` select |
| `templates/pedidos.html` | Converter namespace + navbar + remover JS inline |
| `messages.properties` | **Criar** — textos em português |
| `messages_en.properties` | **Criar** — textos em inglês |
| `application.properties` | Adicionar credenciais OAuth2 GitHub |
| `pom.xml` | Adicionar `spring-boot-starter-security` + `spring-boot-starter-oauth2-client` |

---

## Checklist de aprendizado

Use esta lista para verificar se você entendeu os conceitos trabalhados:

**Atividade (pendências da aula anterior)**
- [ ] Sei identificar quando os campos de um record estão sendo passados na ordem errada
- [ ] Entendo por que um método de record que retorna um novo objeto precisa ter seu resultado atribuído
- [ ] Sei a diferença entre `@GetMapping` e `@PostMapping` e sei quando usar cada um
- [ ] Entendo o papel de `extends CommonController` e o que acontece sem ele
- [ ] Sei a diferença entre `${session.x}` (sessão HTTP) e `${x}` (model do Spring MVC) no Thymeleaf
- [ ] Sei substituir o bloco `<nav>` estático por `th:replace` de um fragment
- [ ] Sei usar `th:field="*{campo}"` e entendo o que ele gera automaticamente
- [ ] Consigo usar `th:each` para gerar linhas de tabela a partir de uma lista Java
- [ ] Sei usar `th:if` e `th:unless` para mostrar/ocultar elementos condicionalmente
- [ ] Sei popular um `<select>` com os valores de um enum usando `th:each`

**Internacionalização**
- [ ] Entendo por que separar textos do código é uma boa prática
- [ ] Sei criar `messages.properties` e `messages_en.properties` com as chaves corretas
- [ ] Sei configurar o `LocaleResolver` e o `LocaleChangeInterceptor`
- [ ] Sei usar `#{chave}` no Thymeleaf para exibir textos traduzidos
- [ ] Sei criar botões de troca de idioma com `@{''(lang=xx)}`
- [ ] Entendo a diferença entre `${expr}`, `#{chave}`, `@{/rota}` e `*{campo}`

**OAuth2 com GitHub**
- [ ] Entendo o fluxo OAuth2 (Authorization Code Grant) em alto nível
- [ ] Sei criar um GitHub OAuth App e configurar a callback URL corretamente
- [ ] Sei adicionar as dependências de Spring Security e OAuth2 Client no `pom.xml`
- [ ] Sei configurar o `SecurityFilterChain` com `oauth2Login`
- [ ] Entendo o que é um `OAuth2AuthenticationToken` e como extrair dados do usuário
- [ ] Sei atualizar o `CommonController` para usar dados reais do GitHub
- [ ] Sei criar um botão de logout com `POST /logout`
- [ ] Entendo por que credenciais não devem ser commitadas no repositório
