package com.mtg.mtgwalletbe.mapper;

import com.mtg.mtgwalletbe.api.response.AccountCreateResponse;
import com.mtg.mtgwalletbe.entity.Account;
import com.mtg.mtgwalletbe.service.dto.AccountDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AccountServiceMapper {
    Account toAccountEntity(AccountDto accountDto);

    AccountDto toAccountDto(Account accountEntity);

    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "id", target = "accountId")
    AccountCreateResponse toAccountCreateResponse(AccountDto accountDto);

    void updateAccountFromDto(AccountDto dto, @MappingTarget Account entity);
}
