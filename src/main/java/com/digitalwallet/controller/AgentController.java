package com.digitalwallet.controller;

import com.digitalwallet.dto.AgentRequestDTO;
import com.digitalwallet.dto.AgentResponseDTO;
import com.digitalwallet.service.AgentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agents")
public class AgentController {

    private static final Logger log = LoggerFactory.getLogger(AgentController.class);

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping
    public ResponseEntity<AgentResponseDTO> createAgent(@RequestParam Long currentUserId,
                                                        @RequestBody @Valid AgentRequestDTO request) {

        log.info("Received request to create agent: {}", request.getEmail());
        AgentResponseDTO response = agentService.createAgent(currentUserId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<AgentResponseDTO>> getAllAgents(@RequestParam Long currentUserId) {

        log.info("Received request to get all agents");
        List<AgentResponseDTO> agents = agentService.getAllAgents(currentUserId);
        return ResponseEntity.ok(agents);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AgentResponseDTO> updateAgent(@RequestParam Long currentUserId,
                                                        @PathVariable Long id, @RequestBody @Valid AgentRequestDTO request) {

        log.info("Received request to update agent ID: {}", id);
        AgentResponseDTO updated = agentService.updateAgent(currentUserId, id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAgent(@RequestParam Long currentUserId,@PathVariable Long id) {

        log.info("Received request to delete agent ID: {}", id);
        agentService.deleteAgent(currentUserId, id);
        return ResponseEntity.noContent().build();
    }
}
