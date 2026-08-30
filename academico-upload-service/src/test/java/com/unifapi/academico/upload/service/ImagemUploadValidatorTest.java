package com.unifapi.academico.upload.service;

import com.unifapi.academico.upload.exception.UploadException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImagemUploadValidatorTest {

    private static final byte[] PNG_HEADER = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};

    @Test
    void deveAceitarPngComAssinaturaValida() {
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "foto.png", "image/png", PNG_HEADER);

        assertThatCode(() -> ImagemUploadValidator.validar(arquivo)).doesNotThrowAnyException();
    }

    @Test
    void deveRejeitarArquivoVazio() {
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "foto.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> ImagemUploadValidator.validar(arquivo))
                .isInstanceOf(UploadException.class);
    }

    @Test
    void deveRejeitarContentTypeNaoPermitido() {
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "foto.gif", "image/gif", PNG_HEADER);

        assertThatThrownBy(() -> ImagemUploadValidator.validar(arquivo))
                .isInstanceOf(UploadException.class);
    }

    @Test
    void deveRejeitarContentTypeForjado() {
        // content-type diz PNG, mas os bytes reais não batem com a assinatura de nenhum formato aceito.
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "foto.png", "image/png", "não é uma imagem".getBytes());

        assertThatThrownBy(() -> ImagemUploadValidator.validar(arquivo))
                .isInstanceOf(UploadException.class);
    }

    @Test
    void deveExtrairExtensaoDoNomeOriginal() {
        assertThatCode(() -> ImagemUploadValidator.extrairExtensao("foto.png")).doesNotThrowAnyException();
        org.assertj.core.api.Assertions.assertThat(ImagemUploadValidator.extrairExtensao("foto.png")).isEqualTo(".png");
        org.assertj.core.api.Assertions.assertThat(ImagemUploadValidator.extrairExtensao("sem-extensao")).isEqualTo(".jpg");
    }
}
