package com.unifapi.academico.service;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    String salvar(MultipartFile arquivo);
    void deletar(String nomeArquivo);
}