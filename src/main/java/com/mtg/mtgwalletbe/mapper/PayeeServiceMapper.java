package com.mtg.mtgwalletbe.mapper;

import com.mtg.mtgwalletbe.api.response.PayeeCreateResponse;
import com.mtg.mtgwalletbe.entity.Payee;
import com.mtg.mtgwalletbe.service.dto.PayeeDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PayeeServiceMapper {
    @Mapping(source = "userId", target = "user.id")
    Payee toPayeeEntity(PayeeDto payeeDto);

    @Mapping(source = "user.id", target = "userId")
    PayeeDto toPayeeDto(Payee payeeEntity);

    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "id", target = "payeeId")
    PayeeCreateResponse toPayeeCreateResponse(PayeeDto payeeDto);

    List<PayeeDto> toPayeeDtoList(List<Payee> payeeList);
}
