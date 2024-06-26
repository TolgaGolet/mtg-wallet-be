package com.mtg.mtgwalletbe.mapper;

import com.mtg.mtgwalletbe.api.response.AccountResponse;
import com.mtg.mtgwalletbe.entity.Account;
import com.mtg.mtgwalletbe.service.dto.AccountDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AccountServiceMapper {
    Account toAccountEntity(AccountDto accountDto);

    AccountDto toAccountDto(Account accountEntity);

    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "id", target = "accountId")
    AccountResponse toAccountResponse(AccountDto accountDto);

    List<AccountResponse> toAccountResponseList(List<AccountDto> accountDtoList);

    void updateAccountFromDto(AccountDto dto, @MappingTarget Account entity);

    List<AccountDto> toAccountDtoList(List<Account> accountList);
}
