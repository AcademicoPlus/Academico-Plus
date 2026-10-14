package com.unifapi.academico.exception;

import com.unifapi.academico.exception.dto.ErroCampo;
import com.unifapi.academico.exception.dto.ErroResposta;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 1. Trata erros de validação DTO (@Valid / @NotNull / @NotBlank / @Email)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResposta> handleValidacao(
            MethodArgumentNotValidException ex, 
            HttpServletRequest request) {

        List<ErroCampo> errosCampos = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ErroCampo(error.getField(), error.getDefaultMessage()))
                .toList();

        ErroResposta erro = ErroResposta.comErros(
                HttpStatus.BAD_REQUEST.value(),
                "Erro de Validação",
                "Um ou mais campos estão inválidos. Faça o preenchimento correto e tente novamente.",
                request.getRequestURI(),
                errosCampos
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

    // 2. Trata ResponseStatusException (como as que você usou no AuthService/UsuarioService)
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErroResposta> handleResponseStatus(
            ResponseStatusException ex, 
            HttpServletRequest request) {

        ErroResposta erro = ErroResposta.simples(
                ex.getStatusCode().value(),
                ex.getStatusCode().toString(),
                ex.getReason(),
                request.getRequestURI()
        );

        return ResponseEntity.status(ex.getStatusCode()).body(erro);
    }

    // 3. Trata Exceções Customizadas de Domínio
    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResposta> handleNaoEncontrado(
            RecursoNaoEncontradoException ex, 
            HttpServletRequest request) {

        ErroResposta erro = ErroResposta.simples(
                HttpStatus.NOT_FOUND.value(),
                "Recurso não encontrado",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(erro);
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ErroResposta> handleRegraDeNegocio(
            RegraDeNegocioException ex, 
            HttpServletRequest request) {

        ErroResposta erro = ErroResposta.simples(
                HttpStatus.BAD_REQUEST.value(),
                "Violação de Regra de Negócio",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

    // 3b. Trata negação de acesso do @PreAuthorize (ex.: hasRole('ADMIN') falhando para
    // quem não é admin). Sem isso, a exceção cai no catch-all de baixo e vira 500 —
    // o AOP do @PreAuthorize roda dentro do próprio dispatch do MVC, então chega
    // primeiro neste @RestControllerAdvice, antes de qualquer filtro de segurança.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResposta> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {

        ErroResposta erro = ErroResposta.simples(
                HttpStatus.FORBIDDEN.value(),
                "Acesso Negado",
                "Você não tem permissão para acessar este recurso.",
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(erro);
    }

    // 4. Trata JSON malformatado ou tipos incorretos no Body
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResposta> handleJsonInvalido(
            HttpMessageNotReadableException ex, 
            HttpServletRequest request) {

        ErroResposta erro = ErroResposta.simples(
                HttpStatus.BAD_REQUEST.value(),
                "Corpo da Requisição Inválido",
                "O JSON enviado contém erros de sintaxe ou valores incompatíveis.",
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

    // 5. Trata Parâmetros de URL ou Path Variables inválidos (ex: enviar 'abc' onde se espera um UUID)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResposta> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, 
            HttpServletRequest request) {

        String mensagem = String.format("O parâmetro '%s' recebeu o valor '%s', que é inválido.", 
                ex.getName(), ex.getValue());

        ErroResposta erro = ErroResposta.simples(
                HttpStatus.BAD_REQUEST.value(),
                "Parâmetro Inválido",
                mensagem,
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

    // 5b. Rede de segurança para constraints do banco (UNIQUE, FK, NOT NULL) que
    // escaparam da validação de regra de negócio. Sem isso qualquer colisão vira
    // 500 "Erro Interno do Servidor" — resposta inútil para quem só reenviou um
    // e-mail já usado. A causa real fica no log, não na resposta: a mensagem do
    // Postgres expõe nomes de tabela/coluna e o valor que colidiu.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResposta> handleViolacaoDeIntegridade(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        log.warn("Violação de integridade na requisição [{}]: ", request.getRequestURI(), ex);

        ErroResposta erro = ErroResposta.simples(
                HttpStatus.CONFLICT.value(),
                "Conflito de Dados",
                "Esta operação conflita com um registro já existente. Revise os dados e tente novamente.",
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(erro);
    }

    // 6. Catch-All: Trata qualquer erro 500 inesperado (NullPointer, erros de BD, etc)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResposta> handleErroInesperado(
            Exception ex, 
            HttpServletRequest request) {

        // Loga a exceção completa no console com a stack trace para debug
        log.error("Erro não tratado na requisição [{}]: ", request.getRequestURI(), ex);

        ErroResposta erro = ErroResposta.simples(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Erro Interno do Servidor",
                "Ocorreu um erro interno inesperado no sistema. Tente novamente mais tarde.",
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(erro);
    }
}