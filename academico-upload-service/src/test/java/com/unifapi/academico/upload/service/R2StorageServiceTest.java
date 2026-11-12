package com.unifapi.academico.upload.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class R2StorageServiceTest {

    private static final String PUBLIC_URL = "https://pub-abc.r2.dev";
    private static final String UUID_GERADO = "3f2b8c1e-9a4d-4e6f-8b2a-1c3d5e7f9a0b";

    @Test
    void deveAceitarUrlGeradaPeloProprioServico() {
        String url = PUBLIC_URL + "/perfis/" + UUID_GERADO + ".png";

        assertThat(R2StorageService.extrairChaveGerenciada(url, PUBLIC_URL, "perfis/"))
                .isEqualTo("perfis/" + UUID_GERADO + ".png");
    }

    @Test
    void deveRecusarChaveCruaSemUrlPublica() {
        assertThat(R2StorageService.extrairChaveGerenciada(
                "perfis/" + UUID_GERADO + ".png", PUBLIC_URL, "perfis/")).isNull();
    }

    @Test
    void deveRecusarUrlDeOutroDominio() {
        String url = "https://evil.example/perfis/" + UUID_GERADO + ".png";

        assertThat(R2StorageService.extrairChaveGerenciada(url, PUBLIC_URL, "perfis/")).isNull();
    }

    @Test
    void deveRecusarDominioQueApenasComecaComAUrlPublica() {
        String url = PUBLIC_URL + ".evil.example/perfis/" + UUID_GERADO + ".png";

        assertThat(R2StorageService.extrairChaveGerenciada(url, PUBLIC_URL, "perfis/")).isNull();
    }

    @Test
    void deveRecusarCapaDeProjetoPeloEndpointDeFotos() {
        String url = PUBLIC_URL + "/projetos/" + UUID_GERADO + ".png";

        assertThat(R2StorageService.extrairChaveGerenciada(url, PUBLIC_URL, "perfis/")).isNull();
    }

    @Test
    void deveRecusarNomeQueNaoEhUuidGerado() {
        assertThat(R2StorageService.extrairChaveGerenciada(
                PUBLIC_URL + "/perfis/qualquer-coisa.png", PUBLIC_URL, "perfis/")).isNull();
    }

    @Test
    void deveRecusarBarraDepoisDoPrefixo() {
        String url = PUBLIC_URL + "/perfis/" + UUID_GERADO + "/../../projetos/x.png";

        assertThat(R2StorageService.extrairChaveGerenciada(url, PUBLIC_URL, "perfis/")).isNull();
    }

    @Test
    void deveRecusarQuandoUrlPublicaNaoConfigurada() {
        String url = PUBLIC_URL + "/perfis/" + UUID_GERADO + ".png";

        assertThat(R2StorageService.extrairChaveGerenciada(url, "", "perfis/")).isNull();
    }
}
