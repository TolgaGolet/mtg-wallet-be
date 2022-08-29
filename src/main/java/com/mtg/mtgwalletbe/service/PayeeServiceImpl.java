package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.request.PayeeCreateRequest;
import com.mtg.mtgwalletbe.entity.Payee;
import com.mtg.mtgwalletbe.enums.TransactionType;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import com.mtg.mtgwalletbe.mapper.PayeeServiceMapper;
import com.mtg.mtgwalletbe.repository.PayeeRepository;
import com.mtg.mtgwalletbe.service.dto.CategoryDto;
import com.mtg.mtgwalletbe.service.dto.PayeeDto;
import com.mtg.mtgwalletbe.service.dto.WalletUserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class PayeeServiceImpl implements PayeeService {
    private final PayeeRepository repository;
    private final PayeeServiceMapper mapper;
    private final CategoryService categoryService;
    private final UserService userService;

    @Override
    public PayeeDto create(PayeeCreateRequest payeeCreateRequest) throws MtgWalletGenericException {
        CategoryDto categoryDto = categoryService.getCategory(payeeCreateRequest.getCategoryId());
        if (categoryDto == null) {
            throw new MtgWalletGenericException(GenericExceptionMessages.CATEGORY_NOT_FOUND.getMessage());
        }
        WalletUserDto walletUserDto = null;
        if (payeeCreateRequest.getUsername() != null) {
            walletUserDto = userService.getUser(payeeCreateRequest.getUsername());
        }
        PayeeDto payeeDtoToSave = PayeeDto.builder().name(payeeCreateRequest.getName())
                .category(categoryDto).user(walletUserDto).build();
        return mapper.toPayeeDto(repository.save(mapper.toPayeeEntity(payeeDtoToSave)));
    }

    @Override
    public PayeeDto getPayee(Long id) {
        Optional<Payee> payee = repository.findById(id);
        return payee.map(mapper::toPayeeDto).orElse(null);
    }

    @Override
    public void addDefaultPayeeForExpenseToUser(String username, Long payeeId) throws MtgWalletGenericException {
        PayeeDto payeeDto = getPayee(payeeId);
        if (payeeDto == null) {
            throw new MtgWalletGenericException(GenericExceptionMessages.PAYEE_NOT_FOUND.getMessage());
        }
        if (payeeDto.getCategory().getTransactionType() != TransactionType.EXPENSE) {
            throw new MtgWalletGenericException(GenericExceptionMessages.PAYEE_CATEGORY_TRANSACTION_TYPE_NOT_VALID.getMessage());
        }
        WalletUserDto walletUserDto = userService.getUser(username);
        walletUserDto.setDefaultPayeeForExpense(mapper.toPayeeEntity(payeeDto));
        userService.updateUser(walletUserDto);
    }

    @Override
    public void addDefaultPayeeForIncomeToUser(String username, Long payeeId) throws MtgWalletGenericException {
        PayeeDto payeeDto = getPayee(payeeId);
        if (payeeDto == null) {
            throw new MtgWalletGenericException(GenericExceptionMessages.PAYEE_NOT_FOUND.getMessage());
        }
        if (payeeDto.getCategory().getTransactionType() != TransactionType.INCOME) {
            throw new MtgWalletGenericException(GenericExceptionMessages.PAYEE_CATEGORY_TRANSACTION_TYPE_NOT_VALID.getMessage());
        }
        WalletUserDto walletUserDto = userService.getUser(username);
        walletUserDto.setDefaultPayeeForIncome(mapper.toPayeeEntity(payeeDto));
        userService.updateUser(walletUserDto);
    }
}
