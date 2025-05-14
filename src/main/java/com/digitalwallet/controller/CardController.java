package com.digitalwallet.controller;

import com.digitalwallet.dto.CardRequestDTO;
import com.digitalwallet.dto.CardResponseDTO;
import com.digitalwallet.service.CardService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private static final Logger log = LoggerFactory.getLogger(CardController.class);

    @Autowired
    private CardService cardService;

    @PostMapping("/batch")
    public ResponseEntity<List<CardResponseDTO>> createCardBatch(@RequestBody @Valid CardRequestDTO request) {
        log.info("Received request to create card batch");
        List<CardResponseDTO> response = cardService.createCardBatch(request);
        log.info("Returning response with {] cards", response.size());
        return ResponseEntity.ok(response);
    }
}

