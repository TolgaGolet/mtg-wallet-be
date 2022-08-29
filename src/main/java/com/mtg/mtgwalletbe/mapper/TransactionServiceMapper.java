package com.mtg.mtgwalletbe.mapper;

import com.mtg.mtgwalletbe.api.response.TransactionCreateResponse;
import com.mtg.mtgwalletbe.entity.Transaction;
import com.mtg.mtgwalletbe.service.dto.TransactionDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionServiceMapper {
    Transaction toTransactionEntity(TransactionDto transactionDto);

    TransactionDto toTransactionDto(Transaction transactionEntity);

    @Mapping(source = "payee.id", target = "payeeId")
    @Mapping(source = "sourceAccount.id", target = "sourceAccountId")
    @Mapping(source = "targetAccount.id", target = "targetAccountId")
    @Mapping(source = "id", target = "transactionId")
    @Mapping(source = "type", target = "transactionType")
    @Mapping(source = "user.username", target = "username")
    TransactionCreateResponse toTransactionCreateResponse(TransactionDto transactionDto);
}
