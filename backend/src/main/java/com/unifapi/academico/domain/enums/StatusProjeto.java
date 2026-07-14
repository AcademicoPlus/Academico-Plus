package com.unifapi.academico.domain.enums;

public enum StatusProjeto {
    ABERTO("Aberto"),
    EM_ANDAMENTO("Em Andamento"),
    CONCLUIDO("Concluído"),
    CANCELADO("Cancelado");
    
    private final String label;
    
    StatusProjeto(String label) {
        this.label = label;
    }
    
    public String getLabel() {
        return label;
    }
}
