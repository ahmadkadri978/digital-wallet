package com.digitalwallet.controller;

import com.digitalwallet.dto.*;
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

    private final CardService cardService;

    public CardController(CardService fileService) {
        this.cardService = fileService;
    }

    @PostMapping("/batch")
    public ResponseEntity<List<CardResponseDTO>> createCardBatch(@RequestBody @Valid CardRequestDTO request) {
        log.info("Received request to create card batch");
        List<CardResponseDTO> response = cardService.createCardBatch(request);
        log.info("Returning response with {] cards", response.size());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/assign-to-agent")
    // We must validate that the current user has_Role Admin
    public ResponseEntity<String> assignCardsToAgent(@RequestBody @Valid AssignCardsToAgentRequestDTO request) {

        log.info("Received request to assign {} cards to agent ID {}", request.getCardCodes().size(), request.getAgentId());

        int count = cardService.assignCardsToAgent(request);

        return ResponseEntity.ok(count + " cards successfully assigned to agent ID " + request.getAgentId());
    }

    @PostMapping("/associate-to-agent")
    public ResponseEntity<Void> associateCardsWithAgent(@RequestBody @Valid AssociateCardsRequestDTO request) {

        log.info("Received request to associate cards from file ID {} to agent ID {}", request.getFileId(), request.getAgentId());

        cardService.associateCardsWithAgent(request);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/assign-to-center")
    public ResponseEntity<String> assignCardsToCenter(
            @RequestParam Long currentUserId,
            @RequestBody @Valid AssignCardsToCenterRequestDTO request) {

        log.info("Agent ID {} is assigning cards to Center ID {}", currentUserId, request.getPurchaseCenterId());

        cardService.assignCardsToCenter(currentUserId, request);

        return ResponseEntity.ok("Cards successfully assigned to center ID " + request.getPurchaseCenterId());
    }

    @PostMapping("/{cardId}/activate")
    public ResponseEntity<String> activateCard(
            @PathVariable Long cardId,
            @RequestBody @Valid ActivateCardRequestDTO request) {
        log.info("Received request to activate Card ID {}", cardId);
        cardService.activateCard(cardId, request.getAgentId());
        return ResponseEntity.ok("Card activated successfully.");
    }

//    @PostMapping("/pay")
//    public ResponseEntity<CardPaymentResponseDTO> payWithCard(@RequestBody @Valid CardPaymentRequestDTO request) {
//        log.info("Received payment request with card code {}", request.getCode());
//
//        int remainingBalance = cardService.processCardPayment(request);
//
//        CardPaymentResponseDTO response = new CardPaymentResponseDTO();
//        response.setMessage("Payment processed successfully");
//        response.setRemainingBalance(remainingBalance);
//
//        return ResponseEntity.ok(response);
//    }

    @PostMapping("/use")
    public ResponseEntity<CardPaymentResponseDTO> useCards(@Valid @RequestBody CardUsageRequestDTO request) {
        CardPaymentResponseDTO response = cardService.useCardsForPayment(request);
        return ResponseEntity.ok(response);
    }



    @GetMapping("/all")
    public ResponseEntity<List<CardResponseDTO>> getAllCards() {
        log.info("Fetching all cards");
        List<CardResponseDTO> cards = cardService.getAllCards();
        return ResponseEntity.ok(cards);
    }


}

