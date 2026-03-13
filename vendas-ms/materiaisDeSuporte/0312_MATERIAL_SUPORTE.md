# Material de Suporte — O que é um Microserviço

**Objetivo:** Entender o que é uma arquitetura de microserviços, por que ela existe, quais problemas ela resolve (e quais ela cria), e os principais conceitos que você precisa conhecer para trabalhar com ela.

> **Contexto no projeto:** O `vendas-ms` que você está construindo **é** um microserviço. O sufixo `-ms` não é coincidência — é a abreviação de *microservice*. Ao final deste material você vai entender o que isso significa e quais são as implicações.

---

## Parte 1 — O ponto de partida: o Monólito

Antes de entender microserviços, é preciso entender o que existia antes: a arquitetura **monolítica**.

### O que é um monólito?

Um **monólito** é uma aplicação em que todas as funcionalidades — clientes, pedidos, pagamentos, estoque, notificações — são desenvolvidas, implantadas e executadas como **uma única unidade**.

```mermaid
graph TB
    subgraph jar["loja-app.jar"]
        C[Clientes]
        P[Pedidos]
        PAG[Pagamentos]
        E[Estoque]
        U[Usuários]
        N[Notificações]
    end
    C & P & PAG & E & U & N --> DB[(MySQL)]
```

Você sobe **um** processo. Tudo compartilha a mesma memória, o mesmo banco de dados, o mesmo processo de build e deploy.

### Problemas do monólito em escala

| Problema | O que acontece na prática |
|---|---|
| **Deploy acoplado** | Para corrigir um bug no módulo de clientes, é preciso fazer deploy da aplicação inteira |
| **Escalabilidade engessada** | Se o módulo de pedidos precisa de mais CPU, você escala a aplicação inteira, incluindo módulos que não precisam |
| **Time bomb tecnológica** | Toda a aplicação usa a mesma versão de Java, Spring, Hibernate — atualizar uma dependência afeta tudo |
| **Times que se bloqueiam** | Dois times trabalhando em módulos diferentes na mesma base de código criam conflitos de merge e coordenação constante |
| **Falha catastrófica** | Um vazamento de memória no módulo de relatórios pode derrubar os pedidos e os pagamentos |

> **Atenção:** monólito **não é errado**. Para equipes pequenas e aplicações jovens, ele é frequentemente a escolha certa. Os problemas acima surgem com o crescimento. Esta é a regra geral: comece simples, decomponha quando a dor justificar o custo.

---

## Parte 2 — Microserviços: a solução (e seus trade-offs)

### O que é um microserviço?

Um **microserviço** é uma aplicação pequena e autônoma que:
1. É responsável por **um domínio específico** do negócio (clientes, pedidos, pagamentos…)
2. É **implantada independentemente** dos outros serviços
3. **Se comunica pela rede** com os outros serviços (HTTP/REST, mensagens assíncronas)
4. Possui seu **próprio banco de dados** — não compartilha dados diretamente com outros serviços

```mermaid
graph TB
    GW[API Gateway]
    GW --> CMS[clientes-ms]
    GW --> PMS[pedidos-ms]
    GW --> PAMS[pagamentos-ms]
    CMS --> DB1[(MySQL)]
    PMS --> DB2[(MySQL)]
    PAMS --> DB3[(PostgreSQL)]
```

O `vendas-ms` que você está construindo é o serviço `pedidos-ms` desse diagrama: responsável por clientes e pedidos de vendas, implantado de forma independente, com seu próprio banco de dados MySQL.

### Comparação: monólito vs microserviços

| Aspecto | Monólito | Microserviços |
|---|---|---|
| **Deploy** | Uma unidade, tudo junto | Cada serviço implantado independentemente |
| **Escalabilidade** | Toda a aplicação | Apenas os serviços que precisam |
| **Banco de dados** | Compartilhado | Um por serviço |
| **Comunicação interna** | Chamada de método (na memória) | HTTP ou mensageria (pela rede) |
| **Falha isolada** | Um módulo pode derrubar tudo | Falha de um serviço não necessariamente afeta os outros |
| **Complexidade** | Baixa operacionalmente | Alta — requer orquestração, service discovery, observabilidade |
| **Recomendado para** | Times pequenos, produto em validação | Produtos maduros, times grandes, escala |

---

## Parte 3 — Conceitos fundamentais

### 3.1 Domínio e Bounded Context

O conceito de **domínio** vem do **Domain-Driven Design (DDD)**: é a área do negócio que o software modela. Um microserviço deve ser responsável por um **Bounded Context** — uma fronteira clara dentro do domínio onde um conjunto de conceitos tem um significado único e consistente.

**Exemplo prático:**

A palavra "cliente" pode significar coisas diferentes em contextos diferentes:
- Para o serviço de **vendas**: cliente é quem fez um pedido (CPF, nome, endereço de entrega)
- Para o serviço de **marketing**: cliente é um lead com histórico de campanhas e score de engajamento
- Para o serviço de **financeiro**: cliente é um devedor com limite de crédito e histórico de pagamentos

Cada serviço tem sua **própria definição** de cliente, adequada ao seu contexto. Eles não compartilham a mesma tabela `clientes` — cada um tem a sua.

```mermaid
graph LR
    subgraph domain["Domínio: E-commerce"]
        subgraph vendas["Bounded Context: Vendas"]
            CV["Cliente\n─────────\ncpf\nendereço\npedidos"]
        end
        subgraph marketing["Bounded Context: Marketing"]
            CM["Cliente\n─────────\nemail\nscore\ncampanhas"]
        end
        subgraph financeiro["Bounded Context: Financeiro"]
            CF["Cliente\n─────────\nlimite de crédito\nhistórico de pgto"]
        end
    end
```

> **Regra prática:** se dois serviços precisam dos mesmos dados, eles podem se comunicar via API — mas cada um armazena os dados que lhe interessam em seu próprio banco.

---

### 3.2 API Gateway

Com múltiplos serviços rodando em endereços diferentes, o cliente (browser, app mobile) não pode conhecer o endereço de cada um. O **API Gateway** é o ponto de entrada único que:

- Recebe todas as requisições do cliente
- Roteia cada requisição para o serviço correto
- Pode aplicar autenticação, rate limiting e logging de forma centralizada

```mermaid
graph TD
    Client["Browser / App Mobile"]
    GW["API Gateway\n(porta 80/443)"]
    Client --> GW
    GW -->|"/clientes/**"| CMS["clientes-ms:8081"]
    GW -->|"/pedidos/**"| PMS["pedidos-ms:8082"]
    GW -->|"/pagamentos/**"| PAMS["pagamentos-ms:8083"]
```

**Exemplos de API Gateways:**
- **Spring Cloud Gateway** — para ecossistemas Spring Boot
- **Kong** — independente de linguagem, altamente configurável
- **AWS API Gateway** — gerenciado na nuvem AWS
- **NGINX** — proxy reverso que pode funcionar como gateway simples

---

### 3.3 Comunicação entre serviços

Quando serviços precisam se falar, existem dois modelos:

#### Comunicação síncrona (HTTP/REST)

Um serviço chama o outro diretamente e **espera a resposta** antes de continuar.

```mermaid
sequenceDiagram
    pedidos-ms->>clientes-ms: GET /clientes/{cpf}
    clientes-ms-->>pedidos-ms: 200 {cliente}
```

**Ferramentas:** REST com `RestTemplate`, `WebClient`, ou **OpenFeign** (que você já usou para chamar o ViaCEP).

**Vantagem:** simples de implementar e debugar.
**Desvantagem:** se `clientes-ms` está fora do ar, `pedidos-ms` também falha. Os serviços ficam **temporariamente acoplados**.

#### Comunicação assíncrona (mensageria)

Um serviço publica uma mensagem em um **broker** (fila/tópico) e continua. O outro serviço consome a mensagem quando estiver disponível.

```mermaid
graph LR
    PMS[pedidos-ms] -->|"publica: PedidoCriado"| BROKER["Kafka / RabbitMQ"]
    BROKER -->|consome| EMS[estoque-ms]
    BROKER -->|consome| NMS[notificacoes-ms]
    BROKER -->|consome| FMS[financeiro-ms]
```

**Ferramentas:** **Apache Kafka**, **RabbitMQ**, **AWS SQS/SNS**

**Vantagem:** desacoplamento total — se `estoque-ms` cair, o pedido ainda é criado; quando ele voltar, o evento ainda estará na fila.
**Desvantagem:** mais complexo de implementar, testar e monitorar. A consistência dos dados é **eventual**, não imediata.

| | Síncrona (REST) | Assíncrona (Mensageria) |
|---|---|---|
| **Modelo** | Request/Response | Publish/Subscribe |
| **Acoplamento temporal** | Alto — emissor espera o receptor | Baixo — emissor não precisa do receptor ativo |
| **Consistência** | Imediata | Eventual |
| **Quando usar** | Quando precisa da resposta para continuar | Quando a operação pode ser processada depois |
| **Exemplo** | Buscar dados de um cliente | Notificar estoque após criar pedido |

---

### 3.4 Service Discovery

Em um ambiente com muitos serviços e instâncias que sobem e caem dinamicamente (especialmente em containers), os endereços IP não são fixos. O **Service Discovery** resolve isso:

- Cada serviço ao iniciar se **registra** em um servidor central com seu nome e endereço
- Quando um serviço quer chamar outro, ele **consulta** o servidor para descobrir o endereço atual

```mermaid
sequenceDiagram
    participant CMS as clientes-ms
    participant SR as Service Registry (Eureka/Consul)
    participant PMS as pedidos-ms

    CMS->>SR: registra "clientes-ms" em 10.0.0.12:8081
    PMS->>SR: qual o endereço de clientes-ms?
    SR-->>PMS: 10.0.0.12:8081
    PMS->>CMS: GET /clientes/{cpf}
```

**Ferramentas:** **Netflix Eureka** (Spring Cloud), **Consul**, **Kubernetes Service** (o Kubernetes tem service discovery nativo).

---

### 3.5 Circuit Breaker (Disjuntor)

Imagine que `pagamentos-ms` está lento: cada chamada demora 10 segundos antes de expirar. `pedidos-ms` faz 100 chamadas simultâneas e trava todas as suas threads esperando respostas que nunca chegam. O resultado: `pedidos-ms` também fica indisponível — o problema se **propaga** em cascata.

O **Circuit Breaker** funciona como um disjuntor elétrico:

```mermaid
stateDiagram-v2
    direction LR
    [*] --> Fechado
    Fechado --> Aberto : N falhas consecutivas
    Aberto --> SemiAberto : timeout expira
    SemiAberto --> Fechado : chamada de teste OK
    SemiAberto --> Aberto : chamada de teste falha

    Fechado: FECHADO\nchamadas passam normalmente
    Aberto: ABERTO\nretorna fallback imediatamente
    SemiAberto: SEMI-ABERTO\ntesta com uma chamada
```

**Ferramentas:** **Resilience4j** (padrão atual no Spring), **Hystrix** (legado da Netflix, em manutenção).

```java
// Exemplo com Resilience4j no Spring Boot
@CircuitBreaker(name = "pagamentos", fallbackMethod = "pagamentoFallback")
public PagamentoDto processarPagamento(PedidoDto pedido) {
    return pagamentosClient.processar(pedido);
}

public PagamentoDto pagamentoFallback(PedidoDto pedido, Exception e) {
    // Resposta alternativa: coloca na fila para processar depois
    return PagamentoDto.pendente(pedido.getId());
}
```

---

### 3.6 Database per Service

Um dos princípios mais importantes — e mais difíceis de seguir — é que cada microserviço deve ter seu **próprio banco de dados**, acessível apenas por ele.

**ERRADO — banco compartilhado:**

```mermaid
graph LR
    CMS[clientes-ms] --> DB[(MySQL compartilhado)]
    PMS[pedidos-ms] --> DB
    EMS[estoque-ms] --> DB
```

Uma migration no `clientes-ms` pode quebrar o `estoque-ms`. O `pedidos-ms` pode ler dados "internos" do `clientes-ms` diretamente. Não é possível escalar os bancos independentemente.

**CORRETO — banco por serviço:**

```mermaid
graph LR
    CMS[clientes-ms] --> DB1[(MySQL\nclientes)]
    PMS[pedidos-ms] --> DB2[(MySQL\npedidos)]
    EMS[estoque-ms] --> DB3[(PostgreSQL\nestoque)]
```

Cada serviço pode inclusive usar um banco de dados diferente, adequado às suas necessidades.

**Consequência:** para obter dados de outro serviço, você precisa chamá-lo pela API — nunca acessar seu banco diretamente. Isso garante o encapsulamento e a independência real entre os serviços.

---

### 3.7 Containerização com Docker

Antes dos containers, implantar um serviço significava configurar o servidor: instalar o Java na versão certa, configurar variáveis de ambiente, lidar com conflitos entre aplicações. Em um ambiente com dezenas de microserviços, isso é inviável.

O **Docker** resolve isso empacotando a aplicação e todas as suas dependências em uma **imagem** portátil:

```dockerfile
# Dockerfile para o vendas-ms
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/vendas-ms.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```bash
# Construir a imagem
docker build -t vendas-ms:1.0 .

# Rodar o container
docker run -p 8080:8080 -e SPRING_DATASOURCE_URL=jdbc:mysql://... vendas-ms:1.0
```

**Conceitos-chave do Docker:**

| Conceito | Analogia | O que é |
|---|---|---|
| **Dockerfile** | Receita | Instruções para construir a imagem |
| **Image** | Classe | Template imutável com o código + dependências |
| **Container** | Instância | Processo em execução criado a partir de uma imagem |
| **Registry** | NPM/Maven Central | Repositório de imagens (Docker Hub, ECR, GCR) |
| **compose.yaml** | `docker-compose` | Orquestra múltiplos containers localmente |

> Você já usa o Docker no projeto: o `compose.yaml` que sobe o MySQL é o Docker Compose. O banco de dados do `vendas-ms` roda em um container Docker.

---

### 3.8 Orquestração com Kubernetes

O Docker Compose é ótimo para desenvolvimento local, mas em produção você precisa de mais: escalar automaticamente, reiniciar serviços que caem, distribuir carga entre instâncias, fazer deploys sem downtime.

O **Kubernetes (K8s)** é o orquestrador de containers mais usado em produção:

```mermaid
graph TB
    subgraph cluster["Kubernetes Cluster"]
        subgraph n1["Node 1"]
            p1["pod: vendas-ms (réplica 1)"]
            p3["pod: clientes-ms"]
        end
        subgraph n2["Node 2"]
            p2["pod: vendas-ms (réplica 2)"]
            p4["pod: pagamentos-ms"]
        end
    end
```

**Conceitos básicos do Kubernetes:**

| Conceito | O que é |
|---|---|
| **Pod** | A menor unidade — um ou mais containers rodando juntos |
| **Deployment** | Define quantas réplicas de um pod devem existir e como atualizá-las |
| **Service** | Endereço estável para acessar um conjunto de pods (service discovery nativo) |
| **Ingress** | Ponto de entrada externo — funciona como o API Gateway para o cluster |
| **ConfigMap / Secret** | Variáveis de configuração e credenciais separadas do código |
| **Namespace** | Agrupamento lógico de recursos dentro do cluster |

> Para o contexto da FIAP, o importante é entender que o Kubernetes é a plataforma onde microserviços rodam em produção. O `compose.yaml` local é o equivalente simplificado para desenvolvimento.

---

### 3.9 Observabilidade

Com dezenas de serviços, quando algo dá errado, como você descobre **onde** está o problema? A **observabilidade** é composta por três pilares:

```mermaid
graph LR
    subgraph obs["Observabilidade"]
        L["Logs\n─────────────\nO quê aconteceu\ne quando\n─────────────\nELK Stack / Loki"]
        M["Métricas\n─────────────\nQuantos / quão\nrápido\n─────────────\nPrometheus / Grafana"]
        T["Rastreamento\n─────────────\nQual caminho uma\nrequisição percorreu\n─────────────\nJaeger / OpenTelemetry"]
    end
```

#### Distributed Tracing

É o conceito mais crítico para microserviços: uma requisição do usuário pode passar por 5 serviços diferentes. Como rastrear o fluxo completo?

A solução é um **Trace ID** — um identificador único gerado na entrada que é propagado em todos os headers HTTP ao longo da cadeia:

```mermaid
sequenceDiagram
    actor U as Usuário
    participant P as pedidos-ms
    participant C as clientes-ms
    participant E as estoque-ms
    participant PAG as pagamentos-ms

    U->>P: Criar pedido [trace-id: abc-123]
    P->>C: Buscar cliente [abc-123]
    C-->>P: cliente encontrado
    P->>E: Verificar disponibilidade [abc-123]
    E-->>P: estoque disponível
    P->>PAG: Processar pagamento [abc-123]
    PAG-->>P: pagamento aprovado
    P-->>U: pedido criado
```

Com o `trace-id`, você busca em um sistema centralizado de logs e vê **toda** a jornada de uma requisição, mesmo que ela tenha passado por múltiplos serviços.

**Ferramentas:** **OpenTelemetry** (padrão aberto), **Jaeger**, **Zipkin**, **AWS X-Ray**.

---

### 3.10 Consistência eventual e o Teorema CAP

Em um sistema distribuído, quando você grava dados em um serviço, eles **não aparecem imediatamente** em todos os outros serviços — especialmente com comunicação assíncrona.

**Consistência eventual** significa: o sistema **vai** atingir um estado consistente, mas pode não ser imediato.

```mermaid
sequenceDiagram
    actor U as Usuário
    participant P as pedidos-ms
    participant B as Kafka
    participant E as estoque-ms

    U->>P: criar pedido
    P->>P: grava no banco ✓
    P->>B: publica evento PedidoCriado
    P-->>U: 201 Created

    Note over E: t=1s — evento ainda na fila
    Note over E: pedido existe em pedidos-ms ✓
    Note over E: estoque-ms ainda não processou ✗

    B->>E: entrega evento PedidoCriado
    E->>E: reserva estoque ✓

    Note over P,E: t=5s — consistência eventual atingida ✓
```

**O Teorema CAP** afirma que um sistema distribuído pode garantir no máximo **dois** dos três:

| Propriedade | O que significa |
|---|---|
| **Consistency (C)** | Toda leitura vê a escrita mais recente |
| **Availability (A)** | Toda requisição recebe uma resposta (mesmo que não seja a mais recente) |
| **Partition tolerance (P)** | O sistema continua funcionando mesmo com falhas de rede entre os nós |

> Na prática, em sistemas distribuídos a falha de rede (P) é inevitável — você precisa tolerá-la. Então a escolha real é entre **CP** (consistência + tolerância, sacrifica disponibilidade) ou **AP** (disponibilidade + tolerância, sacrifica consistência imediata). A maioria dos microserviços escolhe **AP** e aceita consistência eventual.

---

## Parte 4 — O `vendas-ms` no contexto de microserviços

Agora que você conhece os conceitos, veja como o projeto se encaixa:

| Conceito | Como aparece no `vendas-ms` |
|---|---|
| **Microserviço** | A aplicação inteira é um serviço com escopo definido: gestão de vendas |
| **Bounded Context** | Clientes e pedidos no contexto de vendas — com seus próprios campos e regras |
| **Banco por serviço** | MySQL próprio, configurado no `compose.yaml` e `application.properties` |
| **Comunicação síncrona** | OpenFeign chamando a API ViaCEP — um serviço externo |
| **Containerização** | O MySQL roda em Docker; a aplicação pode ser containerizada com um Dockerfile |
| **Autenticação delegada** | OAuth2 com GitHub — autenticação como serviço externo (sem gerenciar senhas) |
| **Controle de acesso** | Spring Security + `ROLE_PEDIDO` — autorização dentro do serviço |

### O que falta para ser um microserviço de produção?

O `vendas-ms` atual é um microserviço em desenvolvimento. Para estar pronto para produção em um ecossistema distribuído, precisaria de:

- **API Gateway** — para ser exposto externamente junto com outros serviços
- **Service Discovery** — para que outros serviços possam encontrá-lo dinamicamente
- **Circuit Breaker** — para lidar com falhas de serviços externos (ViaCEP, futuros serviços internos)
- **Distributed Tracing** — para rastrear requisições em um sistema com múltiplos serviços
- **Dockerfile** — para ser implantado em qualquer ambiente de forma consistente
- **Health check endpoint** — `/actuator/health` para que o orquestrador saiba se está vivo

---

## Quando NÃO usar microserviços

Microserviços não são bala de prata. Eles resolvem problemas que surgem com **escala** — de produto, de times, de tráfego. Antes dessa escala, eles introduzem complexidade sem benefício proporcional.

**Sinais de que o monólito ainda é a escolha certa:**

- O produto ainda está sendo validado no mercado (não faz sentido otimizar o que pode mudar completamente)
- O time tem menos de ~8 pessoas (o overhead operacional de microserviços consome tempo de desenvolvimento)
- As fronteiras de domínio ainda não estão claras (um microserviço mal dividido é pior que um monólito)

**Sinais de que pode ser hora de decompor:**

- Times diferentes estão bloqueados uns pelos outros para fazer deploys
- Uma parte da aplicação precisa de escalabilidade muito diferente das outras
- Partes do sistema precisam de tecnologias radicalmente diferentes (ex: ML em Python, API em Java)
- O processo de build e teste já demora mais de 20-30 minutos

> **Estratégia comum:** começar com um monólito bem estruturado (com módulos bem separados internamente) e extrair microserviços conforme a necessidade aparece. Isso é chamado de **"Strangler Fig Pattern"** — o monólito vai sendo gradualmente substituído pelos serviços.

---

## Parte 5 — Migrations de banco de dados com Flyway

### O problema que o Flyway resolve

O projeto usa `spring.jpa.hibernate.ddl-auto=update` para criar e atualizar as tabelas automaticamente com base nas entidades Java. Isso parece conveniente, mas é perigoso:

| Situação | `ddl-auto=update` | Com Flyway |
|---|---|---|
| **Nova coluna adicionada** | Adiciona — mas nunca remove colunas antigas | Executa o `ALTER TABLE` exatamente como escrito |
| **Coluna renomeada** | Adiciona a nova, deixa a antiga — dados perdidos | Você controla: `RENAME COLUMN` explícito |
| **Em produção com dados reais** | Qualquer erro de schema pode corromper dados | Transação SQL: ou tudo funciona ou nada muda |
| **Rastreabilidade** | Nenhuma — ninguém sabe quem mudou o que | Histórico completo com versão, autor e data |
| **Múltiplos desenvolvedores** | Race condition — dois devs mudam a entidade ao mesmo tempo | Cada um cria um script versionado, sem conflito |
| **Ambientes diferentes** | Dev e prod podem divergir silenciosamente | O mesmo script roda em todos os ambientes |

> **Regra de ouro:** `ddl-auto=update` (ou `create`) é aceitável em desenvolvimento local. Em qualquer ambiente compartilhado — homologação, produção — use `none` com Flyway.

---

### Como o Flyway funciona

O Flyway controla quais scripts já foram executados através de uma tabela chamada `flyway_schema_history`, que ele cria automaticamente no primeiro uso.

```mermaid
sequenceDiagram
    participant App as Aplicação (Spring Boot)
    participant FW as Flyway
    participant DB as Banco de Dados

    App->>FW: inicia (antes do contexto Spring subir)
    FW->>DB: verifica se flyway_schema_history existe
    DB-->>FW: não existe → cria a tabela
    FW->>DB: lê scripts em db/migration/
    FW->>DB: verifica quais versões já foram aplicadas
    DB-->>FW: V1 e V2 já aplicados; V3 pendente
    FW->>DB: executa V3__create_usuario.sql
    DB-->>FW: sucesso
    FW-->>App: schema atualizado — pode continuar subindo
```

**O que o Flyway guarda em `flyway_schema_history`:**

| installed_rank | version | description | script | checksum | success |
|---|---|---|---|---|---|
| 1 | 1 | create cliente | V1__create_cliente.sql | 1234567890 | true |
| 2 | 2 | create pedido | V2__create_pedido.sql | 987654321 | true |
| 3 | 3 | create usuario | V3__create_usuario.sql | 1122334455 | true |

O **checksum** é um hash do conteúdo do arquivo. Se alguém editar um script já aplicado, o Flyway detecta a inconsistência e **recusa a subir a aplicação** — isso é intencional e evita que alguém "corrija" silenciosamente um script já executado em produção.

---

### Convenção de nomenclatura dos arquivos

```
V{versão}__{descrição}.sql
│  │        │
│  │        └── Descrição em snake_case (palavras separadas por _)
│  └─────────── Duplo underscore obrigatório
└────────────── Prefixo V maiúsculo para migrations versionadas
```

**Exemplos válidos:**

```
V1__create_cliente.sql
V2__create_pedido.sql
V3__create_usuario.sql
V4__add_apelido_to_cliente.sql      ← ALTER TABLE
V5__seed_admin_user.sql             ← INSERT (dados iniciais)
V6__rename_completo_to_complemento.sql
```

**Outros tipos de script (menos comuns):**

| Prefixo | Nome | Quando usar |
|---|---|---|
| `V` | Versioned | A grande maioria — scripts que rodam uma vez |
| `R` | Repeatable | Scripts que re-rodam sempre que o conteúdo muda (ex: views, stored procedures) |
| `U` | Undo | Desfaz uma migration (requer Flyway Teams, versão paga) |

---

### Implementação no projeto

#### Passo 1 — Dependência no `pom.xml`

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>
```

> **Por que `flyway-mysql` e não `flyway-core`?** O `flyway-core` é incluído transitivamente pelo Spring Boot. O `flyway-mysql` adiciona o suporte específico ao MySQL 8, necessário desde o Flyway 9. Sem ele, a aplicação não sobe com MySQL moderno.

#### Passo 2 — Atualizar `application.properties`

```properties
# ANTES — Hibernate criava/atualizava o schema
spring.jpa.hibernate.ddl-auto=update

# DEPOIS — Hibernate não toca no schema; Flyway é o responsável
spring.jpa.hibernate.ddl-auto=none
```

O Flyway é detectado automaticamente pelo Spring Boot (auto-configuration). Não é necessário nenhuma anotação ou `@Bean` adicional. As propriedades de conexão (`spring.datasource.*`) já configuradas são reutilizadas.

#### Passo 3 — Criar os scripts em `db/migration/`

O diretório padrão que o Flyway lê é `src/main/resources/db/migration/`. Não é necessário configurar — é a convenção.

**`V1__create_cliente.sql`**

```sql
CREATE TABLE cliente (
    cpf      VARCHAR(255) NOT NULL,
    nome     VARCHAR(255),
    cep      VARCHAR(255),
    numero   VARCHAR(255),
    completo VARCHAR(255),
    telefone VARCHAR(255),
    PRIMARY KEY (cpf)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**`V2__create_pedido.sql`**

```sql
CREATE TABLE pedido (
    id         BINARY(16)   NOT NULL,
    descricao  VARCHAR(255),
    status     VARCHAR(255),
    cliente_id VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT fk_pedido_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (cpf)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**`V3__create_usuario.sql`**

```sql
CREATE TABLE usuario (
    login VARCHAR(255) NOT NULL,
    PRIMARY KEY (login)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE usuarios_roles (
    login VARCHAR(255) NOT NULL,
    role  VARCHAR(255),
    CONSTRAINT fk_roles_usuario FOREIGN KEY (login) REFERENCES usuario (login)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**Por que `BINARY(16)` para o id do pedido?**

O campo `id` na entidade `Pedido` é do tipo `UUID` com `@GeneratedValue(strategy = GenerationType.UUID)`. O Hibernate 6 (usado pelo Spring Boot 3.x) armazena UUIDs no MySQL como `BINARY(16)` — 16 bytes em formato binário, mais eficiente que `VARCHAR(36)` com hifens. O Flyway não sabe disso; quem precisa saber é você ao escrever o DDL.

---

### Estrutura final de arquivos

```
src/main/resources/
├── application.properties
├── messages.properties
├── messages_en.properties
├── db/
│   └── migration/
│       ├── V1__create_cliente.sql
│       ├── V2__create_pedido.sql
│       └── V3__create_usuario.sql
└── templates/
    └── ...
```

---

### Cenários para testar em sala

#### Cenário 1 — Primeiro start: Flyway cria tudo

```bash
# 1. Garantir banco limpo (remove o volume existente)
docker compose down -v
docker compose up -d

# 2. Subir a aplicação
./mvnw spring-boot:run
```

Logs esperados na inicialização:
```
Flyway Community Edition ... by Redgate
Database: jdbc:mysql://localhost:3306/vendasdb (MySQL 8.0)
Successfully validated 3 migrations (execution time 00:00.020s)
Current version of schema `vendasdb`: << Empty Schema >>
Migrating schema `vendasdb` to version "1 - create cliente"
Migrating schema `vendasdb` to version "2 - create pedido"
Migrating schema `vendasdb` to version "3 - create usuario"
Successfully applied 3 migrations to schema `vendasdb` (execution time 00:00.150s)
```

Verificar no banco:
```sql
-- Ver o histórico de migrations aplicadas
SELECT version, description, success, installed_on
FROM flyway_schema_history
ORDER BY installed_rank;

-- Ver as tabelas criadas
SHOW TABLES;
```

---

#### Cenário 2 — Nova migration: adicionar coluna

Simule a adição do campo `apelido` à tabela de clientes.

Crie `V4__add_apelido_to_cliente.sql`:

```sql
ALTER TABLE cliente
    ADD COLUMN apelido VARCHAR(100);
```

Reinicie a aplicação sem derrubar o banco. O Flyway detecta que V1, V2 e V3 já foram aplicados e executa apenas V4:

```
Current version of schema `vendasdb`: 3
Migrating schema `vendasdb` to version "4 - add apelido to cliente"
Successfully applied 1 migration (execution time 00:00.050s)
```

---

#### Cenário 3 — O que acontece ao editar um script já aplicado

Edite `V1__create_cliente.sql` — qualquer mudança, como um comentário:

```sql
-- comentário adicionado depois
CREATE TABLE cliente ( ...
```

Reinicie a aplicação. O Flyway calcula o checksum do arquivo e compara com o salvo em `flyway_schema_history`. Eles diferem — a aplicação **recusa subir**:

```
FlywayException: Validate failed:
Detected failed migration to version 1 (create cliente).
Migration checksum mismatch for migration version 1.
  -> Applied to database: 1234567890
  -> Resolved locally:    9876543210
```

**Por que isso é bom?** Em produção, ninguém pode "corrigir" silenciosamente um script que já rodou. A única forma de desfazer é criar uma nova migration (V5 que reverta o V4, por exemplo).

Para continuar durante o desenvolvimento (apenas!):
```bash
./mvnw flyway:repair -Dflyway.url=jdbc:mysql://... -Dflyway.user=root -Dflyway.password=root
```

> `repair` atualiza o checksum no banco para corresponder ao arquivo atual. **Nunca use isso em produção.**

---

#### Cenário 4 — Migration de dados: seed do usuário admin

Além de DDL (CREATE, ALTER), migrations podem conter DML (INSERT, UPDATE).

Crie `V5__seed_admin_user.sql` para inserir automaticamente um usuário com role de admin ao subir o sistema pela primeira vez:

```sql
-- Substitua 'seu-login-github' pelo login real antes de commitar
INSERT IGNORE INTO usuario (login) VALUES ('seu-login-github');

INSERT IGNORE INTO usuarios_roles (login, role)
VALUES ('seu-login-github', 'ROLE_PEDIDO'),
       ('seu-login-github', 'ROLE_CLIENTE_EDIT');
```

> `INSERT IGNORE` evita erro se o registro já existir. Útil em migrations de seed que podem rodar em bancos que já têm dados.

Agora a primeira pessoa a subir o sistema em um novo ambiente já tem acesso completo — sem precisar executar SQL manual.

---

#### Cenário 5 — Banco com dados existentes: baseline

Você acabou de adicionar o Flyway a um projeto que **já tem o banco criado** pelo `ddl-auto=update`. O Flyway tenta criar as tabelas de V1 e falha porque elas já existem.

Solução: informar ao Flyway que o estado atual do banco já está na versão 3 (ou seja, V1, V2 e V3 estão implicitamente aplicadas):

```properties
# application.properties — apenas para o primeiro start em banco já existente
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=3
```

O Flyway registra as versões 1, 2 e 3 como já aplicadas sem executar os scripts. A partir daí, apenas versões novas (V4+) serão executadas normalmente.

**Remova essas propriedades** após o primeiro start bem-sucedido em ambientes novos — elas não são necessárias (e podem ser perigosas) depois.

---

### `ddl-auto`: comparativo de valores

| Valor | O que faz | Quando usar |
|---|---|---|
| `create` | Dropa e recria o schema a cada start | Testes unitários isolados |
| `create-drop` | Cria ao subir, dropa ao encerrar | Testes de integração em memória |
| `update` | Adiciona colunas/tabelas faltando (nunca remove) | Desenvolvimento local rápido |
| `validate` | Verifica se o schema bate com as entidades — falha se não bater | Com Flyway, para garantir consistência |
| `none` | Não faz nada — Flyway é o único responsável | **Produção e ambientes compartilhados** |

---

## Glossário rápido

| Termo | Significado |
|---|---|
| **ms** | Abreviação de *microservice* |
| **API Gateway** | Ponto de entrada único que roteia requisições para os serviços corretos |
| **Service Discovery** | Mecanismo para serviços se encontrarem dinamicamente em vez de usar IPs fixos |
| **Circuit Breaker** | Padrão que interrompe chamadas a serviços com falha para evitar cascata |
| **Eventual Consistency** | O sistema atingirá consistência, mas não necessariamente de forma imediata |
| **Bounded Context** | Fronteira de domínio dentro da qual um conjunto de conceitos tem significado único |
| **Broker** | Intermediário de mensagens (Kafka, RabbitMQ) usado na comunicação assíncrona |
| **Pod** | Menor unidade implantável no Kubernetes — um ou mais containers |
| **Trace ID** | Identificador propagado entre serviços para rastrear uma requisição de ponta a ponta |
| **Sidecar** | Container auxiliar implantado junto com o serviço principal (ex: proxy de observabilidade) |
| **DDD** | Domain-Driven Design — abordagem de design que organiza o código em torno do domínio |
| **CAP** | Teorema: um sistema distribuído não pode garantir Consistência, Disponibilidade e Tolerância a partições simultaneamente |

---

## Checklist de aprendizado

**Microserviços**
- [ ] Sei explicar a diferença entre arquitetura monolítica e microserviços
- [ ] Entendo por que o monólito não é necessariamente errado — e quando ele é a escolha certa
- [ ] Sei o que é um Bounded Context e por que ele define as fronteiras de um microserviço
- [ ] Entendo o princípio "database per service" e suas consequências
- [ ] Sei a diferença entre comunicação síncrona (REST) e assíncrona (mensageria) e quando usar cada uma
- [ ] Entendo o que é um API Gateway e por que ele existe
- [ ] Sei o que é Service Discovery e por que IPs fixos não funcionam em ambientes dinâmicos
- [ ] Entendo o padrão Circuit Breaker e o problema de cascata que ele resolve
- [ ] Sei o que é Docker e a diferença entre imagem e container
- [ ] Sei o que é Kubernetes e qual o papel de um Pod, Deployment e Service
- [ ] Entendo os três pilares da observabilidade: logs, métricas e rastreamento distribuído
- [ ] Sei o que é consistência eventual e o Teorema CAP
- [ ] Consigo identificar o `vendas-ms` como um microserviço e localizar os conceitos aprendidos no projeto
- [ ] Sei quando microserviços fazem sentido e quando o monólito ainda é a escolha certa

**Flyway**
- [ ] Entendo por que `ddl-auto=update` é perigoso em ambientes compartilhados e produção
- [ ] Sei qual é a convenção de nomenclatura dos scripts (`V{versão}__{descrição}.sql`)
- [ ] Sei adicionar a dependência `flyway-mysql` ao `pom.xml` e mudar o `ddl-auto` para `none`
- [ ] Entendo o que a tabela `flyway_schema_history` armazena e para que serve o checksum
- [ ] Consigo criar migrations de DDL (`CREATE TABLE`, `ALTER TABLE`) e de DML (`INSERT`)
- [ ] Entendo o que acontece quando um script já aplicado é editado (checksum mismatch)
- [ ] Sei usar `baseline-on-migrate` para integrar Flyway a um banco já existente
- [ ] Sei a diferença entre os valores `none`, `validate` e `update` do `ddl-auto`
