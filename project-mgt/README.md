# HATEOAS: Vantagens vs Desvantagens

## 📖 O que é HATEOAS?
HATEOAS (*Hypermedia as the Engine of Application State*) é uma restrição da arquitetura REST que permite que um cliente navegue dinamicamente pela API por meio de links fornecidos nas respostas.

---

## ✅ Vantagens

| Vantagem | Descrição |
|----------|-----------|
| **Descoberta dinâmica** | O cliente pode navegar pela API sem conhecer todas as rotas de antemão. |
| **Menor acoplamento** | Mudanças nos endpoints não quebram o cliente, desde que os links permaneçam corretos. |
| **Melhor semântica** | Aproxima as APIs dos princípios verdadeiros do REST. |
| **Auto-documentação** | As respostas indicam ao cliente quais ações estão disponíveis sem depender apenas de documentação externa. |

---

## ⚠️ Desvantagens

| Desvantagem | Descrição |
|-------------|-----------|
| **Aumento da complexidade no servidor** | Os links precisam ser gerados dinamicamente de acordo com o estado do recurso. |
| **Payloads mais pesados** | Cada resposta carrega metadados extras (links, métodos, descrições). |
| **Curva de aprendizado** | Nem todos os desenvolvedores conhecem ou entendem bem o HATEOAS. |
| **Menos usado em APIs modernas** | Muitas equipes preferem OpenAPI/Swagger com rotas fixas. |
| **Pouco aplicável fora do REST** | Não se aplica a GraphQL, gRPC ou outros estilos de API. |
| **Menos controle para o cliente** | O cliente pode precisar seguir a navegação indicada pelo servidor em vez de chamar endpoints diretamente. |

---

## 📜 Exemplo sem HATEOAS
```json
{
  "id": 1,
  "name": "Tiago",
  "email": "tiago@example.com"
}
```

## 📜 Exemplo com HATEOAS
```json
{
  "id": 1,
  "name": "Tiago",
  "email": "tiago@example.com",
  "_links": {
    "self": { "href": "/users/1" },
    "orders": { "href": "/users/1/orders" },
    "update": { "href": "/users/1", "method": "PUT" }
  }
}
```
