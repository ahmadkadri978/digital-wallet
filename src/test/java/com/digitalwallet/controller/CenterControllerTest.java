package com.digitalwallet.controller;

import com.digitalwallet.dto.CenterRequestDTO;
import com.digitalwallet.dto.CenterResponseDTO;
import com.digitalwallet.service.CenterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CenterController.class)
public class CenterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CenterService centerService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateCenterSuccessfully() throws Exception {
        CenterRequestDTO request = new CenterRequestDTO();
        request.setUsername("center1");
        request.setEmail("center@email.com");
        request.setPassword("pass123");
        request.setLocation("Aleppo");

        CenterResponseDTO response = new CenterResponseDTO();
        response.setId(1L);
        response.setUsername("center1");
        response.setEmail("center@email.com");
        response.setLocation("Aleppo");

        when(centerService.createCenter(eq(10L), any(CenterRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/centers")
                        .param("currentUserId", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("center1"));
    }

    @Test
    void shouldGetAllCentersSuccessfully() throws Exception {
        CenterResponseDTO center = new CenterResponseDTO();
        center.setId(1L);
        center.setUsername("center1");
        center.setEmail("center@email.com");
        center.setLocation("Homs");

        when(centerService.getAllCenters(10L)).thenReturn(List.of(center));

        mockMvc.perform(get("/api/centers")
                        .param("currentUserId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("center1"));
    }

    @Test
    void shouldUpdateCenterSuccessfully() throws Exception {
        CenterRequestDTO request = new CenterRequestDTO();
        request.setUsername("updated");
        request.setEmail("updated@email.com");
        request.setPassword("newpass");
        request.setLocation("Damascus");

        CenterResponseDTO response = new CenterResponseDTO();
        response.setId(1L);
        response.setUsername("updated");
        response.setEmail("updated@email.com");
        response.setLocation("Damascus");

        when(centerService.updateCenter(eq(10L), eq(1L), any(CenterRequestDTO.class))).thenReturn(response);

        mockMvc.perform(put("/api/centers/1")
                        .param("currentUserId", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("updated"));
    }

    @Test
    void shouldDeleteCenterSuccessfully() throws Exception {
        mockMvc.perform(delete("/api/centers/1")
                        .param("currentUserId", "10"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn403_WhenUnauthorized() throws Exception {
        CenterRequestDTO request = new CenterRequestDTO();
        request.setUsername("blocked");
        request.setEmail("bad@email.com");
        request.setPassword("123456");
        request.setLocation("Nowhere");

        when(centerService.createCenter(eq(99L), any(CenterRequestDTO.class)))
                .thenThrow(new SecurityException("Access denied: only Admin can perform this action"));

        mockMvc.perform(post("/api/centers")
                        .param("currentUserId", "99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Access denied: only Admin can perform this action"));
    }




}
