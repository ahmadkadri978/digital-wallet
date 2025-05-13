package com.digitalwallet.service;

import com.digitalwallet.dto.CardRequestDTO;
import com.digitalwallet.dto.CardResponseDTO;
import com.digitalwallet.entity.Card;
import com.digitalwallet.entity.CardStatus;
import com.digitalwallet.repository.CardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CardService {

    @Autowired
    private CardRepository cardRepository;

    public List<CardResponseDTO> createCardBatch(CardRequestDTO request) {
        List<Card> cardBatch = new ArrayList<>();

        for (int i = 0; i < request.getQuantity(); i++) {
            Card card = new Card();
            card.setCode(generateUniqueCode());
            card.setValue(request.getValue());
            card.setStatus(CardStatus.PENDING);
            card.setUsed(false);
            card.setCreatedAt(LocalDateTime.now());

            cardBatch.add(card);
        }
        List<Card> savedCards = cardRepository.saveAll(cardBatch);

        return convertToDTOList(savedCards);
    }

    private String generateUniqueCode() {
        // we can improve it later
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private List<CardResponseDTO> convertToDTOList(List<Card> cards) {
        List<CardResponseDTO> responseList = new ArrayList<>();
        for (Card card : cards) {
            CardResponseDTO dto = new CardResponseDTO();
            dto.setId(card.getId());
            dto.setCode(card.getCode());
            dto.setValue(card.getValue());
            dto.setStatus(card.getStatus());
            dto.setUsed(card.isUsed());
            dto.setCreatedAt(card.getCreatedAt());
            responseList.add(dto);
        }
        return responseList;
    }
}

