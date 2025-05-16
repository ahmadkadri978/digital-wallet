package com.digitalwallet.controller;

import com.digitalwallet.dto.FileRequestDTO;
import com.digitalwallet.dto.FileResponseDTO;
import com.digitalwallet.service.FileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping
    public ResponseEntity<FileResponseDTO> createFile(@RequestBody @Valid FileRequestDTO request) {

        Long userId = 1L; // مؤقتًا

        FileResponseDTO response = fileService.createFile(request, userId);
        return ResponseEntity.ok(response);
    }
}

