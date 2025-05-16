package com.digitalwallet.service;

import com.digitalwallet.dto.FileRequestDTO;
import com.digitalwallet.dto.FileResponseDTO;
import com.digitalwallet.entity.Card;
import com.digitalwallet.entity.File;
import com.digitalwallet.entity.FileStatus;
import com.digitalwallet.entity.User;
import com.digitalwallet.repository.CardRepository;
import com.digitalwallet.repository.FileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    private final FileRepository fileRepository;
    private final CardRepository cardRepository;

    public FileService(FileRepository fileRepository, CardRepository cardRepository) {
        this.fileRepository = fileRepository;
        this.cardRepository = cardRepository;
    }

    public FileResponseDTO createFile(FileRequestDTO request, Long userId) {
        log.info("Start creating file with quantity={} and value={}", request.getQuantity(), request.getValue());

        List<Card> availableCards = cardRepository.findByBatchIsNullAndValue(request.getValue());

        if (availableCards.size() < request.getQuantity()) {
            throw new IllegalArgumentException("Not enough cards available for the requested value");
        }
        File file = new File();
        file.setType(request.getType());
        file.setStatus(FileStatus.PENDING);

        User user = new User();
        user.setId(userId);
        file.setCreatedBy(user);

        file.setCreatedAt(LocalDateTime.now());

        File savedFile = fileRepository.save(file);

        List<Card> selectedCards = availableCards.stream()
                .limit(request.getQuantity())
                .peek(card -> card.setBatch(savedFile))
                .collect(Collectors.toList());

        cardRepository.saveAll(selectedCards);
        log.info("Associated {} cards with File ID {}", selectedCards.size(), savedFile.getId());

        return convertToDTOList(savedFile);
    }

    private FileResponseDTO convertToDTOList(File file) {
        FileResponseDTO dto = new FileResponseDTO();
        dto.setId(file.getId());
        dto.setType(file.getType());
        dto.setStatus(file.getStatus().toString());
        dto.setCreatedBy(file.getCreatedBy().getId());
        dto.setCreatedAt(file.getCreatedAt());
        return dto;
    }
}
