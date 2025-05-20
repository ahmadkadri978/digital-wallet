package com.digitalwallet.repository;

import com.digitalwallet.entity.AgentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgentProfileRepository extends JpaRepository<AgentProfile, Long> {
}
