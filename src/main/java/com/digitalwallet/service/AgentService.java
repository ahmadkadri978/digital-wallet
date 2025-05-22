package com.digitalwallet.service;

import com.digitalwallet.dto.AgentRequestDTO;
import com.digitalwallet.dto.AgentResponseDTO;
import com.digitalwallet.entity.AgentProfile;
import com.digitalwallet.entity.User;
import com.digitalwallet.entity.UserRole;
import com.digitalwallet.repository.AgentProfileRepository;
import com.digitalwallet.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AgentService {
    private static final Logger log = LoggerFactory.getLogger(AgentService.class);
    private final UserRepository userRepository;
    private final AgentProfileRepository agentProfileRepository;

    public AgentService(UserRepository userRepository, AgentProfileRepository agentProfileRepository) {
        this.userRepository = userRepository;
        this.agentProfileRepository = agentProfileRepository;
    }

    public AgentResponseDTO createAgent(Long currentUserId,AgentRequestDTO request) {
        // Create a new user:agent
        validatePermission(currentUserId);

        log.info("Creating new agent: {}", request.getEmail());
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword()); // In the future:encryption
        user.setRole(UserRole.AGENT);
        user.setBalance(0);
        User savedUser = userRepository.save(user);

        // Create agent file
        AgentProfile profile = new AgentProfile();
        profile.setUser(savedUser);
        profile.setRegion(request.getRegion());
        profile.setAssignedSince(LocalDateTime.now());
        agentProfileRepository.save(profile);

        log.info("Agent created successfully with ID: {}", savedUser.getId());
        return convertToDTO(savedUser, profile);
    }

    private AgentResponseDTO convertToDTO(User user, AgentProfile profile) {
        AgentResponseDTO dto = new AgentResponseDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setRegion(profile.getRegion());
        dto.setAssignedSince(profile.getAssignedSince());
        return dto;
    }

    public List<AgentResponseDTO> getAllAgents(Long currentUserId) {

        validatePermission(currentUserId);

        log.info("Retrieving all agents");
        List<User> agents = userRepository.findByRole(UserRole.AGENT);
        return agents.stream()
                .map(user -> {
                    AgentProfile profile = agentProfileRepository.findById(user.getId()).orElse(null);
                    return convertToDTO(user, profile);
                })
                .collect(Collectors.toList());
    }

    public AgentResponseDTO updateAgent(Long currentUserId, Long id, AgentRequestDTO request) {

        validatePermission(currentUserId);

        log.info("Updating agent with ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agent not found"));

        if (!user.getRole().equals(UserRole.AGENT)) {
            throw new IllegalArgumentException("User is not an agent");
        }

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword()); // In the future: encryption
        userRepository.save(user);

        AgentProfile profile = agentProfileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agent profile not found"));

        profile.setRegion(request.getRegion());
        agentProfileRepository.save(profile);

        log.info("Agent with ID {} updated successfully", id);
        return convertToDTO(user, profile);
    }

    public void deleteAgent(Long currentUserId, Long id) {

        validatePermission(currentUserId);

        log.info("Deleting agent with ID: {}", id);
        AgentProfile profile = agentProfileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agent profile not found"));
        agentProfileRepository.delete(profile);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        userRepository.delete(user);
        log.info("Agent with ID {} deleted", id);
    }

    private void validatePermission(Long userId) {
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!(currentUser.getRole() == UserRole.ADMIN )) {
            throw new SecurityException("Access denied: only Admin can perform this action");
        }
    }

}
