package com.unifapi.academico.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponseDTO {
    private String status;
    private String codigo;
    private String mensagem;
    private Object detalhes;
    private LocalDateTime timestamp;
    private String path;
}