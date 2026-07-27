package com.unifapi.academico.exception;

import com.unifapi.academico.api.dto.response.ErrorResponseDTO;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

        /**
         * Tratamento de erros de validação dos DTOs (@Valid)
         */
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponseDTO> handleValidationException(
                        MethodArgumentNotValidException ex,
                        WebRequest request) {

                // Mapeia todos os campos inválidos e suas respectivas mensagens
                Map<String, String> errosCampos = ex.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .collect(Collectors.toMap(
                                                FieldError::getField,
                                                fieldError -> fieldError.getDefaultMessage() != null
                                                                ? fieldError.getDefaultMessage()
                                                                : "Campo inválido",
                                                (mensagem1, mensagem2) -> mensagem1 + ", " + mensagem2));

                ErrorResponseDTO error = ErrorResponseDTO.builder()
                                .status("ERROR")
                                .codigo("VALIDACAO_ERROR")
                                .mensagem("Um ou mais campos estão inválidos")
                                .detalhes(errosCampos)
                                .timestamp(LocalDateTime.now())
                                .path(extractPath(request))
                                .build();

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        /**
         * Recursos não encontrados (Ex: Usuário, Projeto ou Habilidade não existe no
         * banco)
         */
        @ExceptionHandler(RecursoNaoEncontradoException.class)
        public ResponseEntity<ErrorResponseDTO> handleRecursoNaoEncontrado(
                        RecursoNaoEncontradoException ex,
                        WebRequest request) {

                ErrorResponseDTO error = ErrorResponseDTO.builder()
                                .status("ERROR")
                                .codigo("RECURSO_NAO_ENCONTRADO")
                                .mensagem(ex.getMessage())
                                .timestamp(LocalDateTime.now())
                                .path(extractPath(request))
                                .build();

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        /**
         * Regras de negócio violadas (Ex: Tentar se candidatar a um projeto encerrado)
         */
        @ExceptionHandler(RegraNegocioException.class)
        public ResponseEntity<ErrorResponseDTO> handleRegraNegocio(
                        RegraNegocioException ex,
                        WebRequest request) {

                ErrorResponseDTO error = ErrorResponseDTO.builder()
                                .status("ERROR")
                                .codigo("REGRA_NEGOCIO_VIOLADA")
                                .mensagem(ex.getMessage())
                                .timestamp(LocalDateTime.now())
                                .path(extractPath(request))
                                .build();

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }

        /**
         * Falha de autenticação (Credenciais inválidas no login)
         */
        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<ErrorResponseDTO> handleBadCredentials(
                        BadCredentialsException ex,
                        WebRequest request) {

                ErrorResponseDTO error = ErrorResponseDTO.builder()
                                .status("ERROR")
                                .codigo("CREDENCIAIS_INVALIDAS")
                                .mensagem("E-mail ou senha incorretos")
                                .timestamp(LocalDateTime.now())
                                .path(extractPath(request))
                                .build();

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        /**
         * Acesso negado por perfil de usuário (Spring Security)
         */
        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ErrorResponseDTO> handleAccessDeniedException(
                        AccessDeniedException ex,
                        WebRequest request) {

                ErrorResponseDTO error = ErrorResponseDTO.builder()
                                .status("ERROR")
                                .codigo("SEM_PERMISSAO")
                                .mensagem("Você não tem permissão para realizar esta ação")
                                .timestamp(LocalDateTime.now())
                                .path(extractPath(request))
                                .build();

                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        /**
         * Erros genéricos e não esperados (Fallback 500)
         */
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponseDTO> handleUncaughtException(
                        Exception ex,
                        WebRequest request) {

                log.error("Erro interno do servidor não tratado: ", ex);

                ErrorResponseDTO error = ErrorResponseDTO.builder()
                                .status("ERROR")
                                .codigo("ERRO_INTERNO_SERVIDOR")
                                .mensagem("Ocorreu um erro interno no servidor. Entre em contato com o suporte.")
                                .timestamp(LocalDateTime.now())
                                .path(extractPath(request))
                                .build();

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }

        private String extractPath(WebRequest request) {
                return request.getDescription(false).replace("uri=", "");
        }

        @ExceptionHandler(ExpiredJwtException.class)
        public ResponseEntity<ErrorResponseDTO> handleExpiredJwtException(
                        ExpiredJwtException ex,
                        WebRequest request) {

                ErrorResponseDTO error = ErrorResponseDTO.builder()
                                .status("ERROR")
                                .codigo("TOKEN_EXPIRADO")
                                .mensagem("Sua sessão expirou. Faça login novamente.")
                                .timestamp(LocalDateTime.now())
                                .path(extractPath(request))
                                .build();

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        @ExceptionHandler(JwtException.class)
        public ResponseEntity<ErrorResponseDTO> handleJwtException(
                        JwtException ex,
                        WebRequest request) {

                ErrorResponseDTO error = ErrorResponseDTO.builder()
                                .status("ERROR")
                                .codigo("TOKEN_INVALIDO")
                                .mensagem("Token de autenticação inválido ou malformado.")
                                .timestamp(LocalDateTime.now())
                                .path(extractPath(request))
                                .build();

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
}