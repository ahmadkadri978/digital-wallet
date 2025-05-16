package com.digitalwallet.repository;

import com.digitalwallet.entity.Card;
import com.digitalwallet.entity.CardStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardRepository extends JpaRepository<Card, Long> {
     //Page<Card> findByStatus(CardStatus status, Pageable pageable);
    // Optional<Card> findByCode(String code);
     List<Card> findByBatchIsNullAndValue(int value);
}
