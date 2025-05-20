package com.digitalwallet.controller;

import com.digitalwallet.dto.AgentRequestDTO;
import com.digitalwallet.dto.AgentResponseDTO;
import com.digitalwallet.service.AgentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AgentController.class)
public class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AgentService agentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateAgentSuccessfully() throws Exception {
        AgentRequestDTO request = new AgentRequestDTO();
        request.setUsername("agent1");
        request.setEmail("agent1@email.com");
        request.setPassword("123456");
        request.setRegion("Homs");

        AgentResponseDTO response = new AgentResponseDTO();
        response.setId(1L);
        response.setUsername("agent1");
        response.setEmail("agent1@email.com");
        response.setRegion("Homs");
        response.setAssignedSince(LocalDateTime.now());

        when(agentService.createAgent(any(AgentRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/agents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("agent1"));
    }

    @Test
    void shouldGetAllAgents() throws Exception {
        AgentResponseDTO agent = new AgentResponseDTO();
        agent.setId(1L);
        agent.setUsername("agent1");
        agent.setEmail("agent1@email.com");
        agent.setRegion("Homs");
        agent.setAssignedSince(LocalDateTime.now());

        when(agentService.getAllAgents()).thenReturn(List.of(agent));

        mockMvc.perform(get("/api/agents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username").value("agent1"));
    }

    @Test
    void shouldUpdateAgent() throws Exception {
        Long id = 1L;
        AgentRequestDTO request = new AgentRequestDTO();
        request.setUsername("updated");
        request.setEmail("new@email.com");
        request.setPassword("newpass");
        request.setRegion("Aleppo");

        AgentResponseDTO response = new AgentResponseDTO();
        response.setId(id);
        response.setUsername("updated");
        response.setEmail("new@email.com");
        response.setRegion("Aleppo");
        response.setAssignedSince(LocalDateTime.now());

        when(agentService.updateAgent(eq(id), any(AgentRequestDTO.class))).thenReturn(response);

        mockMvc.perform(put("/api/agents/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("updated"));
    }

    @Test
    void shouldDeleteAgent() throws Exception {
        Long id = 1L;

        doNothing().when(agentService).deleteAgent(id);

        mockMvc.perform(delete("/api/agents/{id}", id))
                .andExpect(status().isNoContent());
    }
}
