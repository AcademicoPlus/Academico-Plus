package com.unifapi.academico.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unifapi.academico.BaseTest;
import com.unifapi.academico.domain.model.Projeto;
import com.unifapi.academico.domain.model.Usuario;
import com.unifapi.academico.domain.enums.StatusProjeto;
import com.unifapi.academico.domain.enums.TipoUsuario;
import com.unifapi.academico.repository.HabilidadeRepository; // Novo import
import com.unifapi.academico.repository.ProjetoRepository;
import com.unifapi.academico.repository.RefreshTokenRepository;
import com.unifapi.academico.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("ProjetoController")
class ProjetoControllerTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProjetoRepository projetoRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private HabilidadeRepository habilidadeRepository; // Injetado para limpeza do banco

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Usuario criador;
    private Projeto projeto;
    private String token;

    @BeforeEach
    void setup() throws Exception {
        // Limpeza na ordem correta de dependências (filhos -> pai)
        refreshTokenRepository.deleteAll();
        projetoRepository.deleteAll();
        habilidadeRepository.deleteAll();
        usuarioRepository.deleteAll();

        criador = Usuario.builder()
                .email("carlos@unifapi.edu.br")
                .nome("Carlos")
                .senhaHash(passwordEncoder.encode("password123"))
                .tipo(TipoUsuario.ALUNO)
                .ativo(true)
                .build();

        criador = usuarioRepository.save(criador);

        projeto = Projeto.builder()
                .titulo("Sistema de Gestão")
                .descricao("Desenvolver sistema de gestão acadêmica completo")
                .status(StatusProjeto.ABERTO)
                .vagas(3)
                .vagasPreenchidas(0)
                .criador(criador)
                .ativo(true)
                .build();

        projeto = projetoRepository.save(projeto);

        // Obter token JWT
        var loginResponse = mockMvc.perform(post("/auth/login")
                .contentType("application/json")
                .content("{\"email\":\"carlos@unifapi.edu.br\",\"senha\":\"password123\"}"))
                .andReturn();

        token = objectMapper.readTree(loginResponse.getResponse().getContentAsString())
                .get("token").asText();
    }

    @Test
    @DisplayName("Deve listar projetos (200 OK)")
    void deveListarProjetos() throws Exception {
        mockMvc.perform(get("/projetos?page=0&size=10")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dados").isArray())
                .andExpect(jsonPath("$.total").exists());
    }

    @Test
    @DisplayName("Deve obter projeto por ID (200 OK)")
    void deveOberterProjetoPorId() throws Exception {
        mockMvc.perform(get("/projetos/" + projeto.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(projeto.getId().toString()))
                .andExpect(jsonPath("$.titulo").value("Sistema de Gestão"));
    }

    @Test
    @DisplayName("Deve criar novo projeto (201 CREATED)")
    void deveCriarNovoProjeto() throws Exception {
        // DTO enviado no formato esperado pelo ProjetoRequestDTO
        String novoProjetoJson = """
            {
                "titulo": "Novo Projeto de Extensão",
                "descricao": "Descrição detalhada do novo projeto acadêmico para testes",
                "vagas": 5,
                "habilidadeIds": []
            }
            """;

        mockMvc.perform(post("/projetos")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content(novoProjetoJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.titulo").value("Novo Projeto de Extensão"))
                .andExpect(jsonPath("$.vagas").value(5));
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request ao criar projeto com 0 vagas")
    void deveRetornar400AoCriarProjetoSemVagas() throws Exception {
        String projetoInvalidoJson = """
            {
                "titulo": "Projeto Sem Vagas",
                "descricao": "Descrição detalhada do projeto",
                "vagas": 0,
                "habilidadeIds": []
            }
            """;

        mockMvc.perform(post("/projetos")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content(projetoInvalidoJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve retornar 403/401 Forbidden sem autenticação ao tentar criar projeto")
    void deveRetornar403SemAutenticacao() throws Exception {
        String novoProjetoJson = """
            {
                "titulo": "Projeto",
                "descricao": "Descrição do teste",
                "vagas": 3,
                "habilidadeIds": []
            }
            """;

        mockMvc.perform(post("/projetos")
                .contentType("application/json")
                .content(novoProjetoJson))
                .andExpect(status().isForbidden());
    }
}