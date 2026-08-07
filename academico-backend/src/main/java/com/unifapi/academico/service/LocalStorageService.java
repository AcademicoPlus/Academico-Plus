package com.unifapi.academico.service;

import com.unifapi.academico.exception.RegraDeNegocioException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class LocalStorageService implements StorageService {

    @Value("${app.upload.dir:uploads/perfis}")
    private String uploadDir;

    @Value("${app.upload.base-url:http://localhost:8080/uploads/perfis/}")
    private String baseUrl;

    private Path pastaUpload;

    private static final List<String> EXTENSOES_PERMITIDAS = List.of("image/jpeg", "image/png", "image/webp");

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
        if (arquivo == null || arquivo.isEmpty()) {
            throw new RegraDeNegocioException("Selecione um arquivo de imagem válido.");
        }

        String contentType = arquivo.getContentType();
        if (contentType == null || !EXTENSOES_PERMITIDAS.contains(contentType.toLowerCase())) {
            throw new RegraDeNegocioException("Apenas imagens (PNG, JPG, WEBP) são permitidas.");
        }

        // Content-Type é declarado pelo cliente e pode ser forjado — confere a
        // assinatura real dos primeiros bytes do arquivo antes de gravar em disco.
        validarAssinaturaDaImagem(arquivo);

        String extensao = extrairExtensao(arquivo.getOriginalFilename());
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

    private void validarAssinaturaDaImagem(MultipartFile arquivo) {
        byte[] cabecalho = new byte[12];
        int lidos;
        try (InputStream inputStream = arquivo.getInputStream()) {
            lidos = inputStream.readNBytes(cabecalho, 0, cabecalho.length);
        } catch (IOException e) {
            throw new RegraDeNegocioException("Não foi possível ler o arquivo enviado.");
        }

        if (!assinaturaDeImagemValida(cabecalho, lidos)) {
            throw new RegraDeNegocioException(
                    "O conteúdo do arquivo não corresponde a uma imagem válida (PNG, JPG ou WEBP).");
        }
    }

    private boolean assinaturaDeImagemValida(byte[] cabecalho, int tamanhoLido) {
        // PNG: 89 50 4E 47
        if (tamanhoLido >= 4
                && cabecalho[0] == (byte) 0x89 && cabecalho[1] == 0x50
                && cabecalho[2] == 0x4E && cabecalho[3] == 0x47) {
            return true;
        }
        // JPEG: FF D8 FF
        if (tamanhoLido >= 3
                && cabecalho[0] == (byte) 0xFF && cabecalho[1] == (byte) 0xD8 && cabecalho[2] == (byte) 0xFF) {
            return true;
        }
        // WEBP: "RIFF" nos bytes 0-3 e "WEBP" nos bytes 8-11
        return tamanhoLido >= 12
                && cabecalho[0] == 'R' && cabecalho[1] == 'I' && cabecalho[2] == 'F' && cabecalho[3] == 'F'
                && cabecalho[8] == 'W' && cabecalho[9] == 'E' && cabecalho[10] == 'B' && cabecalho[11] == 'P';
    }

    private String extrairExtensao(String nomeOriginal) {
        if (nomeOriginal != null && nomeOriginal.contains(".")) {
            return nomeOriginal.substring(nomeOriginal.lastIndexOf("."));
        }
        return ".jpg";
    }
}