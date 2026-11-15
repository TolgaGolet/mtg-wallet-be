package com.mtg.mtgwalletbe.mapper;

import com.mtg.mtgwalletbe.api.response.AccountResponse;
import com.mtg.mtgwalletbe.entity.Account;
import com.mtg.mtgwalletbe.service.dto.AccountDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountServiceMapper {
    @Mapping(source = "userId", target = "user.id")
    Account toAccountEntity(AccountDto accountDto);

    @Mapping(source = "user.id", target = "userId")
    AccountDto toAccountDto(Account accountEntity);

    AccountResponse toAccountResponse(AccountDto accountDto);

    AccountResponse toAccountResponse(Account account);
}
