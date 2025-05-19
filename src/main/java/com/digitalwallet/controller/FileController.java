package com.digitalwallet.controller;

import com.digitalwallet.dto.FileRequestDTO;
import com.digitalwallet.dto.FileResponseDTO;
import com.digitalwallet.entity.File;
import com.digitalwallet.service.FileService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;
    private static final Logger log = LoggerFactory.getLogger(CardController.class);

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping
    public ResponseEntity<FileResponseDTO> createFile(@RequestBody @Valid FileRequestDTO request) {

        Long userId = 1L; // مؤقتًا
        log.info("Received request to create file");

        FileResponseDTO response = fileService.createFile(request, userId);

        log.info("Returning response with file", response);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadFile(@PathVariable Long id) {
        byte[] pdf = fileService.generateFilePdf(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "file-" + id + ".pdf");

        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}

