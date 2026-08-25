package com.unifapi.academico.security;

import com.unifapi.academico.exception.RegraDeNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes unitários (sem contexto Spring) para {@link RateLimiterService}.
 *
 * <p>A classe não tem dependências a injetar — apenas um {@code ConcurrentHashMap}
 * interno usado como estado. Uma instância nova é criada em cada teste (via
 * {@code @BeforeEach}) para garantir isolamento entre os casos.
 *
 * <p>Os testes cobrem apenas a lógica de contagem/limite/reset, sem depender da
 * expiração real de 15 minutos das janelas — não há necessidade (nem seria viável)
 * de aguardar esse tempo em um teste unitário.
 */
class RateLimiterServiceTest {

    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        rateLimiterService = new RateLimiterService();
    }

    @Test
    void verificarDisponivelNaoDeveLancarParaChaveNuncaVista() {
        assertThatCode(() -> rateLimiterService.verificarDisponivel("chave-nunca-usada@teste.com"))
                .doesNotThrowAnyException();
    }

    @Test
    void apos5FalhasVerificarDisponivelDeveLancarRegraDeNegocioException() {
        String chave = "usuario@teste.com";

        for (int i = 0; i < 5; i++) {
            rateLimiterService.registrarFalha(chave);
        }

        assertThatThrownBy(() -> rateLimiterService.verificarDisponivel(chave))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Muitas tentativas. Tente novamente mais tarde.");
    }

    @Test
    void apos4FalhasVerificarDisponivelNaoDeveLancarAinda() {
        String chave = "usuario@teste.com";

        for (int i = 0; i < 4; i++) {
            rateLimiterService.registrarFalha(chave);
        }

        assertThatCode(() -> rateLimiterService.verificarDisponivel(chave))
                .doesNotThrowAnyException();
    }

    @Test
    void registrarSucessoDeveZerarContadorDeFalhasDaChave() {
        String chave = "usuario@teste.com";

        for (int i = 0; i < 4; i++) {
            rateLimiterService.registrarFalha(chave);
        }
        rateLimiterService.registrarSucesso(chave);

        // Após o reset, mais 4 falhas (abaixo do limite de 5) não devem bloquear.
        for (int i = 0; i < 4; i++) {
            rateLimiterService.registrarFalha(chave);
        }

        assertThatCode(() -> rateLimiterService.verificarDisponivel(chave))
                .doesNotThrowAnyException();
    }

    @Test
    void registrarSucessoNaoDeveImpedirBloqueioSeNovasFalhasAtingiremOLimite() {
        String chave = "usuario@teste.com";

        for (int i = 0; i < 4; i++) {
            rateLimiterService.registrarFalha(chave);
        }
        rateLimiterService.registrarSucesso(chave);

        // Após o reset, 5 novas falhas devem bloquear normalmente, contadas do zero.
        for (int i = 0; i < 5; i++) {
            rateLimiterService.registrarFalha(chave);
        }

        assertThatThrownBy(() -> rateLimiterService.verificarDisponivel(chave))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void chavesComDiferencaDeCaixaEEspacosDevemSerTratadasComoAMesmaChave() {
        String chaveComEspacosEMaiuscula = "Teste@Email.com ";
        String chaveNormalizada = "teste@email.com";

        for (int i = 0; i < 5; i++) {
            rateLimiterService.registrarFalha(chaveComEspacosEMaiuscula);
        }

        // A mesma chave, normalizada de forma diferente, deve estar bloqueada também.
        assertThatThrownBy(() -> rateLimiterService.verificarDisponivel(chaveNormalizada))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void registrarSucessoComChaveNormalizadaDiferenteDeveLimparOBloqueio() {
        String chaveComEspacosEMaiuscula = "Teste@Email.com ";
        String chaveNormalizada = "teste@email.com";

        for (int i = 0; i < 5; i++) {
            rateLimiterService.registrarFalha(chaveComEspacosEMaiuscula);
        }

        rateLimiterService.registrarSucesso(chaveNormalizada);

        assertThatCode(() -> rateLimiterService.verificarDisponivel(chaveComEspacosEMaiuscula))
                .doesNotThrowAnyException();
    }

    @Test
    void chavesDiferentesDevemTerContadoresIndependentes() {
        String chaveA = "usuario-a@teste.com";
        String chaveB = "usuario-b@teste.com";

        for (int i = 0; i < 5; i++) {
            rateLimiterService.registrarFalha(chaveA);
        }

        assertThatThrownBy(() -> rateLimiterService.verificarDisponivel(chaveA))
                .isInstanceOf(RegraDeNegocioException.class);
        assertThatCode(() -> rateLimiterService.verificarDisponivel(chaveB))
                .doesNotThrowAnyException();
    }
}
