package com.digitalwallet.service;

import com.digitalwallet.dto.*;
import com.digitalwallet.entity.*;
import com.digitalwallet.mapper.CardMapper;
import com.digitalwallet.repository.CardRepository;
import com.digitalwallet.repository.CardTransactionLogRepository;
import com.digitalwallet.repository.FileRepository;
import com.digitalwallet.repository.UserRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import jakarta.transaction.Transactional;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@Service
public class CardService {

    private static final Logger log = LoggerFactory.getLogger(CardService.class);
    private final CardRepository cardRepository;
    private final FileRepository fileRepository;
    private final UserRepository userRepository;

    private final CardTransactionLogRepository transactionLogRepository;
    private final CardMapper cardMapper;

    public CardService(FileRepository fileRepository, CardRepository cardRepository, UserRepository userRepository, CardTransactionLogRepository transactionLogRepository,CardMapper cardMapper) {
        this.fileRepository = fileRepository;
        this.cardRepository = cardRepository;
        this.userRepository = userRepository;
        this.transactionLogRepository = transactionLogRepository;
        this.cardMapper = cardMapper;

    }

    public List<CardResponseDTO> createCardBatch(CardRequestDTO request) {

        if (request.getQuantity() <= 0 || request.getValue() <= 1000) {
            log.error("Invalid input: quantity={} or value={}", request.getQuantity(), request.getValue());
            throw new IllegalArgumentException("Quantity must be > 0 and value must be >= 1000");
        }

        log.info("Start creating card batch: quantity={}, value={}", request.getQuantity(), request.getValue());

        List<Card> cardBatch = IntStream.range(0, request.getQuantity())
                .mapToObj(i -> {
                    Card card = new Card();
                    card.setCode(generateUniqueCode());
                    card.setValue(request.getValue());
                    card.setStatus(CardStatus.PENDING);
                    card.setUsed(false);
                    card.setCreatedAt(LocalDateTime.now());
                    card.setQrImageBase64(generateQrBase64(card.getCode()));

                    return card;
                })
                .collect(Collectors.toList());

        List<Card> savedCards = cardRepository.saveAll(cardBatch);

        log.info("Successfully created {} cards", savedCards.size());

        return convertToDTOList(savedCards);
    }

    @Transactional
    public int assignCardsToAgent(AssignCardsToAgentRequestDTO request) {
        log.info("Assigning {} cards to agent ID {}", request.getCardCodes().size(), request.getAgentId());

        User agent = userRepository.findById(request.getAgentId())
                .orElseThrow(() -> new IllegalArgumentException("Agent not found"));

        if (!agent.getRole().equals(UserRole.AGENT)) {
            throw new IllegalArgumentException("Provided user is not an agent");
        }

        List<Card> cards = cardRepository.findAllByCodeIn(request.getCardCodes());


        if (cards.size() != request.getCardCodes().size()) {
            throw new IllegalArgumentException("Some cards do not exist");
        }

        long invalidCount = cards.stream()
                .filter(card -> card.isUsed()
                        || card.getAssignedTo() != null
                        || card.getBatch() == null
                        || card.getBatch().getStatus() != FileStatus.PRINTED)
                .count();

        if (invalidCount > 0) {
            throw new IllegalArgumentException("Some cards are already assigned, used, or not printed");
        }

        cards.forEach(card -> card.setAssignedTo(agent));

        cardRepository.saveAll(cards);

        log.info("Successfully assigned {} cards to agent ID {}", cards.size(), agent.getId());

        return cards.size();
    }

        public void associateCardsWithAgent(AssociateCardsRequestDTO request) {
            // Validate Agent

            log.info("Associating cards from file ID {} to agent ID {}", request.getFileId(), request.getAgentId());

            User agent = userRepository.findById(request.getAgentId())
                    .orElseThrow(() -> new IllegalArgumentException("Agent not found"));

            if (!agent.getRole().equals(UserRole.AGENT)) {
                throw new SecurityException("Only agents can receive cards");
            }

            // Validate File
            File file = fileRepository.findById(request.getFileId())
                    .orElseThrow(() -> new IllegalArgumentException("File not found"));

            // Fetch Cards
            List<Card> cards = cardRepository.findByBatch(file);
            log.info("Found {} cards linked to file ID {}", cards.size(), file.getId());

            // Associate Cards
            boolean anyAlreadyAssigned = cards.stream().anyMatch(card -> card.getAssignedTo() != null);
            if (anyAlreadyAssigned) {
                throw new IllegalStateException("Some cards are already assigned to another agent. Please contact support.");
            }
            cards.forEach(card -> card.setAssignedTo(agent));
            cards.forEach(card -> card.setStatus(CardStatus.ASSIGNED));
            cardRepository.saveAll(cards);

            log.info("Successfully associated {} cards with agent ID {}", cards.size(), agent.getId());
        }

    public void assignCardsToCenter(Long currentUserId, AssignCardsToCenterRequestDTO request) {
        log.info("User {} is attempting to assign cards to center {}", currentUserId, request.getPurchaseCenterId());

        // Verifying that the current user is an Agent
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (currentUser.getRole() != UserRole.AGENT) {
            throw new SecurityException("Only AGENT users can assign cards to centers");
        }

        // Verifying that the center exists
        User purchaseCenter = userRepository.findById(request.getPurchaseCenterId())
                .orElseThrow(() -> new IllegalArgumentException("Purchase center not found"));

        if (purchaseCenter.getRole() != UserRole.CENTER) {
            throw new IllegalArgumentException("Target user is not a valid purchase center");
        }

        // Fetching cards based on QR codes
        List<Card> cards = cardRepository.findAllByCodeIn(request.getCardQRCodes());

        if (cards.size() != request.getCardQRCodes().size()) {
            throw new IllegalArgumentException("Some cards were not found by provided QR codes");
        }

        // Verify that the cards are printed and assigned to the agent.
        for (Card card : cards) {
            if (card.getBatch() == null || card.getBatch().getStatus() != FileStatus.PRINTED) {
                throw new IllegalStateException("Card " + card.getCode() + " does not belong to a printed file");
            }

            if (card.getAssignedTo() == null || !card.getAssignedTo().getId().equals(currentUserId)) {
                throw new SecurityException("Agent is not allowed to assign card " + card.getCode() + " that is not owned by them");
            }
        }

        // Assigning cards to the center
        cards.forEach(card -> card.setAssignedTo(purchaseCenter));
        cardRepository.saveAll(cards);

        log.info("Assigned {} cards to center ID {} by agent ID {}", cards.size(), purchaseCenter.getId(), currentUserId);
    }



    public List<CardResponseDTO> getAllCards() {
        List<Card> cards = cardRepository.findAll();
        return cards.stream()
                .map(cardMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    public void activateCard(Long cardId, Long agentId) {
        log.info("Agent ID {} is requesting activation for Card ID {}", agentId, cardId);

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));

        if (card.getAssignedTo() == null || !card.getAssignedTo().getId().equals(agentId)) {
            throw new SecurityException("Access denied: This card is not assigned to this agent.");
        }

        if (card.getStatus() == CardStatus.ACTIVATED) {
            throw new IllegalStateException("Card is already activated.");
        }

        card.setStatus(CardStatus.ACTIVATED);
        cardRepository.save(card);

        log.info("Card ID {} has been activated successfully by Agent ID {}", cardId, agentId);
    }


    @Transactional
    public int processCardPayment(CardPaymentRequestDTO request) {
        log.info("Processing payment with card code {}", request.getCode());

        // 1. Fetch card by code
        Optional<Card> optionalCard = cardRepository.findByCode(request.getCode());
        if (optionalCard.isEmpty()) {
            throw new IllegalArgumentException("Card not found");
        }

        Card card = optionalCard.get();

        // 2. Validate card status
        if (card.getStatus() != CardStatus.ACTIVATED) {
            throw new IllegalStateException("Card is not activated");
        }

        // 3. Check balance
        if (card.getValue() < request.getAmount()) {
            throw new IllegalStateException("Insufficient balance on the card");
        }

        // 4. Deduct amount
        card.setValue(card.getValue() - request.getAmount());

        // 5. Save updated card
        cardRepository.save(card);

        log.info("Payment of {} processed successfully using card code {}", request.getAmount(), request.getCode());

        return card.getValue();
    }

    @Transactional
    public CardPaymentResponseDTO useCardsForPayment(CardUsageRequestDTO request) {
        log.info("Processing payment request: {}", request);

        double purchaseAmount = request.getPurchaseAmount();
        double remainingAmount = purchaseAmount;
        double paidAmount = 0.0;

        List<Card> cards = cardRepository.findByCodeIn(request.getCodes());

        if (cards.isEmpty()) {
            throw new IllegalArgumentException("No cards found for the provided codes.");
        }

        for (Card card : cards) {
            log.info("Processing card: {}", card.getCode());

            if (card.getStatus() != CardStatus.ACTIVATED) {
                throw new IllegalStateException("Card with code " + card.getCode() + " is not activated.");
            }

            if (!card.getAssignedTo().getId().equals(request.getAgentId())) {
                throw new SecurityException("Access denied: card does not belong to this agent.");
            }

            double availableBalance = card.getValue();

            if (availableBalance >= remainingAmount) {
                card.setValue((int) (availableBalance - remainingAmount));
                paidAmount += remainingAmount;
                log.info("Deducted {} from card {}", remainingAmount, card.getCode());
                remainingAmount = 0.0;
            } else {
                card.setValue(0);
                paidAmount += availableBalance;
                remainingAmount -= availableBalance;
                log.info("Deducted {} from card {}. Remaining to be paid: {}", availableBalance, card.getCode(), remainingAmount);
            }

            cardRepository.save(card);

            // Log each card transaction
            CardTransactionLog logEntry = new CardTransactionLog();
            logEntry.setCardCode(card.getCode());
            logEntry.setAmountDeducted(paidAmount);
            logEntry.setRemainingBalance(card.getValue());
            logEntry.setOrderId(request.getOrderId());
            logEntry.setTransactionDate(LocalDateTime.now());
            logEntry.setAgentId(request.getAgentId());
            logEntry.setPaymentStatus(remainingAmount > 0 ? PaymentStatus.FAILED.name() : PaymentStatus.SUCCESS.name());
            transactionLogRepository.save(logEntry);

            if (remainingAmount == 0.0) {
                break;
            }
        }

        // Prepare Response DTO
        CardPaymentResponseDTO response = new CardPaymentResponseDTO();
        response.setPaidAmount(paidAmount);
        response.setRemainingAmount(remainingAmount);

        if (remainingAmount > 0) {
            response.setPaymentStatus(PaymentStatus.FAILED.name());
            response.setMessage("Insufficient balance. Please top up your cards.");
        } else {
            response.setPaymentStatus(PaymentStatus.SUCCESS.name());
            response.setMessage("Payment processed successfully.");
        }

        log.info("Payment result: {}", response);
        return response;
    }



    protected String generateUniqueCode() {
        // we can improve it later
        String code = UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        log.debug("Generated code: {}", code);
        return code;
    }

    private String generateQrBase64(String content) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Hashtable<EncodeHintType, Object> hints = new Hashtable<>();
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, 200, 200, hints);

            BufferedImage image = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < 200; x++) {
                for (int y = 0; y < 200; y++) {
                    int grayValue = (bitMatrix.get(x, y) ? 0 : 255);
                    image.setRGB(x, y, (grayValue << 16) | (grayValue << 8) | grayValue);
                }
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);

            return Base64.getEncoder().encodeToString(outputStream.toByteArray());

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR Code", e);
        }
    }


    private List<CardResponseDTO> convertToDTOList(List<Card> cards) {
        return cards.stream().map(card -> {
            CardResponseDTO dto = new CardResponseDTO();
            dto.setId(card.getId());
            dto.setCode(card.getCode());
            dto.setValue(card.getValue());
            dto.setStatus(card.getStatus());
            dto.setUsed(card.isUsed());
            dto.setCreatedAt(card.getCreatedAt());
            dto.setQrImageBase64(card.getQrImageBase64());
            return dto;
        }).collect(Collectors.toList());
    }
}

