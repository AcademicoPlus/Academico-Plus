package com.unifapi.academico.upload.service;

import com.unifapi.academico.upload.exception.UploadException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.regex.Pattern;

@Slf4j
@Service
public class R2StorageService {

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

    /** UUID gerado em salvar() seguido da extensão, sem barra (não sai do prefixo). */
    private static final Pattern NOME_ARQUIVO_GERADO = Pattern.compile(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}[^/]*");

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

    public String salvar(MultipartFile arquivo) {
        return salvar(arquivo, PREFIXO);
    }

    public String salvar(MultipartFile arquivo, String prefixo) {
        ImagemUploadValidator.validar(arquivo);

        String extensao = ImagemUploadValidator.extrairExtensao(arquivo.getOriginalFilename());
        String chave = prefixo + UUID.randomUUID() + extensao;

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
            throw new UploadException("Erro ao processar imagem enviada.");
        } catch (Exception e) {
            log.error("Erro ao enviar arquivo para o R2: {}", e.getMessage());
            throw new UploadException("Erro ao salvar imagem no storage externo.");
        }
    }

    public void deletar(String url) {
        deletar(url, PREFIXO);
    }

    /**
     * Remove um arquivo gerado por {@link #salvar(MultipartFile, String)} sob o
     * prefixo informado. Antes, qualquer chave do bucket era aceita (inclusive
     * uma chave crua, sem a URL pública), então quem conseguisse fazer o
     * monolito repassar uma URL arbitrária apagava foto/capa de outras pessoas.
     */
    public void deletar(String url, String prefixo) {
        if (url == null || url.isBlank()) return;

        String chave = extrairChaveGerenciada(url, publicUrl, prefixo);
        if (chave == null) {
            log.warn("Remoção recusada: URL fora do padrão gerado por este serviço ({}).", prefixo);
            throw new UploadException("URL de arquivo inválida para remoção.");
        }

        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(chave)
                    .build());
            log.info("Arquivo removido do R2: {}", chave);
        } catch (Exception e) {
            log.warn("Não foi possível remover o arquivo antigo do R2: {}", e.getMessage());
        }
    }

    /**
     * Devolve a chave só se a URL tiver exatamente o formato que {@code salvar}
     * produz: {@code <publicUrl>/<prefixo><uuid><extensão>}, sem "/" depois do
     * prefixo. Qualquer outra coisa devolve null.
     */
    static String extrairChaveGerenciada(String url, String publicUrl, String prefixo) {
        if (url == null || publicUrl == null || publicUrl.isBlank() || prefixo == null) {
            return null;
        }

        String base = publicUrl + "/";
        if (!url.startsWith(base)) {
            return null;
        }

        String chave = url.substring(base.length());
        if (!chave.startsWith(prefixo)) {
            return null;
        }

        String nomeArquivo = chave.substring(prefixo.length());
        return NOME_ARQUIVO_GERADO.matcher(nomeArquivo).matches() ? chave : null;
    }
}
