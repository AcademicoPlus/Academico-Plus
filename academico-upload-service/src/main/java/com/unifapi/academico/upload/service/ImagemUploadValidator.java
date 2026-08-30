package com.unifapi.academico.upload.service;

import com.unifapi.academico.upload.exception.UploadException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

final class ImagemUploadValidator {

    private static final List<String> EXTENSOES_PERMITIDAS = List.of("image/jpeg", "image/png", "image/webp");

    private ImagemUploadValidator() {
    }

    static void validar(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new UploadException("Selecione um arquivo de imagem válido.");
        }

        String contentType = arquivo.getContentType();
        if (contentType == null || !EXTENSOES_PERMITIDAS.contains(contentType.toLowerCase())) {
            throw new UploadException("Apenas imagens (PNG, JPG, WEBP) são permitidas.");
        }

        // Content-Type é declarado pelo cliente e pode ser forjado — confere a
        // assinatura real dos primeiros bytes do arquivo antes de gravar.
        byte[] cabecalho = new byte[12];
        int lidos;
        try (InputStream inputStream = arquivo.getInputStream()) {
            lidos = inputStream.readNBytes(cabecalho, 0, cabecalho.length);
        } catch (IOException e) {
            throw new UploadException("Não foi possível ler o arquivo enviado.");
        }

        if (!assinaturaDeImagemValida(cabecalho, lidos)) {
            throw new UploadException(
                    "O conteúdo do arquivo não corresponde a uma imagem válida (PNG, JPG ou WEBP).");
        }
    }

    static String extrairExtensao(String nomeOriginal) {
        if (nomeOriginal != null && nomeOriginal.contains(".")) {
            return nomeOriginal.substring(nomeOriginal.lastIndexOf("."));
        }
        return ".jpg";
    }

    private static boolean assinaturaDeImagemValida(byte[] cabecalho, int tamanhoLido) {
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
}
