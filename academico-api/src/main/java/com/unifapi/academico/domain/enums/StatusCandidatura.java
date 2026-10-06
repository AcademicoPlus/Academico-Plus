package com.unifapi.academico.domain.enums;

public enum StatusCandidatura {
    PENDENTE("Pendente"),
    ACEITO("Aceito"),
    REJEITADO("Rejeitado");
    
    private final String label;
    
    StatusCandidatura(String label) {
        this.label = label;
    }
    
    public String getLabel() {
        return label;
    }
}
