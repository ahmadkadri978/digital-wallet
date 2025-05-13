package com.digitalwallet.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    private Integer value;

    @Enumerated(EnumType.STRING)
    private CardStatus status;

    private boolean isUsed;

    @ManyToOne
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    @ManyToOne
    @JoinColumn(name = "batch_id")
    private File batch;

    private LocalDateTime createdAt;

    public Card() {
    }

    public Card(String code, Integer value, CardStatus status, boolean isUsed, User assignedTo, File batch, LocalDateTime createdAt) {
        this.code = code;
        this.value = value;
        this.status = status;
        this.isUsed = isUsed;
        this.assignedTo = assignedTo;
        this.batch = batch;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public CardStatus getStatus() {
        return status;
    }

    public void setStatus(CardStatus status) {
        this.status = status;
    }

    public boolean isUsed() {
        return isUsed;
    }

    public void setUsed(boolean isUsed) {
        this.isUsed = isUsed;
    }

    public User getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(User assignedTo) {
        this.assignedTo = assignedTo;
    }

    public File getBatch() {
        return batch;
    }

    public void setBatch(File batch) {
        this.batch = batch;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
