package com.mtg.mtgwalletbe.mapper;

import com.mtg.mtgwalletbe.api.response.AccountDetailsResponse;
import com.mtg.mtgwalletbe.api.response.AccountResponse;
import com.mtg.mtgwalletbe.entity.Account;
import com.mtg.mtgwalletbe.service.dto.AccountDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AccountServiceMapper {
    @Mapping(source = "userId", target = "user.id")
    Account toAccountEntity(AccountDto accountDto);

    @Mapping(source = "user.id", target = "userId")
    AccountDto toAccountDto(Account accountEntity);

    AccountResponse toAccountResponse(AccountDto accountDto);

    AccountResponse toAccountResponse(Account account);

    List<AccountResponse> toAccountResponseList(List<AccountDto> accountDtoList);

    @Mapping(source = "userId", target = "user.id")
    void updateAccountFromDto(AccountDto dto, @MappingTarget Account entity);

    List<AccountDto> toAccountDtoList(List<Account> accountList);

    AccountDetailsResponse toAccountDetailsResponse(Account account);

    Account toAccountEntity(AccountDetailsResponse accountDetailsResponse);
}
