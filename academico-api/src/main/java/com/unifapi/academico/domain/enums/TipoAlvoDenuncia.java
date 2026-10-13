package com.unifapi.academico.domain.enums;

/** O que está sendo denunciado. Define qual FK de Denuncia vem preenchida. */
public enum TipoAlvoDenuncia {
    AVALIACAO("Avaliação"),
    PROJETO("Projeto"),
    PERFIL("Perfil");

    private final String label;

    TipoAlvoDenuncia(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
