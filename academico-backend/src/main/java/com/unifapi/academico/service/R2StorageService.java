package com.unifapi.academico.service;

import com.unifapi.academico.exception.RegraDeNegocioException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.UUID;

/**
 * Storage de produção: envia os arquivos para um bucket Cloudflare R2 (API
 * compatível com S3). Ativado com {@code app.storage.provider=r2}.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "r2")
public class R2StorageService implements StorageService {

    @Value("${app.storage.r2.account-id}")
    private String accountId;

    @Value("${app.storage.r2.access-key}")
    private String accessKey;

    @Value("${app.storage.r2.secret-key}")
    private String secretKey;

    @Value("${app.storage.r2.bucket}")
    private String bucket;

    @Value("${app.storage.r2.public-url}")
    private String publicUrl;

    private static final String PREFIXO = "perfis/";

    private S3Client s3Client;

    @PostConstruct
    public void init() {
        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create("https://" + accountId + ".r2.cloudflarestorage.com"))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }

    @Override
    public String salvar(MultipartFile arquivo) {
        ImagemUploadValidator.validar(arquivo);

        String extensao = ImagemUploadValidator.extrairExtensao(arquivo.getOriginalFilename());
        String chave = PREFIXO + UUID.randomUUID() + extensao;

        try (InputStream inputStream = arquivo.getInputStream()) {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(chave)
                            .contentType(arquivo.getContentType())
                            .build(),
                    RequestBody.fromInputStream(inputStream, arquivo.getSize()));

            log.info("Arquivo enviado ao R2 com sucesso: {}", chave);
            return publicUrl + "/" + chave;

        } catch (IOException e) {
            log.error("Erro ao ler arquivo enviado: {}", e.getMessage());
            throw new RegraDeNegocioException("Erro ao processar imagem enviada.");
        } catch (Exception e) {
            log.error("Erro ao enviar arquivo para o R2: {}", e.getMessage());
            throw new RegraDeNegocioException("Erro ao salvar imagem no storage externo.");
        }
    }

    @Override
    public void deletar(String nomeArquivo) {
        if (nomeArquivo == null || nomeArquivo.isBlank()) return;

        try {
            String chave = extrairChave(nomeArquivo);
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(chave)
                    .build());
            log.info("Arquivo removido do R2: {}", chave);
        } catch (Exception e) {
            log.warn("Não foi possível remover o arquivo antigo do R2: {}", e.getMessage());
        }
    }

    private String extrairChave(String urlOuChave) {
        if (urlOuChave.startsWith(publicUrl)) {
            String semBase = urlOuChave.substring(publicUrl.length());
            return semBase.startsWith("/") ? semBase.substring(1) : semBase;
        }
        return urlOuChave;
    }
}
