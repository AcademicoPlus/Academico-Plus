package com.unifapi.academico.upload.web;

import com.unifapi.academico.upload.service.R2StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/fotos")
@RequiredArgsConstructor
public class FotoController {

    private final R2StorageService storageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FotoResponse> upload(@RequestParam("arquivo") MultipartFile arquivo) {
        String url = storageService.salvar(arquivo);
        return ResponseEntity.ok(new FotoResponse(url));
    }

    @DeleteMapping
    public ResponseEntity<Void> deletar(@RequestParam("url") String url) {
        storageService.deletar(url);
        return ResponseEntity.noContent().build();
    }

    public record FotoResponse(String url) {
    }
}
