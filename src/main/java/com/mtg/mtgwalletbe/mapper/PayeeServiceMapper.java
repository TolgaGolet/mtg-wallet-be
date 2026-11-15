package com.mtg.mtgwalletbe.mapper;

import com.mtg.mtgwalletbe.api.response.PayeeResponse;
import com.mtg.mtgwalletbe.entity.Payee;
import com.mtg.mtgwalletbe.service.dto.PayeeDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MtgMapperConfig.class, uses = CategoryServiceMapper.class)
public interface PayeeServiceMapper {
    // Every query whose payees are mapped fetches category.parentCategory, so the nested category costs no query
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "category", target = "category", qualifiedByName = CategoryServiceMapper.WITHOUT_USER)
    PayeeDto toPayeeDto(Payee payeeEntity);

    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category.name", target = "categoryName")
    PayeeResponse toPayeeResponse(PayeeDto payeeDto);

    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category.name", target = "categoryName")
    PayeeResponse toPayeeResponse(Payee payee);
}
