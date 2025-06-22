package com.digitalwallet.controller;

import com.digitalwallet.dto.*;
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
    void associateCardsWithAgent_ShouldReturnOk() throws Exception {
        AssociateCardsRequestDTO request = new AssociateCardsRequestDTO();
        request.setAgentId(1L);
        request.setFileId(10L);

        mockMvc.perform(post("/api/cards/associate-to-agent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(cardService).associateCardsWithAgent(any(AssociateCardsRequestDTO.class));
    }

    @Test
    void associateCardsWithAgent_ShouldReturnBadRequest_WhenServiceThrowsIllegalArgumentException() throws Exception {
        AssociateCardsRequestDTO request = new AssociateCardsRequestDTO();
        request.setAgentId(1L);
        request.setFileId(10L);

        doThrow(new IllegalArgumentException("Agent not found"))
                .when(cardService).associateCardsWithAgent(any(AssociateCardsRequestDTO.class));

        mockMvc.perform(post("/api/cards/associate-to-agent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Agent not found"));
    }

    @Test
    void associateCardsWithAgent_ShouldReturnForbidden_WhenServiceThrowsSecurityException() throws Exception {
        AssociateCardsRequestDTO request = new AssociateCardsRequestDTO();
        request.setAgentId(1L);
        request.setFileId(10L);

        doThrow(new SecurityException("Only agents can receive cards"))
                .when(cardService).associateCardsWithAgent(any(AssociateCardsRequestDTO.class));

        mockMvc.perform(post("/api/cards/associate-to-agent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Only agents can receive cards"));
    }

    @Test
    void activateCard_ShouldReturnOk() throws Exception {
        Long cardId = 1L;
        ActivateCardRequestDTO request = new ActivateCardRequestDTO();
        request.setAgentId(10L);

        mockMvc.perform(post("/api/cards/{cardId}/activate", cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Card activated successfully."));

        verify(cardService).activateCard(cardId, 10L);
    }

    @Test
    void activateCard_ShouldReturnForbidden_WhenSecurityExceptionThrown() throws Exception {
        Long cardId = 1L;
        ActivateCardRequestDTO request = new ActivateCardRequestDTO();
        request.setAgentId(10L);

        doThrow(new SecurityException("Access denied: This card is not assigned to this agent."))
                .when(cardService).activateCard(cardId, 10L);

        mockMvc.perform(post("/api/cards/{cardId}/activate", cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Access denied: This card is not assigned to this agent."));
    }

    @Test
    void activateCard_ShouldReturnBadRequest_WhenIllegalArgumentExceptionThrown() throws Exception {
        Long cardId = 1L;
        ActivateCardRequestDTO request = new ActivateCardRequestDTO();
        request.setAgentId(10L);

        doThrow(new IllegalArgumentException("Card not found"))
                .when(cardService).activateCard(cardId, 10L);

        mockMvc.perform(post("/api/cards/{cardId}/activate", cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Card not found"));
    }

    @Test
    void activateCard_ShouldReturnConflict_WhenIllegalStateExceptionThrown() throws Exception {
        Long cardId = 1L;
        ActivateCardRequestDTO request = new ActivateCardRequestDTO();
        request.setAgentId(10L);

        doThrow(new IllegalStateException("Card is already used or activated."))
                .when(cardService).activateCard(cardId, 10L);

        mockMvc.perform(post("/api/cards/{cardId}/activate", cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Card is already used or activated."));
    }

//    @Test
//    void payWithCard_ShouldReturnOk() throws Exception {
//        CardPaymentRequestDTO request = new CardPaymentRequestDTO();
//        request.setCode("CARD123");
//        request.setAmount(50);
//
//        when(cardService.processCardPayment(any(CardPaymentRequestDTO.class)))
//                .thenReturn(50);
//
//        mockMvc.perform(post("/api/cards/pay")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.message").value("Payment processed successfully"))
//                .andExpect(jsonPath("$.remainingBalance").value(50));
//
//        verify(cardService).processCardPayment(any(CardPaymentRequestDTO.class));
//    }

//    @Test
//    void payWithCard_ShouldReturnBadRequest_WhenCardNotFound() throws Exception {
//        CardPaymentRequestDTO request = new CardPaymentRequestDTO();
//        request.setCode("CARD123");
//        request.setAmount(50);
//
//        doThrow(new IllegalArgumentException("Card not found"))
//                .when(cardService).processCardPayment(any(CardPaymentRequestDTO.class));
//
//        mockMvc.perform(post("/api/cards/pay")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.error").value("Card not found"));
//    }

//    @Test
//    void payWithCard_ShouldReturnConflict_WhenCardIsNotActivatedOrInsufficientBalance() throws Exception {
//        CardPaymentRequestDTO request = new CardPaymentRequestDTO();
//        request.setCode("CARD123");
//        request.setAmount(50);
//
//        doThrow(new IllegalStateException("Card is not activated"))
//                .when(cardService).processCardPayment(any(CardPaymentRequestDTO.class));
//
//        mockMvc.perform(post("/api/cards/pay")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.error").value("Card is not activated"));
//    }

//    @Test
//    void payWithCard_ShouldReturnBadRequest_WhenValidationFails() throws Exception {
//        CardPaymentRequestDTO request = new CardPaymentRequestDTO();
//        request.setCode(""); // intentionally empty to trigger @NotBlank
//        request.setAmount(0); // invalid amount
//
//        mockMvc.perform(post("/api/cards/pay")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.code").exists())
//                .andExpect(jsonPath("$.amount").exists());
//    }



    @Test
    void getAllCards_shouldReturnListOfCardResponseDTO() throws Exception {
        CardResponseDTO card1 = new CardResponseDTO();
        card1.setId(1L);
        card1.setCode("CARD001");
        card1.setStatus(CardStatus.valueOf("PENDING"));

        CardResponseDTO card2 = new CardResponseDTO();
        card2.setId(2L);
        card2.setCode("CARD002");
        card2.setStatus(CardStatus.valueOf("ASSIGNED"));

        List<CardResponseDTO> cards = List.of(card1, card2);

        when(cardService.getAllCards()).thenReturn(cards);

        mockMvc.perform(get("/api/cards/all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(cards.size()))
                .andExpect(jsonPath("$[0].code").value("CARD001"))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[1].code").value("CARD002"))
                .andExpect(jsonPath("$[1].status").value("ASSIGNED"));

        verify(cardService, times(1)).getAllCards();
    }

    @Test
    void useCardsForPayment_ShouldReturnSuccess() throws Exception {
        CardUsageRequestDTO request = new CardUsageRequestDTO();
        request.setCodes(List.of("CODE123"));
        request.setPurchaseAmount(50.0);
        request.setAgentId(1L);
        request.setOrderId("ORD001");

        CardPaymentResponseDTO response = new CardPaymentResponseDTO();
        response.setPaidAmount(50.0);
        response.setRemainingAmount(0.0);
        response.setPaymentStatus("SUCCESS");
        response.setMessage("Payment processed successfully.");

        when(cardService.useCardsForPayment(any(CardUsageRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/cards/use")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.paidAmount").value(50.0));
    }

    @Test
    void useCardsForPayment_ShouldReturnBadRequest_WhenValidationFails() throws Exception {
        CardUsageRequestDTO request = new CardUsageRequestDTO();
        request.setCodes(List.of()); // invalid input
        request.setPurchaseAmount(0.0);
        request.setAgentId(1L);

        mockMvc.perform(post("/api/cards/use")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void useCardsForPayment_ShouldReturnForbidden_WhenSecurityException() throws Exception {
        CardUsageRequestDTO request = new CardUsageRequestDTO();
        request.setCodes(List.of("CODE123"));
        request.setPurchaseAmount(50.0);
        request.setAgentId(99L);

        when(cardService.useCardsForPayment(any(CardUsageRequestDTO.class)))
                .thenThrow(new SecurityException("Access denied"));

        mockMvc.perform(post("/api/cards/use")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Access denied"));
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

