package com.digitalwallet.service;

import com.digitalwallet.dto.AssignCardsToAgentRequestDTO;
import com.digitalwallet.dto.AssociateCardsRequestDTO;
import com.digitalwallet.dto.CardRequestDTO;
import com.digitalwallet.dto.CardResponseDTO;
import com.digitalwallet.entity.*;
import com.digitalwallet.mapper.CardMapper;
import com.digitalwallet.repository.CardRepository;
import com.digitalwallet.repository.FileRepository;
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
    private  FileRepository fileRepository;
    private UserRepository userRepository;
    private CardService cardService;
    private CardMapper cardMapper;

    @BeforeEach
    void setUp() {
        cardRepository = mock(CardRepository.class);
        userRepository = mock(UserRepository.class);
        fileRepository = mock(FileRepository.class);
        cardMapper = mock(CardMapper.class);

        cardService = spy(new CardService(fileRepository, cardRepository, userRepository, cardMapper));
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

    @Test
    void associateCardsWithAgent_ShouldSucceed() {
        Long agentId = 1L;
        Long fileId = 10L;

        User agent = new User();
        agent.setId(agentId);
        agent.setRole(UserRole.AGENT);

        File file = new File();
        file.setId(fileId);

        List<Card> cards = List.of(new Card(), new Card());

        when(userRepository.findById(agentId)).thenReturn(Optional.of(agent));
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));
        when(cardRepository.findByBatch(file)).thenReturn(cards);

        AssociateCardsRequestDTO request = new AssociateCardsRequestDTO();
        request.setAgentId(agentId);
        request.setFileId(fileId);

        assertDoesNotThrow(() -> cardService.associateCardsWithAgent(request));

        verify(cardRepository).saveAll(cards);
    }

    @Test
    void associateCardsWithAgent_ShouldFail_WhenAgentNotFound() {
        Long agentId = 1L;
        Long fileId = 10L;

        when(userRepository.findById(agentId)).thenReturn(Optional.empty());

        AssociateCardsRequestDTO request = new AssociateCardsRequestDTO();
        request.setAgentId(agentId);
        request.setFileId(fileId);

        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                cardService.associateCardsWithAgent(request)
        );

        assertEquals("Agent not found", ex.getMessage());
    }

    @Test
    void associateCardsWithAgent_ShouldFail_WhenAgentRoleInvalid() {
        Long agentId = 1L;
        Long fileId = 10L;

        User user = new User();
        user.setId(agentId);
        user.setRole(UserRole.END_USER);

        when(userRepository.findById(agentId)).thenReturn(Optional.of(user));

        AssociateCardsRequestDTO request = new AssociateCardsRequestDTO();
        request.setAgentId(agentId);
        request.setFileId(fileId);

        Exception ex = assertThrows(SecurityException.class, () ->
                cardService.associateCardsWithAgent(request)
        );

        assertEquals("Only agents can receive cards", ex.getMessage());
    }

    @Test
    void associateCardsWithAgent_ShouldFail_WhenFileNotFound() {
        Long agentId = 1L;
        Long fileId = 10L;

        User agent = new User();
        agent.setId(agentId);
        agent.setRole(UserRole.AGENT);

        when(userRepository.findById(agentId)).thenReturn(Optional.of(agent));
        when(fileRepository.findById(fileId)).thenReturn(Optional.empty());

        AssociateCardsRequestDTO request = new AssociateCardsRequestDTO();
        request.setAgentId(agentId);
        request.setFileId(fileId);

        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                cardService.associateCardsWithAgent(request)
        );

        assertEquals("File not found", ex.getMessage());
    }

    @Test
    void associateCardsWithAgent_ShouldFail_WhenCardsAlreadyAssigned() {
        Long agentId = 1L;
        Long fileId = 10L;

        User agent = new User();
        agent.setId(agentId);
        agent.setRole(UserRole.AGENT);

        File file = new File();
        file.setId(fileId);

        User anotherAgent = new User();
        anotherAgent.setId(2L);

        Card assignedCard = new Card();
        assignedCard.setAssignedTo(anotherAgent);

        List<Card> cards = List.of(assignedCard);

        when(userRepository.findById(agentId)).thenReturn(Optional.of(agent));
        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));
        when(cardRepository.findByBatch(file)).thenReturn(cards);

        AssociateCardsRequestDTO request = new AssociateCardsRequestDTO();
        request.setAgentId(agentId);
        request.setFileId(fileId);

        Exception ex = assertThrows(IllegalStateException.class, () ->
                cardService.associateCardsWithAgent(request)
        );

        assertEquals("Some cards are already assigned to another agent. Please contact support.", ex.getMessage());
    }

    @Test
    void activateCard_ShouldSucceed_WhenCardBelongsToAgent() {
        Long cardId = 1L;
        Long agentId = 10L;

        User agent = new User();
        agent.setId(agentId);

        Card card = new Card();
        card.setId(cardId);
        card.setAssignedTo(agent);
        card.setStatus(CardStatus.ASSIGNED);

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        assertDoesNotThrow(() -> cardService.activateCard(cardId, agentId));
        verify(cardRepository).save(card);
        assertTrue(card.getStatus().equals(CardStatus.ACTIVATED));
    }

    @Test
    void activateCard_ShouldFail_WhenCardNotFound() {
        Long cardId = 1L;
        Long agentId = 10L;

        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                cardService.activateCard(cardId, agentId)
        );

        assertEquals("Card not found", ex.getMessage());
    }

    @Test
    void activateCard_ShouldFail_WhenCardAssignedToAnotherAgent() {
        Long cardId = 1L;
        Long agentId = 10L;

        User anotherAgent = new User();
        anotherAgent.setId(20L);

        Card card = new Card();
        card.setId(cardId);
        card.setAssignedTo(anotherAgent);
        card.setStatus(CardStatus.ASSIGNED);

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        Exception ex = assertThrows(SecurityException.class, () ->
                cardService.activateCard(cardId, agentId)
        );

        assertEquals("Access denied: This card is not assigned to this agent.", ex.getMessage());
    }

    @Test
    void activateCard_ShouldFail_WhenCardAlreadyUsed() {
        Long cardId = 1L;
        Long agentId = 10L;

        User agent = new User();
        agent.setId(agentId);

        Card card = new Card();
        card.setId(cardId);
        card.setAssignedTo(agent);
        card.setStatus(CardStatus.ACTIVATED);

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        Exception ex = assertThrows(IllegalStateException.class, () ->
                cardService.activateCard(cardId, agentId)
        );

        assertEquals("Card is already activated.", ex.getMessage());
    }


    @Test
    void getAllCards_shouldReturnMappedDTOs() {

        // Arrange
        Card card1 = new Card();
        card1.setId(1L);
        card1.setCode("CARD001");
        card1.setValue(100);
        card1.setStatus(CardStatus.valueOf("PENDING"));

        Card card2 = new Card();
        card2.setId(2L);
        card2.setCode("CARD002");
        card2.setValue(200);
        card2.setStatus(CardStatus.valueOf("USED"));


        List<Card> cardEntities = List.of(card1, card2);

        CardResponseDTO dto1 = new CardResponseDTO();
        dto1.setId(1L);
        dto1.setCode("CARD001");
        dto1.setValue(100);
        dto1.setStatus(CardStatus.valueOf("PENDING"));

        CardResponseDTO dto2 = new CardResponseDTO();
        dto2.setId(2L);
        dto2.setCode("CARD002");
        dto2.setValue(200);
        dto2.setStatus(CardStatus.valueOf("USED"));

        when(cardRepository.findAll()).thenReturn(cardEntities);
        when(cardMapper.toResponseDTO(card1)).thenReturn(dto1);
        when(cardMapper.toResponseDTO(card2)).thenReturn(dto2);

        // Act
        List<CardResponseDTO> result = cardService.getAllCards();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("CARD001", result.get(0).getCode());
        assertEquals("CARD002", result.get(1).getCode());

        verify(cardRepository, times(1)).findAll();
        verify(cardMapper, times(1)).toResponseDTO(card1);
        verify(cardMapper, times(1)).toResponseDTO(card2);
    }



}
