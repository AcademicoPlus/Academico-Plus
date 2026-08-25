package com.unifapi.academico.service;

import com.unifapi.academico.exception.RegraDeNegocioException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Implementação de dev: grava no disco da própria instância. Não sobrevive a
 * redeploy/múltiplas instâncias — em produção usar {@link R2StorageService}
 * ({@code app.storage.provider=r2}).
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    @Value("${app.upload.dir:uploads/perfis}")
    private String uploadDir;

    @Value("${app.upload.base-url:http://localhost:8080/uploads/perfis/}")
    private String baseUrl;

    private Path pastaUpload;

    @PostConstruct
    public void init() {
        try {
            this.pastaUpload = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(this.pastaUpload);
        } catch (IOException e) {
            throw new RuntimeException("Não foi possível criar o diretório de uploads.", e);
        }
    }

    @Override
    public String salvar(MultipartFile arquivo) {
        ImagemUploadValidator.validar(arquivo);

        String extensao = ImagemUploadValidator.extrairExtensao(arquivo.getOriginalFilename());
        String nomeArquivoUnico = UUID.randomUUID() + extensao;

        try {
            Path destino = this.pastaUpload.resolve(nomeArquivoUnico).normalize();
            
            try (InputStream inputStream = arquivo.getInputStream()) {
                Files.copy(inputStream, destino, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("Arquivo salvo com sucesso: {}", nomeArquivoUnico);
            return baseUrl + nomeArquivoUnico;

        } catch (IOException e) {
            log.error("Erro ao salvar arquivo em disco: {}", e.getMessage());
            throw new RegraDeNegocioException("Erro ao salvar imagem no servidor.");
        }
    }

    @Override
    public void deletar(String nomeArquivo) {
        if (nomeArquivo == null || nomeArquivo.isBlank()) return;

        try {
            // Extrai o nome do arquivo se receber a URL completa
            String nomeLimpo = nomeArquivo.substring(nomeArquivo.lastIndexOf('/') + 1);
            Path arquivoPath = this.pastaUpload.resolve(nomeLimpo).normalize();
            Files.deleteIfExists(arquivoPath);
            log.info("Arquivo deletado: {}", nomeLimpo);
        } catch (IOException e) {
            log.warn("Não foi possível remover o arquivo antigo: {}", e.getMessage());
        }
    }
}