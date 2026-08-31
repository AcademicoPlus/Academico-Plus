package com.unifapi.academico.upload.web;

import com.unifapi.academico.upload.exception.UploadException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UploadException.class)
    public ResponseEntity<ErroResponse> handleUploadException(UploadException e) {
        return ResponseEntity.badRequest().body(new ErroResponse(e.getMessage()));
    }

    public record ErroResponse(String mensagem) {
    }
}
