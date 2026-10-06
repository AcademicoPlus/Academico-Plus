package com.unifapi.academico.domain.enums;

public enum StatusDenuncia {
    PENDENTE("Pendente"),
    PROCEDENTE("Procedente"),
    IMPROCEDENTE("Improcedente");

    private final String label;

    StatusDenuncia(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
