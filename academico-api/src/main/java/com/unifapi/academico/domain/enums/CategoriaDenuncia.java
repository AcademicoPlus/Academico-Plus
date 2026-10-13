package com.unifapi.academico.domain.enums;

/**
 * Motivo em categoria fechada, para o admin conseguir triar a fila sem ler
 * cada texto. O relato livre continua existindo em Denuncia.motivo.
 */
public enum CategoriaDenuncia {
    SPAM("Spam ou divulgação indevida"),
    CONTEUDO_OFENSIVO("Conteúdo ofensivo ou discriminatório"),
    INFORMACAO_FALSA("Informação falsa ou enganosa"),
    GOLPE("Golpe ou tentativa de fraude"),
    OUTRO("Outro");

    private final String label;

    CategoriaDenuncia(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
