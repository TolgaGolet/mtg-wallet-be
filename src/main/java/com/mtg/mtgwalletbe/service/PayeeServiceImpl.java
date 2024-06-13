package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.request.PayeeCreateRequest;
import com.mtg.mtgwalletbe.entity.Payee;
import com.mtg.mtgwalletbe.enums.TransactionType;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import com.mtg.mtgwalletbe.mapper.PayeeServiceMapper;
import com.mtg.mtgwalletbe.mapper.UserServiceMapper;
import com.mtg.mtgwalletbe.repository.PayeeRepository;
import com.mtg.mtgwalletbe.service.dto.CategoryDto;
import com.mtg.mtgwalletbe.service.dto.PayeeDto;
import com.mtg.mtgwalletbe.service.dto.WalletUserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class PayeeServiceImpl implements PayeeService {
    private final PayeeRepository repository;
    private final PayeeServiceMapper mapper;
    private final CategoryService categoryService;
    private final UserService userService;
    private final UserServiceMapper userServiceMapper;

    @Override
    public PayeeDto create(PayeeCreateRequest payeeCreateRequest) throws MtgWalletGenericException {
        List<PayeeDto> userPayees = findAllByCurrentUser();
        if (userPayees.stream().anyMatch(payee -> payee.getName().equals(payeeCreateRequest.getName()))) {
            throw new MtgWalletGenericException(GenericExceptionMessages.PAYEE_NAME_ALREADY_EXISTS.getMessage());
        }
        CategoryDto categoryDto = categoryService.getCategory(payeeCreateRequest.getCategoryId());
        if (categoryDto == null) {
            throw new MtgWalletGenericException(GenericExceptionMessages.CATEGORY_NOT_FOUND.getMessage());
        }
        WalletUserDto walletUserDto = userService.getCurrentLoggedInUser();
        PayeeDto payeeDtoToSave = PayeeDto.builder().name(payeeCreateRequest.getName())
                .category(categoryDto).user(walletUserDto).build();
        return mapper.toPayeeDto(repository.save(mapper.toPayeeEntity(payeeDtoToSave)));
    }

    @Override
    public List<PayeeDto> findAllByCurrentUser() {
        WalletUserDto walletUserDto = userService.getCurrentLoggedInUser();
        return mapper.toPayeeDtoList(repository.findAllByUser(userServiceMapper.toWalletUserEntity(walletUserDto)));
    }

    @Override
    public PayeeDto getPayee(Long id) throws MtgWalletGenericException {
        Optional<Payee> payee = repository.findById(id);
        if (payee.isPresent()) {
            userService.validateUsernameIfItsTheCurrentUser(payee.get().getUser().getUsername());
        }
        return payee.map(mapper::toPayeeDto).orElse(null);
    }

    @Override
    public void addDefaultPayeeForExpenseToUser(Long payeeId) throws MtgWalletGenericException {
        PayeeDto payeeDto = getPayee(payeeId);
        if (payeeDto == null) {
            throw new MtgWalletGenericException(GenericExceptionMessages.PAYEE_NOT_FOUND.getMessage());
        }
        if (payeeDto.getCategory().getTransactionType() != TransactionType.EXPENSE) {
            throw new MtgWalletGenericException(GenericExceptionMessages.PAYEE_CATEGORY_TRANSACTION_TYPE_NOT_VALID.getMessage());
        }
        WalletUserDto walletUserDto = userService.getCurrentLoggedInUser();
        walletUserDto.setDefaultPayeeForExpense(mapper.toPayeeEntity(payeeDto));
        userService.updateUser(walletUserDto);
    }

    @Override
    public void addDefaultPayeeForIncomeToUser(Long payeeId) throws MtgWalletGenericException {
        PayeeDto payeeDto = getPayee(payeeId);
        if (payeeDto == null) {
            throw new MtgWalletGenericException(GenericExceptionMessages.PAYEE_NOT_FOUND.getMessage());
        }
        if (payeeDto.getCategory().getTransactionType() != TransactionType.INCOME) {
            throw new MtgWalletGenericException(GenericExceptionMessages.PAYEE_CATEGORY_TRANSACTION_TYPE_NOT_VALID.getMessage());
        }
        WalletUserDto walletUserDto = userService.getCurrentLoggedInUser();
        walletUserDto.setDefaultPayeeForIncome(mapper.toPayeeEntity(payeeDto));
        userService.updateUser(walletUserDto);
    }
}
