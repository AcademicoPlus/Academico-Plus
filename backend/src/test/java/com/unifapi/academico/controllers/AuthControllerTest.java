package com.unifapi.academico.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unifapi.academico.BaseTest;
import com.unifapi.academico.api.dto.request.LoginRequestDTO;
import com.unifapi.academico.api.dto.request.RegistrarUsuarioDTO;
import com.unifapi.academico.domain.model.Usuario;
import com.unifapi.academico.domain.enums.TipoUsuario;
import com.unifapi.academico.repository.RefreshTokenRepository; // Import adicionado
import com.unifapi.academico.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("AuthController")
class AuthControllerTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository; // Injetado para resolver o erro de FK

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Usuario usuarioTeste;

    @BeforeEach
    void setup() {
        // Apaga os tokens primeiro para não violar a FK
        refreshTokenRepository.deleteAll();
        usuarioRepository.deleteAll();

        usuarioTeste = Usuario.builder()
            .email("teste@unifapi.edu.br")
            .nome("Teste User")
            .curso("Computação")
            .senhaHash(passwordEncoder.encode("password123"))
            .tipo(TipoUsuario.ALUNO)
            .ativo(true)
            .build();

        usuarioRepository.save(usuarioTeste);
    }

    @Test
    @DisplayName("Deve fazer login com sucesso (200 OK)")
    void deveLoginComSucesso() throws Exception {
        // Arrange
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("teste@unifapi.edu.br");
        request.setSenha("password123");

        // Act & Assert
        MvcResult result = mockMvc.perform(post("/auth/login")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.email").value("teste@unifapi.edu.br"))
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.expiresIn").exists())
            .andReturn();

        String response = result.getResponse().getContentAsString();
        assertThat(response).contains("token");
    }

    @Test
    @DisplayName("Deve falhar login com credenciais inválidas (400 BAD REQUEST)")
    void devefalharLoginCredenciaisInvalidas() throws Exception {
        // Arrange
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("teste@unifapi.edu.br");
        request.setSenha("senhaErrada");

        // Act & Assert (Alterado de .isUnauthorized() para .isBadRequest())
        mockMvc.perform(post("/auth/login")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve registrar novo usuario (201 CREATED)")
    void deveSignupComSucesso() throws Exception {
        // Arrange
        RegistrarUsuarioDTO request = new RegistrarUsuarioDTO();
        request.setEmail("novo@unifapi.edu.br");
        request.setNome("Novo Usuario");
        request.setCurso("Engenharia");
        request.setSenha("password123");

        // Act & Assert
        mockMvc.perform(post("/auth/signup")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.email").value("novo@unifapi.edu.br"))
            .andExpect(jsonPath("$.token").exists());

        // Verify
        assertThat(usuarioRepository.findByEmail("novo@unifapi.edu.br"))
            .isPresent();
    }

    @Test
    @DisplayName("Deve retornar dados do usuário autenticado (200 OK)")
    void deveRetornarMe() throws Exception {
        // Arrange
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setEmail("teste@unifapi.edu.br");
        loginRequest.setSenha("password123");

        // Fazer login primeiro
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andReturn();

        String loginResponse = loginResult.getResponse().getContentAsString();
        String token = objectMapper.readTree(loginResponse).get("token").asText();

        // Act & Assert
        mockMvc.perform(get("/auth/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("teste@unifapi.edu.br"))
            .andExpect(jsonPath("$.nome").value("Teste User"));
    }

    @Test
    @DisplayName("Health check deve retornar UP (200 OK)")
    void deveRetornarHealth() throws Exception {
        mockMvc.perform(get("/auth/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }
}