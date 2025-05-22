package com.digitalwallet.service;

import com.digitalwallet.dto.AgentRequestDTO;
import com.digitalwallet.dto.AgentResponseDTO;
import com.digitalwallet.dto.CenterRequestDTO;
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
    void createAgent_ShouldSucceed_WhenUserIsAuthorized() {
        Long adminId = 1L;

        User adminUser = new User();
        adminUser.setId(adminId);
        adminUser.setRole(UserRole.ADMIN);

        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));

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

        AgentResponseDTO response = agentService.createAgent(adminId, request);

        assertNotNull(response);
        assertEquals("agent1", response.getUsername());
        assertEquals("Damascus", response.getRegion());
        assertEquals("agent1@email.com", response.getEmail());
        assertNotNull(response.getAssignedSince());
    }
    @Test
    void createAgent_ShouldFail_WhenUserIsUnauthorized() {
        Long userId = 2L;

        User endUser = new User();
        endUser.setId(userId);
        endUser.setRole(UserRole.END_USER); // Not Agent / Admin

        when(userRepository.findById(userId)).thenReturn(Optional.of(endUser));

        AgentRequestDTO request = new AgentRequestDTO();
        request.setUsername("agent1");
        request.setEmail("agent1@email.com");
        request.setPassword("123456");
        request.setRegion("Damascus");

        Exception ex = assertThrows(SecurityException.class, () ->
                agentService.createAgent(userId, request)
        );
        assertEquals("Access denied: only Admin can perform this action", ex.getMessage());
    }

    @Test
    void getAllAgents_ShouldReturnListOfAgents() {

        Long adminId = 3L;

        User admin = new User();
        admin.setId(adminId);
        admin.setRole(UserRole.ADMIN);


        User user = new User();
        user.setId(1L);
        user.setUsername("agent1");
        user.setEmail("a@email.com");
        user.setRole(UserRole.AGENT);

        AgentProfile profile = new AgentProfile();
        profile.setUser(user);
        profile.setRegion("Homs");
        profile.setAssignedSince(LocalDateTime.now());

        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(userRepository.findByRole(UserRole.AGENT)).thenReturn(List.of(user));
        when(agentProfileRepository.findById(1L)).thenReturn(Optional.of(profile));

        List<AgentResponseDTO> agents = agentService.getAllAgents(adminId);

        assertEquals(1, agents.size());
        assertEquals("agent1", agents.get(0).getUsername());
        assertEquals("Homs", agents.get(0).getRegion());
    }

    @Test
    void getAllAgents_ShouldFail_WhenUnauthorized() {
        Long currentUserId = 4L;

        User unauthorized = new User();
        unauthorized.setId(currentUserId);
        unauthorized.setRole(UserRole.END_USER);

        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(unauthorized));

        Exception ex = assertThrows(SecurityException.class, () ->
                agentService.getAllAgents(currentUserId)
        );

        assertEquals("Access denied: only Admin can perform this action", ex.getMessage());
    }


    @Test
    void updateAgent_ShouldUpdateUserAndProfile() {
        Long adminId = 1L;
        Long targetId = 100L;

        User admin = new User();
        admin.setId(adminId);
        admin.setRole(UserRole.ADMIN);

        User existing = new User();
        existing.setId(targetId);
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

        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(userRepository.findById(targetId)).thenReturn(Optional.of(existing));
        when(agentProfileRepository.findById(targetId)).thenReturn(Optional.of(profile));
        when(userRepository.save(any())).thenReturn(existing);
        when(agentProfileRepository.save(any())).thenReturn(profile);

        AgentResponseDTO updated = agentService.updateAgent(adminId, targetId, request);

        assertEquals("newName", updated.getUsername());
        assertEquals("NewRegion", updated.getRegion());
    }

    @Test
    void updateAgent_ShouldFail_WhenUnauthorized() {
        Long currentUserId = 2L;
        Long targetAgentId = 100L;

        User endUser = new User();
        endUser.setId(currentUserId);
        endUser.setRole(UserRole.END_USER);

        AgentRequestDTO request = new AgentRequestDTO();
        request.setUsername("new");
        request.setEmail("email");
        request.setPassword("123456");
        request.setRegion("region");

        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(endUser));

        Exception ex = assertThrows(SecurityException.class, () ->
                agentService.updateAgent(currentUserId, targetAgentId, request)
        );

        assertEquals("Access denied: only Admin can perform this action", ex.getMessage());
    }





    @Test
    void deleteAgent_ShouldDeleteBothProfileAndUser() {
        Long currentUserId = 1L;
        Long targetId = 100L;

        User admin = new User();
        admin.setId(currentUserId);
        admin.setRole(UserRole.ADMIN);

        User user = new User();
        user.setId(targetId);

        AgentProfile profile = new AgentProfile();
        profile.setUser(user);

        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(admin));
        when(agentProfileRepository.findById(targetId)).thenReturn(Optional.of(profile));
        when(userRepository.findById(targetId)).thenReturn(Optional.of(user));

        agentService.deleteAgent(currentUserId, targetId);

        verify(agentProfileRepository).delete(profile);
        verify(userRepository).delete(user);
    }

    @Test
    void deleteAgent_ShouldFail_WhenUnauthorized() {
        Long currentUserId = 5L;
        Long targetAgentId = 101L;

        User webshop = new User();
        webshop.setId(currentUserId);
        webshop.setRole(UserRole.WEBSHOP);

        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(webshop));

        Exception ex = assertThrows(SecurityException.class, () ->
                agentService.deleteAgent(currentUserId, targetAgentId)
        );

        assertEquals("Access denied: only Admin can perform this action", ex.getMessage());
    }


}

