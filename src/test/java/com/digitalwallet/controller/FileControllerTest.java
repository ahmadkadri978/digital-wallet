package com.digitalwallet.controller;

import com.digitalwallet.dto.FileRequestDTO;
import com.digitalwallet.dto.FileResponseDTO;
import com.digitalwallet.entity.FileStatus;
import com.digitalwallet.service.FileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FileController.class)
public class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileService fileService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldReturnSuccess_WhenFileCreated() throws Exception {
        FileRequestDTO request = new FileRequestDTO();
        request.setQuantity(5);
        request.setValue(5000);
        request.setType("PDF");

        FileResponseDTO response = new FileResponseDTO();
        response.setId(1L);
        response.setType("PDF");
        response.setStatus(FileStatus.PENDING.toString());
        response.setCreatedAt(LocalDateTime.now());
        response.setCreatedBy(1L);

        when(fileService.createFile(any(FileRequestDTO.class), any(Long.class))).thenReturn(response);

        mockMvc.perform(post("/api/files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.type").value("PDF"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldFailValidation_WhenQuantityOrValueInvalid() throws Exception {
        FileRequestDTO request = new FileRequestDTO();
        request.setQuantity(0);
        request.setValue(500);
        request.setType("PDF");

        mockMvc.perform(post("/api/files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.quantity").value("Quantity must be at least 1"))
                .andExpect(jsonPath("$.value").value("Card value must be at least 1000"));
    }

    @Test
    void shouldReturnError_WhenCardsAreInsufficient() throws Exception {
        FileRequestDTO request = new FileRequestDTO();
        request.setQuantity(10);
        request.setValue(5000);
        request.setType("PDF");

        when(fileService.createFile(any(FileRequestDTO.class), any(Long.class)))
                .thenThrow(new IllegalArgumentException("Please generate 5 more cards with value 5000"));

        mockMvc.perform(post("/api/files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Please generate 5 more cards with value 5000"));
    }
}
