package com.unifapi.academico.dto.recomendacao;

import com.unifapi.academico.dto.habilidade.HabilidadeResumoResponse;
import com.unifapi.academico.dto.projeto.ProjetoResumoResponse;

import java.util.List;

public record ProjetoRecomendadoResponse(
        ProjetoResumoResponse projeto,
        /** 0.0 a 1.0 — cobertura ponderada dos requisitos do projeto pelas habilidades do usuário. */
        double compatibilidade,
        List<HabilidadeResumoResponse> habilidadesEmComum
) {
}
