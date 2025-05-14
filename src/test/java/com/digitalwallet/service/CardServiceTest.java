package com.digitalwallet.service;

import com.digitalwallet.dto.CardRequestDTO;
import com.digitalwallet.entity.Card;
import com.digitalwallet.entity.CardStatus;
import com.digitalwallet.repository.CardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.mockito.Mockito.*;

public class CardServiceTest {

    private CardRepository cardRepository;
    private CardService cardService;

    @BeforeEach
    void setUp(){
        cardRepository = mock(CardRepository.class);
        cardService = new CardService(cardRepository);
        cardService = spy(new CardService(cardRepository)); //..
    }

    @Test
    void createCardBatch_ShouldCreateCorrectNumberOfCards() {

        CardRequestDTO request = new CardRequestDTO();
        request.setQuantity(5);
        request.setValue(7000);

        List<Card> fakeCards = IntStream.range(0, request.getQuantity())
                .mapToObj(i -> {
                    Card card = new Card();
                    card.setId((long)i+1);
                    card.setCode(cardService.generateUniqueCode());
                    card.setValue(7000);
                    card.setStatus(CardStatus.PENDING);
                    return card;
                }).collect(Collectors.toList());

        when(cardRepository.saveAll(anyList())).thenReturn(fakeCards);

        var result = cardService.createCardBatch(request);

        assertEquals(5, result.size());
        assertEquals(7000, result.get(0).getValue());
        assertEquals(CardStatus.PENDING, result.get(0).getStatus());
    }

    @Test
    void createCardBatch_ShouldThrowException_WhenQuantityOrValueInvalid() {
        CardRequestDTO invalidRequest = new CardRequestDTO();
        invalidRequest.setQuantity(0);
        invalidRequest.setValue(500);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> cardService.createCardBatch(invalidRequest)
        );

        assertEquals("Quantity must be > 0 and value must be >= 1000", exception.getMessage());
    }

    @Test
    void generateUniqueCode_ShouldBe16CharactersAndUnique() {
        String code1 = cardService.generateUniqueCode();
        String code2 = cardService.generateUniqueCode();

        assertNotNull(code1);
        assertNotNull(code2);
        assertEquals(16, code1.length());
        assertEquals(16, code2.length());
        assertNotEquals(code1, code2);
    }

}
