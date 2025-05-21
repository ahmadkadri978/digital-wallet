package com.digitalwallet.service;

import com.digitalwallet.dto.AssignCardsToAgentRequestDTO;
import com.digitalwallet.dto.CardRequestDTO;
import com.digitalwallet.entity.*;
import com.digitalwallet.repository.CardRepository;
import com.digitalwallet.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.mockito.Mockito.*;

public class CardServiceTest {

    private CardRepository cardRepository;
    private UserRepository userRepository;
    private CardService cardService;

    @BeforeEach
    void setUp(){
        cardRepository = mock(CardRepository.class);
        userRepository = mock(UserRepository.class);
        cardService = new CardService(cardRepository, userRepository);
        cardService = spy(new CardService(cardRepository,userRepository));
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

    @Test
    void assignCardsToAgent_ShouldSucceed_WhenAllConditionsAreMet() {
        AssignCardsToAgentRequestDTO request = new AssignCardsToAgentRequestDTO();
        request.setAgentId(1L);
        request.setCardCodes(List.of("CODE1", "CODE2"));

        User agent = new User();
        agent.setId(1L);
        agent.setRole(UserRole.AGENT);

        Card card1 = new Card();
        card1.setQrImageBase64("CODE1");
        card1.setUsed(false);
        card1.setAssignedTo(null);
        File batch = new File();
        batch.setStatus(FileStatus.PRINTED);
        card1.setBatch(batch);

        Card card2 = new Card();
        card2.setQrImageBase64("CODE2");
        card2.setUsed(false);
        card2.setAssignedTo(null);
        card2.setBatch(batch);

        when(userRepository.findById(1L)).thenReturn(Optional.of(agent));
        when(cardRepository.findAllByCodeIn(List.of("CODE1", "CODE2"))).thenReturn(List.of(card1, card2));

        int result = cardService.assignCardsToAgent(request);

        assertEquals(2, result);
        assertEquals(agent, card1.getAssignedTo());
        assertEquals(agent, card2.getAssignedTo());
        verify(cardRepository).saveAll(List.of(card1, card2));
    }

    @Test
    void assignCardsToAgent_ShouldThrow_WhenAgentNotFound() {
        AssignCardsToAgentRequestDTO request = new AssignCardsToAgentRequestDTO();
        request.setAgentId(99L);
        request.setCardCodes(List.of("CODE1"));

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                cardService.assignCardsToAgent(request)
        );

        assertEquals("Agent not found", ex.getMessage());
    }

    @Test
    void assignCardsToAgent_ShouldThrow_WhenUserIsNotAgent() {
        User user = new User();
        user.setId(1L);
        user.setRole(UserRole.CENTER); //  NOT AGENT

        AssignCardsToAgentRequestDTO request = new AssignCardsToAgentRequestDTO();
        request.setAgentId(1L);
        request.setCardCodes(List.of("CODE1"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                cardService.assignCardsToAgent(request)
        );

        assertEquals("Provided user is not an agent", ex.getMessage());
    }

    @Test
    void assignCardsToAgent_ShouldThrow_WhenSomeCardsDoNotExist() {
        User agent = new User();
        agent.setId(1L);
        agent.setRole(UserRole.AGENT);

        AssignCardsToAgentRequestDTO request = new AssignCardsToAgentRequestDTO();
        request.setAgentId(1L);
        request.setCardCodes(List.of("CODE1", "CODE2"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(agent));
        when(cardRepository.findAllByCodeIn(any())).thenReturn(List.of(new Card())); // فقط بطاقة واحدة

        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                cardService.assignCardsToAgent(request)
        );

        assertEquals("Some cards do not exist", ex.getMessage());
    }

    @Test
    void assignCardsToAgent_ShouldThrow_WhenCardIsInvalid() {
        User agent = new User();
        agent.setId(1L);
        agent.setRole(UserRole.AGENT);

        Card card = new Card();
        card.setCode("CODE1");
        card.setUsed(true); // cards are already used
        File batch = new File();
        batch.setStatus(FileStatus.PRINTED);
        card.setBatch(batch);

        AssignCardsToAgentRequestDTO request = new AssignCardsToAgentRequestDTO();
        request.setAgentId(1L);
        request.setCardCodes(List.of("CODE1"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(agent));
        when(cardRepository.findAllByCodeIn(List.of("CODE1"))).thenReturn(List.of(card));

        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                cardService.assignCardsToAgent(request)
        );

        assertEquals("Some cards are already assigned, used, or not printed", ex.getMessage());
    }





}
