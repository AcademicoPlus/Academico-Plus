package com.unifapi.academico.service;

import com.unifapi.academico.exception.RegraDeNegocioException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Chama o academico-upload-service via REST síncrono — mesmo mecanismo do
 * FotoStorageClient (foto de perfil), mas para a capa/banner do projeto,
 * salva sob um prefixo diferente no mesmo bucket R2 (ver BannerController
 * no upload-service).
 */
@Slf4j
@Component
public class BannerStorageClient {

    @Value("${app.upload-service.url}")
    private String uploadServiceUrl;

    @Value("${app.upload-service.api-key}")
    private String uploadServiceApiKey;

    private final RestClient restClient = RestClient.create();

    public String salvar(MultipartFile arquivo) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        try {
            var partBuilder = builder.part("arquivo", arquivo.getBytes())
                    .filename(arquivo.getOriginalFilename());
            if (arquivo.getContentType() != null) {
                partBuilder.contentType(MediaType.parseMediaType(arquivo.getContentType()));
            }
        } catch (IOException e) {
            throw new RegraDeNegocioException("Erro ao processar imagem enviada.");
        }

        try {
            BannerResponse response = restClient.post()
                    .uri(uploadServiceUrl + "/banners")
                    .header("X-Internal-Api-Key", uploadServiceApiKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(builder.build())
                    .retrieve()
                    .body(BannerResponse.class);

            return response.url();
        } catch (Exception e) {
            log.error("Erro ao enviar imagem para o upload-service: {}", e.getMessage());
            throw new RegraDeNegocioException("Erro ao salvar imagem no storage externo.");
        }
    }

    public void deletar(String url) {
        if (url == null || url.isBlank()) return;

        try {
            restClient.delete()
                    .uri(uploadServiceUrl + "/banners?url={url}", url)
                    .header("X-Internal-Api-Key", uploadServiceApiKey)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Não foi possível remover o banner antigo via upload-service: {}", e.getMessage());
        }
    }

    private record BannerResponse(String url) {
    }
}
