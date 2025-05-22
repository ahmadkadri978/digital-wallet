package com.digitalwallet.controller;

import com.digitalwallet.dto.CenterRequestDTO;
import com.digitalwallet.dto.CenterResponseDTO;
import com.digitalwallet.service.CenterService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/centers")
public class CenterController {

    private static final Logger log = LoggerFactory.getLogger(CenterController.class);

    private final CenterService centerService;

    public CenterController(CenterService centerService) {
        this.centerService = centerService;
    }

    @PostMapping
    public ResponseEntity<CenterResponseDTO> createCenter(
            @RequestParam Long currentUserId,
            @RequestBody @Valid CenterRequestDTO request) {

        log.info("User ID {} requested to create a center", currentUserId);
        CenterResponseDTO response = centerService.createCenter(currentUserId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<CenterResponseDTO>> getAllCenters(@RequestParam Long currentUserId) {
        log.info("User ID {} requested to get all centers", currentUserId);
        return ResponseEntity.ok(centerService.getAllCenters(currentUserId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CenterResponseDTO> updateCenter(
            @RequestParam Long currentUserId,
            @PathVariable Long id,
            @RequestBody @Valid CenterRequestDTO request) {

        log.info("User ID {} requested to update center ID {}", currentUserId, id);
        return ResponseEntity.ok(centerService.updateCenter(currentUserId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCenter(
            @RequestParam Long currentUserId,
            @PathVariable Long id) {

        log.info("User ID {} requested to delete center ID {}", currentUserId, id);
        centerService.deleteCenter(currentUserId, id);
        return ResponseEntity.noContent().build();
    }
}

