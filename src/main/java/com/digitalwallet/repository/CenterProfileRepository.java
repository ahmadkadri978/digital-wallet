package com.digitalwallet.repository;

import com.digitalwallet.entity.CenterProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CenterProfileRepository extends JpaRepository<CenterProfile, Long> {
}
