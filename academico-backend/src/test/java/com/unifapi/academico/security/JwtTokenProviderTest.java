package com.unifapi.academico.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes unitários (sem contexto Spring) para {@link JwtTokenProvider}.
 *
 * <p>O provider recebe {@code secret} e {@code expirationMillis} diretamente pelo
 * construtor (os valores que, em produção, o Spring injeta via {@code @Value}), então
 * não é necessário usar {@code ReflectionTestUtils} aqui — basta instanciar a classe
 * passando um segredo de teste e uma expiração curta.
 */
class JwtTokenProviderTest {

    private static final String SEGREDO_TESTE =
            "segredo-de-teste-para-jwt-com-tamanho-suficiente-para-hmac-sha256-0123456789";

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        // Expiração de 5 minutos: suficiente para os testes de token válido.
        jwtTokenProvider = new JwtTokenProvider(SEGREDO_TESTE, 5 * 60 * 1000L);
    }

    @Test
    void deveGerarTokenDeAcessoValidoParaOTipoAccess() {
        UUID userId = UUID.randomUUID();

        String token = jwtTokenProvider.gerarTokenAcesso(userId, "ALUNO");

        assertThat(jwtTokenProvider.validarToken(token, "ACCESS")).isTrue();
    }

    @Test
    void tokenDeAcessoNaoDeveSerValidoParaTipoResetPassword() {
        UUID userId = UUID.randomUUID();

        String token = jwtTokenProvider.gerarTokenAcesso(userId, "ALUNO");

        assertThat(jwtTokenProvider.validarToken(token, "RESET_PASSWORD")).isFalse();
    }

    @Test
    void deveGerarTokenDeResetDeSenhaValidoParaValidarTokenReset() {
        UUID userId = UUID.randomUUID();

        String token = jwtTokenProvider.gerarTokenResetSenha(userId);

        assertThat(jwtTokenProvider.validarTokenReset(token)).isTrue();
        assertThat(jwtTokenProvider.validarToken(token, "RESET_PASSWORD")).isTrue();
    }

    @Test
    void tokenDeResetDeSenhaNaoDeveSerValidoParaTipoAccess() {
        UUID userId = UUID.randomUUID();

        String token = jwtTokenProvider.gerarTokenResetSenha(userId);

        assertThat(jwtTokenProvider.validarToken(token, "ACCESS")).isFalse();
    }

    @Test
    void validarTokenDeveRetornarFalsoParaTokenMalformadoSemLancarExcecao() {
        String tokenInvalido = "isto-nao-e-um-jwt-valido";

        boolean resultado = jwtTokenProvider.validarToken(tokenInvalido, "ACCESS");

        assertThat(resultado).isFalse();
    }

    @Test
    void validarTokenDeveRetornarFalsoParaTokenVazio() {
        assertThat(jwtTokenProvider.validarToken("", "ACCESS")).isFalse();
    }

    @Test
    void validarTokenDeveRetornarFalsoParaTokenExpirado() throws InterruptedException {
        // Provider configurado com expiração de 1ms: o token já estará expirado
        // poucos milissegundos após ser gerado.
        JwtTokenProvider providerComExpiracaoCurta = new JwtTokenProvider(SEGREDO_TESTE, 1L);
        UUID userId = UUID.randomUUID();

        String token = providerComExpiracaoCurta.gerarTokenAcesso(userId, "ALUNO");
        Thread.sleep(20);

        assertThat(providerComExpiracaoCurta.validarToken(token, "ACCESS")).isFalse();
    }

    @Test
    void validarTokenComAssinaturaDeSegredoDiferenteDeveRetornarFalso() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.gerarTokenAcesso(userId, "ALUNO");

        JwtTokenProvider providerComOutroSegredo = new JwtTokenProvider(
                "outro-segredo-completamente-diferente-com-tamanho-suficiente-para-hmac-sha256",
                5 * 60 * 1000L
        );

        assertThat(providerComOutroSegredo.validarToken(token, "ACCESS")).isFalse();
    }

    @Test
    void obterSubjectDoTokenDeveRetornarOUserIdUsadoNaGeracao() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.gerarTokenAcesso(userId, "ALUNO");

        assertThat(jwtTokenProvider.obterSubjectDoToken(token)).isEqualTo(userId);
    }

    @Test
    void obterRoleDoTokenDeveRetornarARoleUsadaNaGeracao() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.gerarTokenAcesso(userId, "PROFESSOR");

        assertThat(jwtTokenProvider.obterRoleDoToken(token)).isEqualTo("PROFESSOR");
    }

    @Test
    void obterRoleDoTokenDeveRetornarNuloParaTokenDeResetDeSenha() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.gerarTokenResetSenha(userId);

        assertThat(jwtTokenProvider.obterRoleDoToken(token)).isNull();
    }

    @Test
    void obterDataExpiracaoDoTokenDeveRetornarValorNaoNulo() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenProvider.gerarTokenAcesso(userId, "ALUNO");

        assertThat(jwtTokenProvider.obterDataExpiracaoDoToken(token)).isNotNull();
    }

    @Test
    void obterSubjectDoTokenDeveLancarExcecaoParaTokenMalformado() {
        // Diferente de validarToken, obterSubjectDoToken não captura a exceção
        // lançada pela biblioteca jjwt ao tentar fazer parse de um token inválido.
        String tokenInvalido = "isto-nao-e-um-jwt-valido";

        assertThatThrownBy(() -> jwtTokenProvider.obterSubjectDoToken(tokenInvalido))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void obterRoleDoTokenDeveLancarExcecaoParaTokenMalformado() {
        String tokenInvalido = "isto-nao-e-um-jwt-valido";

        assertThatThrownBy(() -> jwtTokenProvider.obterRoleDoToken(tokenInvalido))
                .isInstanceOf(JwtException.class);
    }
}
