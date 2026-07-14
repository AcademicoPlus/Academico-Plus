package com.unifapi.academico.service;

import com.unifapi.academico.domain.enums.TipoUsuario;
import com.unifapi.academico.domain.model.Usuario;
import com.unifapi.academico.repository.UsuarioRepository;
import com.unifapi.academico.api.dto.response.UserDTO;
import com.unifapi.academico.api.dto.request.CreateUserRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService {
    
    @Autowired
    private UsuarioRepository userRepository;
    
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll()
            .stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    public UserDTO getUserById(Long id) {
        Usuario user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado com ID: " + id));
        return convertToDTO(user);
    }
    
    public List<UserDTO> getUsersByType(TipoUsuario userType) {
        return userRepository.findByTipo(userType)
            .stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    public UserDTO createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email já existe: " + request.getEmail());
        }
        
        Usuario user = Usuario.builder()
            .email(request.getEmail())
            .nome(request.getFullName())
            .senhaHash(request.getPassword()) // Em produção, usar bcrypt!
            .tipo(request.getUserType())
            .curso(request.getAcademicInfo())
            .bio(request.getBio())
            .ativo(true)
            .build();
        
        Usuario savedUser = userRepository.save(user);
        return convertToDTO(savedUser);
    }
    
    public UserDTO updateUser(Long id, CreateUserRequest request) {
        Usuario user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado com ID: " + id));
        
        user.setNome(request.getFullName());
        user.setCurso(request.getAcademicInfo());
        user.setBio(request.getBio());
        
        Usuario updatedUser = userRepository.save(user);
        return convertToDTO(updatedUser);
    }
    
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Usuário não encontrado com ID: " + id);
        }
        userRepository.deleteById(id);
    }
    
    private UserDTO convertToDTO(Usuario user) {
        return UserDTO.builder()
            .id(user.getId())
            .email(user.getEmail())
            .nome(user.getNome())
            .userType(user.getTipo())
            .academicInfo(user.getCurso())
            .bio(user.getBio())
            .profilePhotoUrl(user.getFotoUrl())
            .isActive(user.getAtivo())
            .createdAt(user.getCriadoEm())
            .updatedAt(user.getAtualizadoEm())
            .build();
    }
}