package com.mtg.mtgwalletbe.mapper;

import com.mtg.mtgwalletbe.api.response.PayeeCreateResponse;
import com.mtg.mtgwalletbe.entity.Payee;
import com.mtg.mtgwalletbe.service.dto.PayeeDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PayeeServiceMapper {
    Payee toPayeeEntity(PayeeDto payeeDto);

    PayeeDto toPayeeDto(Payee payeeEntity);

    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "id", target = "payeeId")
    PayeeCreateResponse toPayeeCreateResponse(PayeeDto payeeDto);
}
