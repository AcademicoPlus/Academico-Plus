package com.unifapi.academico.dto.auth;
import java.util.List;

import com.unifapi.academico.dto.denuncia.SancaoResponse;
import com.unifapi.academico.dto.projeto.ProjetoCanceladoResumoResponse;


public record LoginResponse(
        boolean sucesso,
        String mensagem,
        String token,
        UsuarioResumoResponse usuario,
        List<ProjetoCanceladoResumoResponse> avisosCancelamento,

        /**
         * Advertências (e suspensões já cumpridas) que o usuário ainda não viu.
         * Entregues uma única vez, no primeiro login após a aplicação da sanção.
         */
        List<SancaoResponse> avisosModeracao
) {}