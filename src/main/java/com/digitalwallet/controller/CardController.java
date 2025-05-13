package com.digitalwallet.controller;

import com.digitalwallet.dto.CardRequestDTO;
import com.digitalwallet.dto.CardResponseDTO;
import com.digitalwallet.service.CardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    @Autowired
    private CardService cardService;

    @PostMapping("/batch")
    public ResponseEntity<List<CardResponseDTO>> createCardBatch(@RequestBody CardRequestDTO request) {
        List<CardResponseDTO> response = cardService.createCardBatch(request);
        return ResponseEntity.ok(response);
    }
}

