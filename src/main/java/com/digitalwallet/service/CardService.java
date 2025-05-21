package com.digitalwallet.service;

import com.digitalwallet.dto.AssignCardsToAgentRequestDTO;
import com.digitalwallet.dto.CardRequestDTO;
import com.digitalwallet.dto.CardResponseDTO;
import com.digitalwallet.entity.*;
import com.digitalwallet.mapper.CardMapper;
import com.digitalwallet.repository.CardRepository;
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
    private final UserRepository userRepository;
    private final CardMapper cardMapper;

    public CardService(CardRepository cardRepository, UserRepository userRepository,CardMapper cardMapper) {
        this.cardRepository = cardRepository;
        this.userRepository = userRepository;
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

    public List<CardResponseDTO> getAllCards() {
        List<Card> cards = cardRepository.findAll();
        return cards.stream()
                .map(cardMapper::toResponseDTO)
                .collect(Collectors.toList());
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

