# Material de suporte da Aula do dia 20/02

**Objetivo:** Implementar a camada web (controllers + Thymeleaf) e a integração com uma API externa (OpenFeign → ViaCEP) partindo do estado atual do projeto.

> **Nota sobre segurança:** O Spring Security fica fora do escopo desta atividade. O `CommonController` usa valores fixos onde, em produção, os dados viriam de um token OAuth2. O padrão de código é idêntico — só a fonte do dado muda.

---

## O que já existe no projeto

Você já tem a base pronta. **Não altere** os arquivos abaixo, pois eles serão utilizados pelo código que você vai criar:

- **Entidades:** `Cliente`, `Pedido` (com enum `Status`)
- **Repositórios:** `ClienteRepository`, `PedidoRepository` (com `findByCliente_Cpf`)
- **Serviços:** `ClienteService`/`ClienteServiceImpl`, `PedidoService`/`PedidoServiceImpl`
- **`pom.xml`:** já inclui Thymeleaf, Spring MVC, Spring Data JPA, OpenFeign e MySQL

## O que você vai construir

```
dto/ClienteDto.java
dto/PedidoDto.java
external_interface/feign/CepDetails.java   ← Java Record
external_interface/feign/CepApi.java       ← @FeignClient
VendasMsApplication.java                   ← adicionar @EnableFeignClients
controller/HomeController.java
controller/CommonController.java           ← classe base abstrata com @ModelAttribute
controller/ClienteController.java          ← reescrever (estava como @RestController vazio)
controller/PedidoController.java
templates/ (6 arquivos HTML Thymeleaf)
application.properties                     ← configuração do datasource + JPA
compose.yaml                               ← MySQL via Docker
```

---

## Parte 1 — Fundação

### 1.1 Getters e Setters nas Entidades

O Thymeleaf precisa dos **getters** para ler os valores de um objeto e exibi-los na tela. O Spring MVC usa os **setters** para preencher objetos automaticamente a partir dos dados enviados pelo formulário HTML. Sem eles, nenhuma das duas coisas funciona.

Abra `Cliente.java` e adicione:

```java
public Cliente(String cpf) { this.cpf = cpf; }

public String getCpf()      { return cpf; }
public String getNome()     { return nome; }
// ... demais getters e setters para todos os campos
```

Faça o mesmo para `Pedido.java`, incluindo um construtor de conveniência:

```java
public Pedido(Cliente cliente, Status status, String descricao) {
    this.cliente = cliente;
    this.status = status;
    this.descricao = descricao;
}

public UUID getId()         { return id; }
public Cliente getCliente() { return cliente; }
// ... demais getters e setters
```

> **Dica:** O Lombok (`@Data`, `@Getter`, `@Setter`) gera tudo isso automaticamente. Aqui escrevemos de forma explícita para entender o que está acontecendo por baixo.

---

### 1.2 DTOs — separando entidade de view

**Por que não usar a entidade diretamente na view?**

A entidade é o contrato com o banco de dados: ela define colunas, relacionamentos e regras de persistência. A view (e a API) tem necessidades diferentes — pode precisar de campos calculados, campos de APIs externas, ou omitir campos sensíveis. O **DTO (Data Transfer Object)** é a camada que separa esses dois mundos.

> **Atenção:** Neste projeto os DTOs são implementados como `record`s — não como classes comuns. Isso os torna imutáveis por padrão. Preste atenção nas implicações disso, especialmente no `enrichWith()`.

Crie `dto/ClienteDto.java`:

```java
public record ClienteDto(
        String cpf,
        String nome,
        String cep,
        String numero,
        String complemento,
        String telefone,
        // Campos enriquecidos pela API ViaCEP
        String logradouro,
        String bairro,
        String localidade,
        String estado
) {
    // Cria um DTO vazio com apenas o CPF preenchido (cliente ainda não cadastrado)
    public static ClienteDto empty(String cpf) {
        return new ClienteDto(cpf, null, null, null, null, null, null, null, null, null);
    }

    // Converte entidade → DTO (sem dados de CEP)
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

    // Records são imutáveis: retorna um NOVO DTO com os campos de CEP preenchidos
    public ClienteDto enrichWith(CepDetails cepDetails) {
        if (cepDetails == null) return this;
        return new ClienteDto(cpf, nome, cep, numero, complemento, telefone,
                cepDetails.logradouro(), cepDetails.bairro(),
                cepDetails.localidade(), cepDetails.estado());
    }

    // Converte DTO → entidade
    public Cliente toEntity() {
        return new Cliente(cpf, nome, cep, numero, complemento, telefone);
    }
}
```

Crie `dto/PedidoDto.java`:

```java
public record PedidoDto(
        UUID id,
        ClienteDto cliente,
        String status,
        String descricao
) {
    // Converte entidade → DTO
    public static PedidoDto from(Pedido pedido) {
        return new PedidoDto(
                pedido.getId(),
                ClienteDto.from(pedido.getCliente()),
                pedido.getStatus().name(),
                pedido.getDescricao()
        );
    }

    // Converte lista de entidades → lista de DTOs
    public static List<PedidoDto> from(List<Pedido> pedidos) {
        return pedidos.stream().map(PedidoDto::from).toList();
    }
}
```

**Padrões utilizados:**

- **`record` como DTO:** assim como `CepDetails`, os DTOs também são records — imutáveis, sem getters verbosos (`cpf()` em vez de `getCpf()`), com `equals`, `hashCode` e `toString` automáticos.
- **`from()` — factory method estático:** cria o DTO a partir da entidade sem expor o construtor diretamente. Centraliza a lógica de conversão em um único lugar.
- **`empty()` — factory method para cliente novo:** quando o CPF digitado não existe no banco, criamos um DTO com apenas o CPF preenchido e o restante `null`. Isso permite reutilizar o mesmo formulário para cadastro e edição.
- **`toEntity()` — caminho inverso:** converte o DTO de volta para entidade, necessário na hora de persistir no banco.
- **`enrichWith()` — imutabilidade em ação:** como records são imutáveis, não é possível alterar os campos existentes. Em vez disso, o método retorna um **novo record** com todos os campos originais mais os dados do CEP. Compare com uma classe comum onde bastaria `this.logradouro = cepDetails.logradouro()` — aqui a imutabilidade força uma abordagem mais explícita e segura.
- **`status.name()`** no `PedidoDto`: o enum `Pedido.Status` é convertido para `String` com `.name()`, que retorna o nome exato do valor do enum (ex: `"PENDENTE"`). Isso facilita a exibição na view sem acoplamento ao tipo enum.

---

### 1.3 Java Record para CepDetails

Crie `external_interface/feign/CepDetails.java`:

```java
public record CepDetails(
    String cep,
    String logradouro,
    String bairro,
    String localidade,
    String estado
) {}
```

**O que é um `record`?**

Introduzido no Java 16, um `record` é uma forma compacta de declarar uma classe imutável. Com uma única linha de declaração, o Java gera automaticamente:
- Construtor com todos os campos
- Getters (sem o prefixo `get` — usa o próprio nome do campo: `cepDetails.logradouro()`)
- `equals()`, `hashCode()` e `toString()`

É ideal para objetos que apenas carregam dados, como respostas de APIs externas.

---

### 1.4 OpenFeign — chamando a API ViaCEP

#### O que é o OpenFeign?

O **Feign** foi criado pela **Netflix** em 2012 para eliminar o código repetitivo de chamadas HTTP entre serviços. Em 2016 o **Spring Cloud** assumiu a manutenção sob o nome **Spring Cloud OpenFeign**, integrando-o ao ecossistema Spring com suporte nativo a injeção de dependência e anotações do Spring MVC.

#### Comparação com outras formas de fazer chamadas HTTP

| | OpenFeign | RestTemplate | WebClient | Retrofit |
|---|---|---|---|---|
| **Estilo** | Declarativo (interface) | Imperativo | Reativo/imperativo | Declarativo (interface) |
| **Boilerplate** | Mínimo | Alto | Médio | Mínimo |
| **Paradigma** | Síncrono | Síncrono | Assíncrono (reactive) | Síncrono |
| **Foco** | Microserviços Spring | Legado Spring | Spring WebFlux | Android / Java geral |
| **Integração Spring MVC** | Nativa | Manual | Nativa | Anotações próprias |

**Quando usar cada um:**
- **OpenFeign** → chamadas entre microsserviços Spring; código limpo e declarativo.
- **WebClient** → aplicações reativas (Spring WebFlux) ou quando I/O assíncrono é crítico.
- **RestTemplate** → projetos legados (está em *maintenance mode* desde o Spring 5).
- **Retrofit** → quando o consumidor é Android ou uma biblioteca Java fora do Spring.

#### Como o Feign funciona por baixo dos panos

Você define apenas uma **interface** — não há implementação. O Spring Cloud gera um **proxy dinâmico** em runtime que traduz cada chamada de método em uma requisição HTTP real. Chamar `cepApi.get("01310100")` é equivalente a:

```java
// O que o Feign faz automaticamente
var response = httpClient.get("https://viacep.com.br/ws/01310100/json/");
return objectMapper.readValue(response.body(), CepDetails.class);
```

#### Implementando a CepApi

Crie `external_interface/feign/CepApi.java`:

```java
@FeignClient(name = "cep-api", url = "https://viacep.com.br/ws")
public interface CepApi {

    @GetMapping("/{cep}/json/")
    CepDetails get(@PathVariable("cep") String cep);
}
```

- `@FeignClient` define a URL base da API.
- `@GetMapping` e `@PathVariable` funcionam exatamente como nos seus controllers Spring MVC.
- Para usar, basta injetar `CepApi` como qualquer outro `@Bean`.

#### Habilitando o Feign na aplicação

Adicione `@EnableFeignClients` em `VendasMsApplication.java`:

```java
@SpringBootApplication
@EnableFeignClients  // Escaneia e registra todas as interfaces @FeignClient
public class VendasMsApplication {
    public static void main(String[] args) {
        SpringApplication.run(VendasMsApplication.class, args);
    }
}
```

---

## Parte 2 — Controllers

### 2.1 HomeController

Crie `controller/HomeController.java`:

```java
@Controller
@RequestMapping("/")
public class HomeController {

    @GetMapping
    public String index() {
        return "index";  // Resolve para templates/index.html
    }
}
```

**`@Controller` vs `@RestController`**

| Anotação | O que o valor retornado representa |
|---|---|
| `@Controller` | **Nome lógico da view** — o Thymeleaf busca `templates/<nome>.html` |
| `@RestController` | **Corpo da resposta HTTP** — serializado como JSON/XML |

Use `@Controller` quando quiser renderizar páginas HTML. Use `@RestController` quando quiser retornar dados para uma API.

---

### 2.2 CommonController — comportamento compartilhado entre controllers

Crie `controller/CommonController.java`:

```java
public abstract class CommonController {

    @ModelAttribute
    public void preProcessor(Model model) {
        model.addAttribute("username", "aluno-fiap");
        model.addAttribute("urlAvatar", "https://github.com/identicons/fiap.png");
    }
}
```

**O que está acontecendo aqui?**

- Um método anotado com `@ModelAttribute` (em vez de um parâmetro) é **executado automaticamente antes de qualquer `@GetMapping` ou `@PostMapping`** do controller.
- Por ser `abstract`, os controllers que herdam `CommonController` recebem esse comportamento sem copiar código — princípio **DRY** (Don't Repeat Yourself).
- O `Model` é um mapa chave→valor que o Thymeleaf recebe para renderizar a view. Tudo que você adicionar com `model.addAttribute("chave", valor)` fica disponível no HTML como `${chave}`.

> Em produção com Spring Security, `username` e `urlAvatar` viriam do `OAuth2AuthenticationToken`. O padrão de código é o mesmo — apenas a fonte do dado muda.

---

### 2.3 ClienteController

Crie `controller/ClienteController.java`:

```java
@Controller
@RequestMapping("/clientes")
public class ClienteController extends CommonController {

    private final ClienteService clienteService;
    private final CepApi cepApi;

    // Injeção por construtor: campos final + construtor = imutável e testável
    public ClienteController(ClienteService clienteService, CepApi cepApi) {
        this.clienteService = clienteService;
        this.cepApi = cepApi;
    }

    @GetMapping
    public String index() { return "cliente"; }

    @PostMapping("/detalhe")
    public String detalhe(@ModelAttribute ClienteDto form, Model model) {
        ClienteDto clienteDto;
        try {
            // Cliente existe → carrega do banco
            var cliente = clienteService.findById(form.getCpf());
            clienteDto = ClienteDto.from(cliente);
        } catch (NoSuchElementException e) {
            // Cliente não existe → cria DTO vazio com o CPF digitado
            clienteDto = new ClienteDto();
            clienteDto.setCpf(form.getCpf());
        }
        // Se o cliente tem CEP, consulta a API ViaCEP para enriquecer o endereço
        if (clienteDto.getCep() != null && !clienteDto.getCep().isBlank()) {
            var cepDetails = cepApi.get(clienteDto.getCep());
            clienteDto.enrichWith(cepDetails);
        }
        model.addAttribute("clienteDto", clienteDto);
        return "detalhe-cliente";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute ClienteDto form) {
        clienteService.saveOrUpdate(form.toEntity());
        return "redirect:/pedidos/detalhe/" + form.getCpf();
    }
}
```

**Conceitos importantes neste controller:**

| Conceito | Onde aparece | O que faz |
|---|---|---|
| `@ModelAttribute` no parâmetro | `detalhe(@ModelAttribute ClienteDto form)` | Spring lê os campos do formulário e popula o DTO automaticamente |
| OpenFeign em uso | `cepApi.get(cep)` | Uma linha para chamar a API externa e obter o endereço completo |
| Padrão "buscar ou criar" | `try/catch` com `NoSuchElementException` | Reutiliza o mesmo endpoint para cadastro e edição de cliente |
| `redirect:` | `return "redirect:/pedidos/..."` | Instrui o browser a fazer um novo `GET`, evitando resubmissão do form ao recarregar a página |
| Injeção por construtor | `private final` + construtor | Melhor prática: mais fácil de testar e garante que as dependências não mudam após a criação |

---

### 2.4 PedidoController

Crie `controller/PedidoController.java`:

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
    public String index() { return "pedidos"; }

    @GetMapping("/detalhe/{cpf}")
    public String detalhe(@PathVariable String cpf, Model model) {
        var pedidos = pedidoService.findByClienteCpf(cpf);
        model.addAttribute("pedidoDtos", PedidoDto.from(pedidos));
        model.addAttribute("cpf", cpf);
        return "detalhe-pedidos";
    }

    @PostMapping("/novo")
    public String novo(@RequestParam String cpf, Model model) {
        model.addAttribute("cpf", cpf);
        model.addAttribute("statuses", Pedido.Status.values());
        return "novo-pedido";
    }

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

**Como capturar dados da requisição — três formas diferentes:**

| Anotação | De onde lê | Exemplo |
|---|---|---|
| `@PathVariable` | Segmento da URL: `/detalhe/{cpf}` | `GET /pedidos/detalhe/12345678900` |
| `@RequestParam` | Parâmetro de query string ou campo de form | `?cpf=12345` ou `<input name="cpf">` |
| `@ModelAttribute` | Múltiplos campos de form mapeados para um objeto | Formulário com vários campos → preenche um DTO inteiro |

O `redirect:` aqui aplica o padrão **PRG (Post/Redirect/Get)**: após salvar, redireciona para a listagem com um `GET`, evitando que o usuário crie pedidos duplicados ao recarregar a página.

---

## Parte 3 — Views com Thymeleaf

### O que é o Thymeleaf?

O **Thymeleaf** é o *template engine* padrão do Spring Boot para renderização de páginas HTML no servidor. Foi criado em 2011 por Daniel Fernández e tornou-se o substituto oficial do JSP a partir do Spring Boot 1.x.

#### O conceito de "Natural Templates"

A característica mais importante do Thymeleaf é que seus atributos `th:*` são **atributos HTML válidos**, simplesmente ignorados pelos browsers quando abertos como arquivo estático. Isso significa que você pode abrir um arquivo `.html` Thymeleaf direto no browser para ver o layout, sem precisar rodar o servidor:

```html
<!-- O browser exibe "Nome de exemplo"; o servidor substitui pelo valor real -->
<p th:text="${cliente.nome}">Nome de exemplo</p>
```

JSP não permite isso — mistura Java com HTML e exige compilação no servidor antes de qualquer visualização.

#### Por que o Spring Boot abandonou o JSP

1. **Empacotamento:** JSP exige deploy como WAR em servidor externo. O Spring Boot usa um *fat jar* executável que não suporta JSP nativamente.
2. **Separação de responsabilidades:** JSP mistura lógica Java no HTML, dificultando o trabalho de designers e desenvolvedores em paralelo.
3. **Ecossistema:** Thymeleaf integra Spring Security, internacionalização (i18n) e form binding sem configuração adicional.

#### Comparação com alternativas

| | Thymeleaf | JSP | FreeMarker | Mustache | React / Angular |
|---|---|---|---|---|---|
| **Natural templates** | Sim | Não | Não | Parcial | N/A |
| **Renderização** | Server-side | Server-side | Server-side | Server-side | Client-side |
| **Integração Spring Boot** | Nativa (auto-config) | Requer WAR | Requer config | Suportado | Arquitetura separada |
| **Curva de aprendizado** | Baixa | Média | Média | Muito baixa | Alta |
| **Recomendado pelo Spring Boot** | Sim | Não (desde SB 1.4) | Alternativa | Alternativa | — |

### Referência rápida dos atributos `th:*`

| Atributo | O que faz |
|---|---|
| `th:text="${expr}"` | Substitui o conteúdo de texto do elemento pelo valor da expressão |
| `th:href="@{/rota}"` | Gera o href correto respeitando o context-path da aplicação |
| `th:action="@{/rota}"` | Define o destino de um formulário |
| `th:object="${obj}"` | Vincula um objeto ao formulário — todos os `*{campo}` referenciam esse objeto |
| `th:field="*{campo}"` | Gera `id`, `name` e `value` automaticamente a partir do objeto em `th:object` |
| `th:each="item : ${lista}"` | Itera sobre uma coleção, repetindo o elemento HTML para cada item |
| `th:if="${cond}"` | Renderiza o elemento somente se a condição for verdadeira |
| `th:unless="${cond}"` | Renderiza o elemento somente se a condição for **falsa** |
| `th:value="${val}"` | Define o atributo `value` do elemento |
| `*{campo}` | Atalho para acessar propriedade do objeto declarado em `th:object` |

---

### 3.1 index.html

Página inicial que exibe o nome do usuário e links de navegação:

```html
<h1>Bem-vindo, <span th:text="${username}">usuario</span>!</h1>
<img th:if="${urlAvatar != null}" th:src="${urlAvatar}" alt="avatar"/>
<a th:href="@{/clientes}">Gerenciar Clientes</a>
<a th:href="@{/pedidos}">Gerenciar Pedidos</a>
```

---

### 3.2 cliente.html — formulário de busca

Formulário simples para buscar um cliente pelo CPF:

```html
<form th:action="@{/clientes/detalhe}" method="post">
    <label>CPF: <input type="text" name="cpf" required/></label>
    <button type="submit">Buscar</button>
</form>
```

---

### 3.3 detalhe-cliente.html — `th:object`, `th:field` e `th:unless`

Formulário de edição com dados do cliente e endereço enriquecido pelo ViaCEP:

```html
<form th:action="@{/clientes/save}" th:object="${clienteDto}" method="post">
    <input type="text" th:field="*{cpf}" readonly/>
    <input type="text" th:field="*{nome}"/>
    <input type="text" th:field="*{cep}"/>

    <!-- Endereço enriquecido pela API ViaCEP — só aparece se o logradouro existir -->
    <div th:unless="${clienteDto.logradouro == null}">
        <p>Logradouro: <strong th:text="*{logradouro}">-</strong></p>
        <p>Bairro: <strong th:text="*{bairro}">-</strong></p>
        <p th:text="*{localidade} + ' / ' + *{estado}">-</p>
    </div>

    <input type="text" th:field="*{numero}"/>
    <input type="text" th:field="*{completo}"/>
    <input type="text" th:field="*{telefone}"/>
    <button type="submit">Salvar</button>
</form>
```

**Por que usar `th:field`?**

`th:field="*{campo}"` é um atalho poderoso: equivale a escrever manualmente `id="campo" name="campo" th:value="*{campo}"`. A sintaxe `*{campo}` usa **seleção relativa** ao objeto declarado em `th:object` — não precisa repetir o nome do objeto em cada campo.

---

### 3.4 detalhe-pedidos.html — `th:each` e `th:if`

Listagem dos pedidos de um cliente com tratamento para lista vazia:

```html
<!-- Exibe mensagem se não houver pedidos -->
<p th:if="${#lists.isEmpty(pedidoDtos)}">Nenhum pedido encontrado.</p>

<!-- Exibe a tabela somente se houver pedidos -->
<table th:unless="${#lists.isEmpty(pedidoDtos)}">
    <tr th:each="pedido : ${pedidoDtos}">
        <td th:text="${pedido.id}">-</td>
        <td th:text="${pedido.descricao}">-</td>
        <td th:text="${pedido.status}">-</td>
        <td th:text="${pedido.cliente.nome}">-</td>
    </tr>
</table>
```

**Expression Utility Objects:** `#lists` é um utilitário de expressão do Thymeleaf que oferece métodos auxiliares para coleções. Outros exemplos úteis: `#strings` (operações com texto), `#dates` (formatação de datas), `#numbers` (formatação numérica).

---

### 3.5 novo-pedido.html — `th:each` em `<option>`

Formulário para criação de pedido com select populado a partir de um enum Java:

```html
<select name="status">
    <option th:each="s : ${statuses}"
            th:value="${s.name()}"
            th:text="${s.name()}">STATUS</option>
</select>
```

O `th:each` funciona em **qualquer elemento HTML**, não só em `<tr>` ou `<li>`. Os valores do enum `Pedido.Status` são passados pelo controller com `model.addAttribute("statuses", Pedido.Status.values())`.

---

## Parte 4 — Infraestrutura

### application.properties

Configure a conexão com o banco de dados e o comportamento do JPA:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/vendasdb
spring.datasource.username=root
spring.datasource.password=root
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.mvc.hiddenmethod.filter.enabled=true
```

> `ddl-auto=update` faz o Hibernate atualizar o schema do banco automaticamente com base nas entidades. Útil em desenvolvimento — em produção use `validate` ou migrations com Flyway/Liquibase.

### compose.yaml

Sobe um container MySQL local com Docker:

```yaml
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: vendasdb
    ports:
      - "3306:3306"
```

### Como rodar o projeto

```bash
# 1. Suba o banco de dados
docker compose up -d

# 2. Inicie a aplicação Spring Boot
./mvnw spring-boot:run

# 3. Acesse no browser
# http://localhost:8080
```

---

## Mapa de Rotas

Visão geral de todas as rotas da aplicação:

| Método | Rota | Controller | View |
|---|---|---|---|
| `GET` | `/` | `HomeController.index` | `index.html` |
| `GET` | `/clientes` | `ClienteController.index` | `cliente.html` |
| `POST` | `/clientes/detalhe` | `ClienteController.detalhe` | `detalhe-cliente.html` |
| `POST` | `/clientes/save` | `ClienteController.save` | redirect → `/pedidos/detalhe/{cpf}` |
| `GET` | `/pedidos` | `PedidoController.index` | `pedidos.html` |
| `GET` | `/pedidos/detalhe/{cpf}` | `PedidoController.detalhe` | `detalhe-pedidos.html` |
| `POST` | `/pedidos/novo` | `PedidoController.novo` | `novo-pedido.html` |
| `POST` | `/pedidos/novo/salvar` | `PedidoController.salvar` | redirect → `/pedidos/detalhe/{cpf}` |

---

## Checklist de aprendizado

Use esta lista para verificar se você entendeu os conceitos trabalhados:

- [ ] Sei a diferença entre `@Controller` e `@RestController`
- [ ] Entendo o uso de `@RequestMapping`, `@GetMapping` e `@PostMapping`
- [ ] Sei quando usar `@PathVariable`, `@RequestParam` e `@ModelAttribute` em parâmetros
- [ ] Entendo como `@ModelAttribute` em um método funciona como pré-processador no `CommonController`
- [ ] Sei usar `Model` para passar dados da camada Java para a view Thymeleaf
- [ ] Entendo o padrão `redirect:` e por que ele evita resubmissão de formulários (PRG)
- [ ] Consigo criar uma hierarquia de controllers usando herança com classe `abstract`
- [ ] Sei criar e usar um `@FeignClient` para consumir uma API externa
- [ ] Entendo o que é um `@EnableFeignClients` e onde colocar
- [ ] Sei criar um Java Record e entendo suas vantagens em relação a uma classe comum
- [ ] Consigo criar DTOs com factory methods (`from()`) e conversão de volta (`toEntity()`)
- [ ] Sei usar `th:text`, `th:href`, `th:action`, `th:object` e `th:field` no Thymeleaf
- [ ] Sei iterar sobre coleções com `th:each` e controlar renderização com `th:if`/`th:unless`
- [ ] Entendo a diferença entre `${expr}` e `*{campo}` no Thymeleaf
- [ ] Sei popular um `<select>` com os valores de um enum via `th:each`
- [ ] Sei configurar o datasource no `application.properties`
- [ ] Consigo subir o banco de dados com `docker compose up -d`
