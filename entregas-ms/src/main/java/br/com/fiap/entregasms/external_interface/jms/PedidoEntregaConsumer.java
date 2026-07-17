package br.com.fiap.entregasms.external_interface.jms;

import br.com.fiap.entregasms.models.Entrega;
import br.com.fiap.entregasms.services.EntregaService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PedidoEntregaConsumer {

    private final EntregaService entregaService;
    private final ObjectMapper objectMapper;

    public PedidoEntregaConsumer(EntregaService entregaService, ObjectMapper objectMapper) {
        this.entregaService = entregaService;
        this.objectMapper = objectMapper;
    }

    @JmsListener(destination = "pedido.queue")
    @Transactional
    public void consume(String message) throws JsonProcessingException {
        final MessageInput input = this.objectMapper.readValue(message,MessageInput.class);
        this.entregaService.save(new Entrega(input.getId(),input.getCliente().getNome(),input.getCliente().getEnderecoCompleto()));
    }

    private static final class MessageInput {
        private UUID id;
        private ClienteMessageInput cliente;

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public ClienteMessageInput getCliente() {
            return cliente;
        }

        public void setCliente(ClienteMessageInput cliente) {
            this.cliente = cliente;
        }

        private static final class ClienteMessageInput {
            private String nome, cep, numero, logradouro, bairro, localidade, estado, complemento;

            public String getEnderecoCompleto(){
                return logradouro.concat(", ").concat(numero).concat("\n")
                        .concat(complemento).concat("\n")
                        .concat(bairro).concat(" - ").concat(localidade).concat(", ").concat(estado)
                        .concat("\nCEP: ").concat(cep);
            }



            public String getNome() {
                return nome;
            }


            public String getCep() {
                return cep;
            }

            public String getNumero() {
                return numero;
            }

            public String getLogradouro() {
                return logradouro;
            }

            public String getBairro() {
                return bairro;
            }

            public String getLocalidade() {
                return localidade;
            }

            public String getEstado() {
                return estado;
            }

            public String getComplemento() {
                return complemento;
            }

            public void setNome(String nome) {
                this.nome = nome;
            }

            public void setCep(String cep) {
                this.cep = cep;
            }

            public void setNumero(String numero) {
                this.numero = numero;
            }

            public void setLogradouro(String logradouro) {
                this.logradouro = logradouro;
            }

            public void setBairro(String bairro) {
                this.bairro = bairro;
            }

            public void setLocalidade(String localidade) {
                this.localidade = localidade;
            }

            public void setEstado(String estado) {
                this.estado = estado;
            }

            public void setComplemento(String complemento) {
                this.complemento = complemento;
            }
        }
    }
}
