package com.unifapi.academico.domain.models;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** Regra que derruba sessões abertas quando a senha é trocada ou redefinida. */
class UsuarioTokenTrocaDeSenhaTest {

    private static Instant instanteDe(LocalDateTime dataHora) {
        return dataHora.atZone(ZoneId.systemDefault()).toInstant();
    }

    @Test
    void contaQueNuncaTrocouASenhaNaoInvalidaNenhumToken() {
        Usuario usuario = new Usuario();

        assertThat(usuario.tokenAnteriorATrocaDeSenha(Instant.now().minus(1, ChronoUnit.DAYS))).isFalse();
    }

    @Test
    void tokenEmitidoAntesDaTrocaDeixaDeValer() {
        LocalDateTime troca = LocalDateTime.now();
        Usuario usuario = new Usuario();
        usuario.setSenhaAlteradaEm(troca);

        assertThat(usuario.tokenAnteriorATrocaDeSenha(instanteDe(troca).minusSeconds(5))).isTrue();
    }

    @Test
    void tokenEmitidoDepoisDaTrocaContinuaValendo() {
        LocalDateTime troca = LocalDateTime.now();
        Usuario usuario = new Usuario();
        usuario.setSenhaAlteradaEm(troca);

        assertThat(usuario.tokenAnteriorATrocaDeSenha(instanteDe(troca).plusSeconds(5))).isFalse();
    }

    @Test
    void tokenNovoEmitidoNoMesmoSegundoDaTrocaContinuaValendo() {
        // O "iat" do JWT não tem fração de segundo: o token que a própria troca
        // devolve sai com iat truncado e não pode ser confundido com um antigo.
        LocalDateTime troca = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).plusNanos(700_000_000);
        Usuario usuario = new Usuario();
        usuario.setSenhaAlteradaEm(troca);

        Instant iatDoTokenNovo = instanteDe(troca).truncatedTo(ChronoUnit.SECONDS);

        assertThat(usuario.tokenAnteriorATrocaDeSenha(iatDoTokenNovo)).isFalse();
    }

    @Test
    void semIatNaoInvalida() {
        Usuario usuario = new Usuario();
        usuario.setSenhaAlteradaEm(LocalDateTime.now());

        assertThat(usuario.tokenAnteriorATrocaDeSenha(null)).isFalse();
    }
}
