package com.digitalwallet.service;

import com.digitalwallet.dto.CenterRequestDTO;
import com.digitalwallet.dto.CenterResponseDTO;
import com.digitalwallet.entity.CenterProfile;
import com.digitalwallet.entity.User;
import com.digitalwallet.entity.UserRole;
import com.digitalwallet.repository.CenterProfileRepository;
import com.digitalwallet.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CenterServiceTest {

    private UserRepository userRepository;
    private CenterProfileRepository centerProfileRepository;
    private CenterService centerService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        centerProfileRepository = mock(CenterProfileRepository.class);
        centerService = new CenterService(userRepository, centerProfileRepository);
    }

    @Test
    void createCenter_ShouldSucceed_WhenUserIsAuthorized() {
        Long adminOrAgentId = 1L;

        User adminUser = new User();
        adminUser.setId(adminOrAgentId);
        adminUser.setRole(UserRole.ADMIN);

        when(userRepository.findById(adminOrAgentId)).thenReturn(Optional.of(adminUser));

        CenterRequestDTO request = new CenterRequestDTO();
        request.setUsername("center1");
        request.setEmail("center1@email.com");
        request.setPassword("123456");
        request.setLocation("Damascus");

        User savedCenter = new User();
        savedCenter.setId(10L);
        savedCenter.setUsername("center1");
        savedCenter.setEmail("center1@email.com");
        savedCenter.setRole(UserRole.CENTER);

        when(userRepository.save(any(User.class))).thenReturn(savedCenter);
        when(centerProfileRepository.save(any(CenterProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        CenterResponseDTO result = centerService.createCenter(adminOrAgentId, request);

        assertNotNull(result);
        assertEquals("center1", result.getUsername());
        assertEquals("Damascus", result.getLocation());
    }

    @Test
    void createCenter_ShouldFail_WhenUserIsUnauthorized() {
        Long userId = 2L;

        User endUser = new User();
        endUser.setId(userId);
        endUser.setRole(UserRole.END_USER); // Not Agent / Admin

        when(userRepository.findById(userId)).thenReturn(Optional.of(endUser));

        CenterRequestDTO request = new CenterRequestDTO();
        request.setUsername("unauthorized");
        request.setEmail("bad@email.com");
        request.setPassword("123456");
        request.setLocation("Aleppo");

        Exception ex = assertThrows(SecurityException.class, () ->
                centerService.createCenter(userId, request)
        );

        assertEquals("Access denied: only Admin or Agent can perform this action", ex.getMessage());
    }

    @Test
    void getAllCenters_ShouldSucceed_WhenAuthorized() {
        Long adminOrAgentId = 3L;

        User adminOrAgent = new User();
        adminOrAgent.setId(adminOrAgentId);
        adminOrAgent.setRole(UserRole.AGENT);

        User centerUser = new User();
        centerUser.setId(100L);
        centerUser.setUsername("centerA");
        centerUser.setEmail("a@email.com");
        centerUser.setRole(UserRole.CENTER);

        CenterProfile profile = new CenterProfile();
        profile.setUser(centerUser);
        profile.setLocation("Homs");

        when(userRepository.findById(adminOrAgentId)).thenReturn(Optional.of(adminOrAgent));
        when(userRepository.findByRole(UserRole.CENTER)).thenReturn(List.of(centerUser));
        when(centerProfileRepository.findById(100L)).thenReturn(Optional.of(profile));

        var results = centerService.getAllCenters(adminOrAgentId);

        assertEquals(1, results.size());
        assertEquals("centerA", results.get(0).getUsername());
    }

    @Test
    void updateCenter_ShouldSucceed_WhenAuthorized() {
        Long currentUserId = 1L;
        Long centerId = 10L;

        User admin = new User();
        admin.setId(currentUserId);
        admin.setRole(UserRole.ADMIN);

        User centerUser = new User();
        centerUser.setId(centerId);
        centerUser.setUsername("oldCenter");
        centerUser.setEmail("old@email.com");
        centerUser.setRole(UserRole.CENTER);

        CenterProfile profile = new CenterProfile();
        profile.setUser(centerUser);
        profile.setLocation("OldLocation");

        CenterRequestDTO request = new CenterRequestDTO();
        request.setUsername("newCenter");
        request.setEmail("new@email.com");
        request.setPassword("newpass");
        request.setLocation("NewLocation");

        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(admin));
        when(userRepository.findById(centerId)).thenReturn(Optional.of(centerUser));
        when(centerProfileRepository.findById(centerId)).thenReturn(Optional.of(profile));
        when(userRepository.save(any())).thenReturn(centerUser);
        when(centerProfileRepository.save(any())).thenReturn(profile);

        var response = centerService.updateCenter(currentUserId, centerId, request);

        assertNotNull(response);
        assertEquals("newCenter", response.getUsername());
        assertEquals("NewLocation", response.getLocation());
    }


    @Test
    void updateCenter_ShouldFail_WhenUnauthorized() {
        Long currentUserId = 2L;

        User webshop = new User();
        webshop.setId(currentUserId);
        webshop.setRole(UserRole.WEBSHOP);

        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(webshop));

        CenterRequestDTO request = new CenterRequestDTO();
        request.setUsername("updateFail");
        request.setEmail("fail@email.com");
        request.setPassword("123456");
        request.setLocation("Latakia");

        Exception ex = assertThrows(SecurityException.class, () ->
                centerService.updateCenter(currentUserId, 10L, request)
        );

        assertEquals("Access denied: only Admin or Agent can perform this action", ex.getMessage());
    }

    @Test
    void deleteCenter_ShouldFail_WhenUnauthorized() {
        Long adminOrAgentId = 99L;

        User unauthorized = new User();
        unauthorized.setId(adminOrAgentId);
        unauthorized.setRole(UserRole.END_USER);

        when(userRepository.findById(adminOrAgentId)).thenReturn(Optional.of(unauthorized));

        Exception ex = assertThrows(SecurityException.class, () ->
                centerService.deleteCenter(adminOrAgentId, 5L)
        );

        assertEquals("Access denied: only Admin or Agent can perform this action", ex.getMessage());
    }

}

