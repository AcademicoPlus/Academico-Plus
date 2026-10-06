package com.unifapi.academico.dto.auth;
import java.util.List;

import com.unifapi.academico.dto.projeto.ProjetoCanceladoResumoResponse;


public record LoginResponse(
        boolean sucesso,
        String mensagem,
        String token,
        UsuarioResumoResponse usuario,
        List<ProjetoCanceladoResumoResponse> avisosCancelamento
) {}