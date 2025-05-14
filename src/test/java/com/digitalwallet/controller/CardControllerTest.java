package com.digitalwallet.controller;

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

