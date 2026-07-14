package com.unifapi.academico.exception;

import com.unifapi.academico.api.dto.response.ErrorResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationException(
            MethodArgumentNotValidException ex,
            WebRequest request) {
        
        String campo = "unknown";
        String mensagem = "Erro de validação";
        
        if (ex.getBindingResult().getFieldError() != null) {
            campo = ex.getBindingResult().getFieldError().getField();
            mensagem = ex.getBindingResult().getFieldError().getDefaultMessage();
        }

        ErrorResponseDTO error = ErrorResponseDTO.builder()
            .status("ERROR")
            .codigo("VALIDACAO_ERROR")
            .mensagem(mensagem)
            .detalhes(campo)
            .timestamp(LocalDateTime.now())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleAccessDeniedException(
            AccessDeniedException ex,
            WebRequest request) {
        
        ErrorResponseDTO error = ErrorResponseDTO.builder()
            .status("ERROR")
            .codigo("SEM_PERMISSAO")
            .mensagem("Sem permissão para acessar este recurso")
            .timestamp(LocalDateTime.now())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponseDTO> handleRuntimeException(
            RuntimeException ex,
            WebRequest request) {
        
        ErrorResponseDTO error = ErrorResponseDTO.builder()
            .status("ERROR")
            .codigo("ERRO_INTERNO")
            .mensagem(ex.getMessage())
            .timestamp(LocalDateTime.now())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleException(
            Exception ex,
            WebRequest request) {
        
        ErrorResponseDTO error = ErrorResponseDTO.builder()
            .status("ERROR")
            .codigo("ERRO_SERVIDOR")
            .mensagem("Erro interno do servidor")
            .timestamp(LocalDateTime.now())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}