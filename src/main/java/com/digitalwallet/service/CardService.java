package com.digitalwallet.service;

import com.digitalwallet.dto.CardRequestDTO;
import com.digitalwallet.dto.CardResponseDTO;
import com.digitalwallet.entity.Card;
import com.digitalwallet.entity.CardStatus;
import com.digitalwallet.repository.CardRepository;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@Service
public class CardService {

    private static final Logger log = LoggerFactory.getLogger(CardService.class);
    private final CardRepository cardRepository;

    public CardService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    public List<CardResponseDTO> createCardBatch(CardRequestDTO request) {

        if (request.getQuantity() <= 0 || request.getValue() <= 1000) {
            log.error("Invalid input: quantity={} or value={}", request.getQuantity(), request.getValue());
            throw new IllegalArgumentException("Quantity must be > 0 and value must be >= 1000");
        }

        log.info("Start creating card batch: quantity={}, value={}", request.getQuantity(), request.getValue());

        List<Card> cardBatch = IntStream.range(0, request.getQuantity())
                .mapToObj(i -> {
                    Card card = new Card();
                    card.setCode(generateUniqueCode());
                    card.setValue(request.getValue());
                    card.setStatus(CardStatus.PENDING);
                    card.setUsed(false);
                    card.setCreatedAt(LocalDateTime.now());
                    return card;
                })
                .collect(Collectors.toList());

        List<Card> savedCards = cardRepository.saveAll(cardBatch);

        log.info("Successfully created {} cards", savedCards.size());

        return convertToDTOList(savedCards);
    }

    protected String generateUniqueCode() {
        // we can improve it later
        String code = UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        log.debug("Generated code: {}", code);
        return code;
    }

    private List<CardResponseDTO> convertToDTOList(List<Card> cards) {
        return cards.stream().map(card -> {
            CardResponseDTO dto = new CardResponseDTO();
            dto.setId(card.getId());
            dto.setCode(card.getCode());
            dto.setValue(card.getValue());
            dto.setStatus(card.getStatus());
            dto.setUsed(card.isUsed());
            dto.setCreatedAt(card.getCreatedAt());
            return dto;
        }).collect(Collectors.toList());
    }
}

