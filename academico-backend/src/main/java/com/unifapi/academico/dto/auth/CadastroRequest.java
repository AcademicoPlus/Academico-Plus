package com.unifapi.academico.dto.auth;


import java.util.UUID;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CadastroRequest(

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ser válido")
    String email,

    @NotBlank(message = "Nome completo é obrigatório")
    @Size(min = 3, max = 255, message = "Nome deve ter entre 3 e 255 caracteres")
    String nome,

    Integer periodo,


    @NotNull(message = "A área/curso é obrigatória.")
    UUID idCurso,

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 8, message = "Senha deve ter no mínimo 8 caracteres")
    String senha,

    @NotNull(message = "É necessário indicar se aceita os Termos de Uso e a Política de Privacidade.")
    @AssertTrue(message = "É necessário aceitar os Termos de Uso e a Política de Privacidade para se cadastrar.")
    Boolean aceitouTermos

) {

}
