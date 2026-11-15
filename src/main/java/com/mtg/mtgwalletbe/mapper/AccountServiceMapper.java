package com.mtg.mtgwalletbe.mapper;

import com.mtg.mtgwalletbe.api.response.AccountResponse;
import com.mtg.mtgwalletbe.entity.Account;
import com.mtg.mtgwalletbe.service.dto.AccountDto;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MtgMapperConfig.class)
public interface AccountServiceMapper {
    // Setters, not Account's Lombok builder: the builder has no inherited audit fields, so their ignores can't apply
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(source = "userId", target = "user.id")
    Account toAccountEntity(AccountDto accountDto);

    @Mapping(source = "user.id", target = "userId")
    AccountDto toAccountDto(Account accountEntity);

    AccountResponse toAccountResponse(AccountDto accountDto);

    AccountResponse toAccountResponse(Account account);
}
