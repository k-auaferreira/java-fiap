package br.com.fiap.vendasms.external_interface.jms;

public class Entrega {

    public enum Status {
        PENDENTE,
        COLETADO,
        EM_ROTA_ENTREGA,
        ENTREGUE
    }
}
