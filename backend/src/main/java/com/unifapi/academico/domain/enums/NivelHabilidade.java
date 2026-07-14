package com.unifapi.academico.domain.enums;

public enum NivelHabilidade {
    INICIANTE("Iniciante"),
    INTERMEDIARIO("Intermediário"),
    AVANCADO("Avançado"),
    EXPERT("Expert");
    
    private final String label;
    
    NivelHabilidade(String label) {
        this.label = label;
    }
    
    public String getLabel() {
        return label;
    }
}
