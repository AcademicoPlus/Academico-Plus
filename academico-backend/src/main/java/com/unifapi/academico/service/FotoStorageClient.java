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
 * Chama o academico-upload-service via REST síncrono (o front precisa da URL
 * da foto de volta na resposta — não é um fluxo fire-and-forget como o de e-mail).
 */
@Slf4j
@Component
public class FotoStorageClient {

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
            FotoResponse response = restClient.post()
                    .uri(uploadServiceUrl + "/fotos")
                    .header("X-Internal-Api-Key", uploadServiceApiKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(builder.build())
                    .retrieve()
                    .body(FotoResponse.class);

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
                    .uri(uploadServiceUrl + "/fotos?url={url}", url)
                    .header("X-Internal-Api-Key", uploadServiceApiKey)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Não foi possível remover a foto antiga via upload-service: {}", e.getMessage());
        }
    }

    private record FotoResponse(String url) {
    }
}
