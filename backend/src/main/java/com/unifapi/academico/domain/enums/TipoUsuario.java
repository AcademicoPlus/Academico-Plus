package com.unifapi.academico.domain.enums;

public enum TipoUsuario {
    ALUNO("Aluno"),
    STUDENT("Aluno"),
    PROFESSOR("Professor");
    
    private final String label;
    
    TipoUsuario(String label) {
        this.label = label;
    }
    
    public String getLabel() {
        return label;
    }
}