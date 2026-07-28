package com.unifapi.academico.api.mapper;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.unifapi.academico.api.dto.request.ProjetoRequestDTO;
import com.unifapi.academico.api.dto.response.ProjetoCardDTO;
import com.unifapi.academico.domain.model.Projeto;
import com.unifapi.academico.domain.model.ProjetoHabilidade;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProjetoMapper {

    @Mapping(target = "habilidades", source = "habilidadesNecessarias")
    ProjetoCardDTO toCardDTO(Projeto projeto);

    // Converte Set<ProjetoHabilidade> diretamente para a List<String> esperada no ProjetoCardDTO
    default List<String> mapHabilidades(Set<ProjetoHabilidade> habilidadesNecessarias) {
        if (habilidadesNecessarias == null) {
            return Collections.emptyList();
        }
        return habilidadesNecessarias.stream()
                .filter(ph -> ph.getHabilidade() != null)
                .map(ph -> ph.getHabilidade().getNome()) // Mapeia para o nome da habilidade (ex: "React Native")
                .collect(Collectors.toList());
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "criador", ignore = true)
    @Mapping(target = "status", ignore = true)
    Projeto toEntity(ProjetoRequestDTO dto);
}