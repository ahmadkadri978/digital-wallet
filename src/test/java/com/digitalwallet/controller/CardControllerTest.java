package com.digitalwallet.controller;

import com.digitalwallet.dto.AssignCardsToAgentRequestDTO;
import com.digitalwallet.dto.CardRequestDTO;
import com.digitalwallet.dto.CardResponseDTO;
import com.digitalwallet.entity.CardStatus;
import com.digitalwallet.service.CardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CardController.class)
public class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CardService cardService;

    @Autowired
    private ObjectMapper objectMapper;


    @Test
    void createCardBatch_ShouldReturnSuccess() throws Exception {
        CardRequestDTO request = new CardRequestDTO();
        request.setQuantity(3);
        request.setValue(5000);

        List<CardResponseDTO> mockResponse = List.of(
                createCardDTO(1L, "CODE1"),
                createCardDTO(2L, "CODE2"),
                createCardDTO(3L, "CODE3")
        );

        when(cardService.createCardBatch(any())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/cards/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].code").value("CODE1"));
    }

    @Test
    void createCardBatch_ShouldFail_WhenQuantityIsZero() throws Exception {
        CardRequestDTO request = new CardRequestDTO();
        request.setQuantity(0);
        request.setValue(5000);

        mockMvc.perform(post("/api/cards/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.quantity").value("Quantity must be at least 1"));
    }

    @Test
    void createCardBatch_ShouldFail_WhenValueIsBelowMinimum() throws Exception {
        CardRequestDTO request = new CardRequestDTO();
        request.setQuantity(5);
        request.setValue(500); // invalid

        mockMvc.perform(post("/api/cards/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.value").value("Value must be at least 1000"));
    }

    @Test
    void shouldAssignCardsSuccessfully() throws Exception {
        AssignCardsToAgentRequestDTO request = new AssignCardsToAgentRequestDTO();
        request.setAgentId(3L);
        request.setCardCodes(List.of("CODE1", "CODE2"));

        when(cardService.assignCardsToAgent(any())).thenReturn(2);

        mockMvc.perform(post("/api/cards/assign-to-agent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("2 cards successfully assigned to agent ID 3"));
    }

    @Test
    void shouldReturn400_WhenValidationFails() throws Exception {
        AssignCardsToAgentRequestDTO invalid = new AssignCardsToAgentRequestDTO();
        invalid.setAgentId(null);
        invalid.setCardCodes(List.of());

        mockMvc.perform(post("/api/cards/assign-to-agent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }


    @Test
    void getAllCards_shouldReturnListOfCardResponseDTO() throws Exception {
        CardResponseDTO card1 = new CardResponseDTO();
        card1.setId(1L);
        card1.setCode("CARD001");
        card1.setStatus(CardStatus.valueOf("PENDING"));

        CardResponseDTO card2 = new CardResponseDTO();
        card2.setId(2L);
        card2.setCode("CARD002");
        card2.setStatus(CardStatus.valueOf("USED"));

        List<CardResponseDTO> cards = List.of(card1, card2);

        when(cardService.getAllCards()).thenReturn(cards);

        mockMvc.perform(get("/api/cards/all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(cards.size()))
                .andExpect(jsonPath("$[0].code").value("CARD001"))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[1].code").value("CARD002"))
                .andExpect(jsonPath("$[1].status").value("USED"));

        verify(cardService, times(1)).getAllCards();
    }

    private CardResponseDTO createCardDTO(Long id, String code) {
        CardResponseDTO dto = new CardResponseDTO();
        dto.setId(id);
        dto.setCode(code);
        dto.setValue(5000);
        dto.setStatus(CardStatus.PENDING);
        dto.setUsed(false);
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }
}

