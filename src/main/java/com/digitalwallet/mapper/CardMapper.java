package com.digitalwallet.mapper;

import com.digitalwallet.dto.CardResponseDTO;
import com.digitalwallet.entity.Card;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CardMapper {
    @Mapping(target = "status", expression = "java(card.getStatus())")
    CardResponseDTO toResponseDTO(Card card);
}
