package com.mtg.mtgwalletbe.mapper;

import com.mtg.mtgwalletbe.api.response.AccountSaveResponse;
import com.mtg.mtgwalletbe.entity.Account;
import com.mtg.mtgwalletbe.service.dto.AccountDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccountServiceMapper {
    Account toAccountEntity(AccountDto accountDto);

    AccountDto toAccountDto(Account accountEntity);

    @Mapping(source = "user.username", target = "username")
    AccountSaveResponse toAccountSaveResponse(AccountDto accountDto);
}
