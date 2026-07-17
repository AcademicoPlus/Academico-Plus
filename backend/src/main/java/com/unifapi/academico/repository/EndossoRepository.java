package com.unifapi.academico.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.unifapi.academico.domain.model.EndossoProfessor;

@Repository
public interface EndossoRepository extends JpaRepository<EndossoProfessor, Long> {

    Optional<EndossoProfessor> findByProfessorIdAndAlunoIdAndHabilidadeId(Long professorId, Long alunoId, Long habilidadeId);

    
}
    

