package com.digitalwallet.service;

import com.digitalwallet.dto.FileRequestDTO;
import com.digitalwallet.dto.FileResponseDTO;
import com.digitalwallet.entity.Card;
import com.digitalwallet.entity.File;
import com.digitalwallet.entity.FileStatus;
import com.digitalwallet.entity.User;
import com.digitalwallet.repository.CardRepository;
import com.digitalwallet.repository.FileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class FileServiceTest {

    private FileRepository fileRepository;
    private CardRepository cardRepository;
    private FileService fileService;

    @BeforeEach
    void setUp() {
        fileRepository = mock(FileRepository.class);
        cardRepository = mock(CardRepository.class);
        fileService = new FileService(fileRepository, cardRepository);
    }

    @Test
    void createFile_ShouldCreateFileAndAssignCards_WhenCardsAreSufficient() {
        FileRequestDTO request = new FileRequestDTO();
        request.setQuantity(3);
        request.setValue(5000);
        request.setType("PDF");

        List<Card> availableCards = IntStream.range(0, 5)
                .mapToObj(i -> {
                    Card card = new Card();
                    card.setId((long) i + 1);
                    card.setValue(5000);
                    return card;
                })
                .collect(Collectors.toList());

        File savedFile = new File();
        User createdBy = new User();
        createdBy.setId(1L);
        savedFile.setId(1L);
        savedFile.setType("PDF");
        savedFile.setStatus(FileStatus.PENDING);
        savedFile.setCreatedAt(LocalDateTime.now());
        savedFile.setCreatedBy(createdBy);

        when(cardRepository.findByBatchIsNullAndValue(5000)).thenReturn(availableCards);
        when(fileRepository.save(any(File.class))).thenReturn(savedFile);
        when(cardRepository.saveAll(anyList())).thenReturn(availableCards.subList(0, 3));

        FileResponseDTO response = fileService.createFile(request, 1L);

        assertNotNull(response);
        assertEquals("PDF", response.getType());
        assertEquals("PENDING", response.getStatus());
        assertEquals(1L, response.getCreatedBy());
    }

    @Test
    void createFile_ShouldThrowException_WhenCardsAreInsufficient() {
        FileRequestDTO request = new FileRequestDTO();
        request.setQuantity(10);
        request.setValue(5000);
        request.setType("PDF");

        List<Card> availableCards = IntStream.range(0, 5)
                .mapToObj(i -> {
                    Card card = new Card();
                    card.setId((long) i + 1);
                    card.setValue(5000);
                    return card;
                })
                .collect(Collectors.toList());

        when(cardRepository.findByBatchIsNullAndValue(5000)).thenReturn(availableCards);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> fileService.createFile(request, 1L)
        );

        assertTrue(exception.getMessage().equals("Please generate 5 more cards with value 5000"));
    }

}
