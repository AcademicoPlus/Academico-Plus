package com.unifapi.academico.support;

import com.unifapi.academico.domain.enums.CategoriaDenuncia;
import com.unifapi.academico.domain.enums.NivelHabilidade;
import com.unifapi.academico.domain.enums.StatusCandidatura;
import com.unifapi.academico.domain.enums.StatusProjeto;
import com.unifapi.academico.domain.enums.TipoAlvoDenuncia;
import com.unifapi.academico.domain.models.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Builders mínimos e válidos de entidades JPA para uso em testes (unitários com
 * Mockito e slice tests). Generaliza o helper {@code novoUsuario} já usado nos
 * smoke tests de repositório para todas as entidades do domínio.
 *
 * Ids são atribuídos manualmente (não dependem de persistência), o que é seguro
 * porque equals/hashCode dessas entidades são baseados em id (Lombok
 * {@code @EqualsAndHashCode(onlyExplicitlyIncluded = true)}).
 */
public final class EntityFixtures {

    private EntityFixtures() {
    }

    public static NivelAcesso nivelAcesso(String nome) {
        return NivelAcesso.builder()
                .id(UUID.randomUUID())
                .nome(nome)
                .descricao(nome)
                .build();
    }

    public static Curso curso(String nome) {
        return Curso.builder()
                .id(UUID.randomUUID())
                .nome(nome)
                .sigla(nome.substring(0, Math.min(3, nome.length())).toUpperCase())
                .ativo(true)
                .criadoEm(LocalDateTime.now())
                .build();
    }

    public static Usuario usuario(String email, NivelAcesso nivelAcesso) {
        return usuario(email, "Usuario Teste", nivelAcesso, null);
    }

    public static Usuario usuario(String email, String nome, NivelAcesso nivelAcesso, Curso curso) {
        return Usuario.builder()
                .id(UUID.randomUUID())
                .email(email)
                .nome(nome)
                .senhaHash("$2a$10$hashDeTeste")
                .nivelAcesso(nivelAcesso)
                .curso(curso)
                .ativo(true)
                .totalAvaliacoes(0)
                .termosAceitosEm(LocalDateTime.now())
                .criadoEm(LocalDateTime.now())
                .build();
    }

    public static Habilidade habilidade(String nome) {
        return habilidade(nome, "Backend");
    }

    public static Habilidade habilidade(String nome, String categoria) {
        return Habilidade.builder()
                .id(UUID.randomUUID())
                .nome(nome)
                .categoria(categoria)
                .usuariosCount(0)
                .projetosCount(0)
                .criadoEm(LocalDateTime.now())
                .build();
    }

    public static Projeto projeto(Usuario criador, int vagas) {
        return projeto(criador, "Projeto de teste", vagas, StatusProjeto.ABERTO);
    }

    public static Projeto projeto(Usuario criador, String titulo, int vagas, StatusProjeto status) {
        return Projeto.builder()
                .id(UUID.randomUUID())
                .criador(criador)
                .titulo(titulo)
                .descricao("Descricao com pelo menos vinte caracteres para passar na validacao.")
                .status(status)
                .vagas(vagas)
                .vagasPreenchidas(0)
                .ativo(true)
                .criadoEm(LocalDateTime.now())
                .atualizadoEm(LocalDateTime.now())
                .build();
    }

    public static ProjetoHabilidade projetoHabilidade(Projeto projeto, Habilidade habilidade, boolean obrigatoria) {
        return ProjetoHabilidade.builder()
                .id(UUID.randomUUID())
                .projeto(projeto)
                .habilidade(habilidade)
                .obrigatoria(obrigatoria)
                .build();
    }

    public static ProjetoMembro projetoMembro(Projeto projeto, Usuario usuario, String funcao) {
        return ProjetoMembro.builder()
                .id(UUID.randomUUID())
                .projeto(projeto)
                .usuario(usuario)
                .funcao(funcao)
                .dataAdesao(LocalDateTime.now())
                .build();
    }

    public static UsuarioHabilidade usuarioHabilidade(Usuario usuario, Habilidade habilidade, NivelHabilidade nivel) {
        return UsuarioHabilidade.builder()
                .id(UUID.randomUUID())
                .usuario(usuario)
                .habilidade(habilidade)
                .nivel(nivel == null ? NivelHabilidade.INICIANTE : nivel)
                .criadoEm(LocalDateTime.now())
                .build();
    }

    public static Candidatura candidatura(Projeto projeto, Usuario usuario, StatusCandidatura status) {
        return Candidatura.builder()
                .id(UUID.randomUUID())
                .projeto(projeto)
                .usuario(usuario)
                .status(status == null ? StatusCandidatura.PENDENTE : status)
                .dataCandidatura(LocalDateTime.now())
                .build();
    }

    public static Avaliacao avaliacao(Projeto projeto, Usuario avaliador, Usuario avaliado, int nota) {
        return Avaliacao.builder()
                .id(UUID.randomUUID())
                .projeto(projeto)
                .avaliador(avaliador)
                .avaliado(avaliado)
                .nota(nota)
                .removida(false)
                .criadoEm(LocalDateTime.now())
                .build();
    }

    public static Denuncia denuncia(Avaliacao avaliacao, Usuario denunciante, String motivo) {
        return Denuncia.builder()
                .id(UUID.randomUUID())
                .tipoAlvo(TipoAlvoDenuncia.AVALIACAO)
                .avaliacao(avaliacao)
                .denunciante(denunciante)
                .categoria(CategoriaDenuncia.CONTEUDO_OFENSIVO)
                .motivo(motivo)
                .criadoEm(LocalDateTime.now())
                .build();
    }

    public static Denuncia denunciaDeProjeto(Projeto projeto, Usuario denunciante, String motivo) {
        return Denuncia.builder()
                .id(UUID.randomUUID())
                .tipoAlvo(TipoAlvoDenuncia.PROJETO)
                .projeto(projeto)
                .denunciante(denunciante)
                .categoria(CategoriaDenuncia.SPAM)
                .motivo(motivo)
                .criadoEm(LocalDateTime.now())
                .build();
    }

    public static Denuncia denunciaDePerfil(Usuario denunciado, Usuario denunciante, String motivo) {
        return Denuncia.builder()
                .id(UUID.randomUUID())
                .tipoAlvo(TipoAlvoDenuncia.PERFIL)
                .usuarioDenunciado(denunciado)
                .denunciante(denunciante)
                .categoria(CategoriaDenuncia.CONTEUDO_OFENSIVO)
                .motivo(motivo)
                .criadoEm(LocalDateTime.now())
                .build();
    }
}
