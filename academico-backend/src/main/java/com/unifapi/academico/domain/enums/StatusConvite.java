package com.unifapi.academico.domain.enums;

public enum StatusConvite {
    PENDENTE("Pendente"),
    ACEITO("Aceito"),
    RECUSADO("Recusado"),
    CANCELADO("Cancelado");

    private final String label;

    StatusConvite(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
