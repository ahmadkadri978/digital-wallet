package com.digitalwallet.repository;

import com.digitalwallet.entity.CardTransactionLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardTransactionLogRepository extends JpaRepository<CardTransactionLog, Long> {
}
