package com.digitalwallet.service;

import com.digitalwallet.dto.AgentRequestDTO;
import com.digitalwallet.dto.AgentResponseDTO;
import com.digitalwallet.entity.AgentProfile;
import com.digitalwallet.entity.User;
import com.digitalwallet.entity.UserRole;
import com.digitalwallet.repository.AgentProfileRepository;
import com.digitalwallet.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AgentServiceTest {

    private UserRepository userRepository;
    private AgentProfileRepository agentProfileRepository;
    private AgentService agentService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        agentProfileRepository = mock(AgentProfileRepository.class);
        agentService = new AgentService(userRepository, agentProfileRepository);
    }

    @Test
    void createAgent_ShouldCreateUserAndProfileSuccessfully() {
        AgentRequestDTO request = new AgentRequestDTO();
        request.setUsername("agent1");
        request.setEmail("agent1@email.com");
        request.setPassword("123456");
        request.setRegion("Damascus");

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername("agent1");
        savedUser.setEmail("agent1@email.com");
        savedUser.setPassword("123456");
        savedUser.setRole(UserRole.AGENT);

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(agentProfileRepository.save(any(AgentProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        AgentResponseDTO response = agentService.createAgent(request);

        assertNotNull(response);
        assertEquals("agent1", response.getUsername());
        assertEquals("Damascus", response.getRegion());
        assertEquals("agent1@email.com", response.getEmail());
        assertNotNull(response.getAssignedSince());
    }

    @Test
    void getAllAgents_ShouldReturnListOfAgents() {
        User user = new User();
        user.setId(1L);
        user.setUsername("agent1");
        user.setEmail("a@email.com");
        user.setRole(UserRole.AGENT);

        AgentProfile profile = new AgentProfile();
        profile.setUser(user);
        profile.setRegion("Homs");
        profile.setAssignedSince(LocalDateTime.now());

        when(userRepository.findByRole(UserRole.AGENT)).thenReturn(List.of(user));
        when(agentProfileRepository.findById(1L)).thenReturn(Optional.of(profile));

        List<AgentResponseDTO> agents = agentService.getAllAgents();

        assertEquals(1, agents.size());
        assertEquals("agent1", agents.get(0).getUsername());
        assertEquals("Homs", agents.get(0).getRegion());
    }

    @Test
    void updateAgent_ShouldUpdateUserAndProfile() {
        Long id = 1L;

        User existing = new User();
        existing.setId(id);
        existing.setUsername("old");
        existing.setEmail("old@email.com");
        existing.setRole(UserRole.AGENT);

        AgentProfile profile = new AgentProfile();
        profile.setUser(existing);
        profile.setRegion("OldRegion");

        AgentRequestDTO request = new AgentRequestDTO();
        request.setUsername("newName");
        request.setEmail("new@email.com");
        request.setPassword("pass123");
        request.setRegion("NewRegion");

        when(userRepository.findById(id)).thenReturn(Optional.of(existing));
        when(agentProfileRepository.findById(id)).thenReturn(Optional.of(profile));
        when(userRepository.save(any())).thenReturn(existing);
        when(agentProfileRepository.save(any())).thenReturn(profile);

        AgentResponseDTO updated = agentService.updateAgent(id, request);

        assertEquals("newName", updated.getUsername());
        assertEquals("NewRegion", updated.getRegion());
    }

    @Test
    void deleteAgent_ShouldDeleteBothProfileAndUser() {
        Long id = 1L;

        User user = new User();
        user.setId(id);

        AgentProfile profile = new AgentProfile();
        profile.setUser(user);

        when(agentProfileRepository.findById(id)).thenReturn(Optional.of(profile));
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        agentService.deleteAgent(id);

        verify(agentProfileRepository).delete(profile);
        verify(userRepository).delete(user);
    }
}

