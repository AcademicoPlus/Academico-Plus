package com.unifapi.academico.domain.enums;

/**
 * Escada de medidas que o admin aplica ao resolver uma denúncia, da mais
 * branda à mais severa. É cumulativa de cima para baixo: advertir e suspender
 * também derrubam o conteúdo denunciado — se o conteúdo justifica punir o
 * autor, não faz sentido deixá-lo no ar.
 *
 * Quem escolhe é sempre o admin; o serviço só sugere (ver
 * DenunciaService.sugerirMedida), porque a gravidade varia demais entre um
 * spam leve e um golpe para um limiar automático decidir sozinho.
 */
public enum MedidaModeracao {

    /** Denúncia improcedente: nada acontece com o alvo. */
    ARQUIVAR("Arquivar (improcedente)", false, false),

    /** Procedente: o conteúdo sai do ar, o autor não é punido. */
    REMOVER_CONTEUDO("Remover conteúdo", true, false),

    /** Procedente: conteúdo fora do ar e advertência registrada ao responsável. */
    ADVERTENCIA("Advertir o responsável", true, true),

    /** Procedente: conteúdo fora do ar e acesso suspenso por prazo determinado. */
    SUSPENSAO("Suspender o responsável", true, true);

    private final String label;
    private final boolean removeConteudo;
    private final boolean puneResponsavel;

    MedidaModeracao(String label, boolean removeConteudo, boolean puneResponsavel) {
        this.label = label;
        this.removeConteudo = removeConteudo;
        this.puneResponsavel = puneResponsavel;
    }

    public String getLabel() {
        return label;
    }

    public boolean removeConteudo() {
        return removeConteudo;
    }

    public boolean puneResponsavel() {
        return puneResponsavel;
    }

    /** ARQUIVAR é a única que fecha a denúncia como improcedente. */
    public boolean ehProcedente() {
        return this != ARQUIVAR;
    }
}
