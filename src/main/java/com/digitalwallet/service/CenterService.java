package com.digitalwallet.service;

import com.digitalwallet.dto.CenterRequestDTO;
import com.digitalwallet.dto.CenterResponseDTO;
import com.digitalwallet.entity.CenterProfile;
import com.digitalwallet.entity.User;
import com.digitalwallet.entity.UserRole;
import com.digitalwallet.repository.CenterProfileRepository;
import com.digitalwallet.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CenterService {

    private static final Logger log = LoggerFactory.getLogger(CenterService.class);

    private final UserRepository userRepository;
    private final CenterProfileRepository centerProfileRepository;

    public CenterService(UserRepository userRepository, CenterProfileRepository centerProfileRepository) {
        this.userRepository = userRepository;
        this.centerProfileRepository = centerProfileRepository;
    }

    public CenterResponseDTO createCenter(Long currentUserId, CenterRequestDTO request) {

        validatePermission(currentUserId);

        log.info("Creating new center: {}", request.getEmail());


        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword()); // encode it later
        user.setRole(UserRole.CENTER);
        user.setBalance(0);

        User savedUser = userRepository.save(user);

        CenterProfile profile = new CenterProfile();
        profile.setUser(savedUser);
        profile.setLocation(request.getLocation());
        centerProfileRepository.save(profile);

        log.info("Center created successfully with ID: {}", savedUser.getId());

        return convertToDTO(savedUser, profile);
    }

    public List<CenterResponseDTO> getAllCenters(Long currentUserId) {

        validatePermission(currentUserId);

        log.info("Retrieving all centers");

        List<User> centers = userRepository.findByRole(UserRole.CENTER);

        return centers.stream()
                .map(user -> {
                    CenterProfile profile = centerProfileRepository.findById(user.getId()).orElse(null);
                    return convertToDTO(user, profile);
                })
                .collect(Collectors.toList());
    }

    public CenterResponseDTO updateCenter(Long currentUserId, Long id, CenterRequestDTO request) {

        validatePermission(currentUserId);

        log.info("Updating center with ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Center not found"));

        if (!user.getRole().equals(UserRole.CENTER)) {
            throw new IllegalArgumentException("User is not a center");
        }

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        userRepository.save(user);

        CenterProfile profile = centerProfileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Center profile not found"));

        profile.setLocation(request.getLocation());
        centerProfileRepository.save(profile);

        log.info("Center with ID {} updated successfully", id);
        return convertToDTO(user, profile);
    }

    public void deleteCenter(Long currentUserId, Long id) {

        validatePermission(currentUserId);

        log.info("Deleting center with ID: {}", id);

        CenterProfile profile = centerProfileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Center profile not found"));
        centerProfileRepository.delete(profile);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        userRepository.delete(user);

        log.info("Center with ID {} deleted", id);
    }




    private CenterResponseDTO convertToDTO(User user, CenterProfile profile) {
        CenterResponseDTO dto = new CenterResponseDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setLocation(profile != null ? profile.getLocation() : null);
        return dto;
    }

    private void validatePermission(Long userId) {
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!(currentUser.getRole() == UserRole.ADMIN || currentUser.getRole() == UserRole.AGENT)) {
            throw new SecurityException("Access denied: only Admin or Agent can perform this action");
        }
    }

}
