package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.request.CategorySearchRequest;
import com.mtg.mtgwalletbe.api.request.PayeeCreateRequest;
import com.mtg.mtgwalletbe.api.request.PayeeSearchRequest;
import com.mtg.mtgwalletbe.api.request.PayeeUpdateRequest;
import com.mtg.mtgwalletbe.api.response.CategoryResponse;
import com.mtg.mtgwalletbe.api.response.PayeeCreateScreenEnumResponse;
import com.mtg.mtgwalletbe.api.response.PayeeResponse;
import com.mtg.mtgwalletbe.entity.Category;
import com.mtg.mtgwalletbe.entity.Payee;
import com.mtg.mtgwalletbe.enums.Status;
import com.mtg.mtgwalletbe.enums.TransactionType;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import com.mtg.mtgwalletbe.mapper.CategoryServiceMapper;
import com.mtg.mtgwalletbe.mapper.PayeeServiceMapper;
import com.mtg.mtgwalletbe.mapper.UserServiceMapper;
import com.mtg.mtgwalletbe.repository.PayeeRepository;
import com.mtg.mtgwalletbe.service.dto.CategoryDto;
import com.mtg.mtgwalletbe.service.dto.PayeeDto;
import com.mtg.mtgwalletbe.service.dto.WalletUserBasicDto;
import com.mtg.mtgwalletbe.service.dto.WalletUserDto;
import com.mtg.mtgwalletbe.specification.PayeeSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.mtg.mtgwalletbe.service.CategoryServiceImpl.MAX_ALLOWED_CATEGORY_COUNT;

@Service
@RequiredArgsConstructor
@Transactional
public class PayeeServiceImpl implements PayeeService {
    private final PayeeRepository repository;
    private final PayeeServiceMapper mapper;
    private final CategoryService categoryService;
    private final UserService userService;
    private final UserServiceMapper userServiceMapper;
    private final CategoryServiceMapper categoryServiceMapper;
    public static final int MAX_ALLOWED_PAYEE_COUNT = 100;

    @Override
    public PayeeDto create(PayeeCreateRequest payeeCreateRequest) throws MtgWalletGenericException {
        List<PayeeDto> userPayees = findAllByCurrentUserByStatus(Status.ACTIVE);
        if (userPayees.size() >= MAX_ALLOWED_PAYEE_COUNT) {
            throw new MtgWalletGenericException(GenericExceptionMessages.PAYEES_LIMIT_EXCEEDED.getMessage());
        }
        if (userPayees.stream().anyMatch(payee -> payee.getName().equals(payeeCreateRequest.getName()))) {
            throw new MtgWalletGenericException(GenericExceptionMessages.PAYEE_NAME_ALREADY_EXISTS.getMessage());
        }
        CategoryDto categoryDto = categoryService.getCategory(payeeCreateRequest.getCategoryId());
        if (categoryDto == null) {
            throw new MtgWalletGenericException(GenericExceptionMessages.CATEGORY_NOT_FOUND.getMessage());
        }
        WalletUserBasicDto walletUserDto = userService.getCurrentLoggedInUser();
        PayeeDto payeeDtoToSave = PayeeDto.builder().name(payeeCreateRequest.getName())
                .category(categoryDto).userId(walletUserDto.getId()).status(Status.ACTIVE).build();
        return mapper.toPayeeDto(repository.save(mapper.toPayeeEntity(payeeDtoToSave)));
    }

    @Override
    public Page<PayeeResponse> search(PayeeSearchRequest request, Status status, Pageable pageable) {
        WalletUserBasicDto walletUserDto = userService.getCurrentLoggedInUser();
        request.setUserId(walletUserDto.getId());
        Specification<Payee> specification = PayeeSpecification.search(request, status);
        Page<Payee> categories = repository.findAll(specification, pageable);
        return categories.map(mapper::toPayeeResponse);
    }

    @Override
    public PayeeDto update(PayeeUpdateRequest payeeUpdateRequest, Long id) throws MtgWalletGenericException {
        Payee payee = repository.findByIdAndStatus(id, Status.ACTIVE).orElseThrow(() -> new MtgWalletGenericException(GenericExceptionMessages.PAYEE_NOT_FOUND.getMessage()));
        userService.validateUsernameIfItsTheCurrentUser(payee.getUser().getUsername());
        List<PayeeDto> userPayees = findAllByCurrentUserByStatus(Status.ACTIVE);
        if (userPayees.stream().anyMatch(existingPayee -> !Objects.equals(existingPayee.getId(), id) && existingPayee.getName().equals(payeeUpdateRequest.getName()))) {
            throw new MtgWalletGenericException(GenericExceptionMessages.PAYEE_NAME_ALREADY_EXISTS.getMessage());
        }
        payee.setName(payeeUpdateRequest.getName());
        Category category = categoryServiceMapper.toCategoryEntity(categoryService.getCategory(payeeUpdateRequest.getCategoryId()));
        payee.setCategory(category);
        return mapper.toPayeeDto(repository.save(payee));
    }

    @Override
    public void delete(Long id) throws MtgWalletGenericException {
        Payee payee = repository.findById(id).orElseThrow(() -> new MtgWalletGenericException(GenericExceptionMessages.PAYEE_NOT_FOUND.getMessage()));
        userService.validateUsernameIfItsTheCurrentUser(payee.getUser().getUsername());
        payee.setName("Deleted Payee");
        payee.setStatus(Status.DELETED);
        repository.save(payee);
    }

    @Override
    public List<PayeeDto> findAllByCurrentUserByStatus(Status status) {
        WalletUserBasicDto walletUserDto = userService.getCurrentLoggedInUser();
        return mapper.toPayeeDtoList(repository.findAllByUserAndStatus(userServiceMapper.toWalletUserEntity(walletUserDto), status));
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
    public PayeeCreateScreenEnumResponse getPayeeCreateScreenEnums() {
        PayeeCreateScreenEnumResponse response = new PayeeCreateScreenEnumResponse();
        Page<CategoryResponse> categoriesPage = categoryService.search(CategorySearchRequest.builder().build(), Status.ACTIVE, PageRequest.of(0, MAX_ALLOWED_CATEGORY_COUNT, Sort.by("name").ascending()));
        response.setCategoryList(categoriesPage);
        return response;
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
        WalletUserDto walletUserDto = userService.getCurrentLoggedInUserFull();
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
        WalletUserDto walletUserDto = userService.getCurrentLoggedInUserFull();
        walletUserDto.setDefaultPayeeForIncome(mapper.toPayeeEntity(payeeDto));
        userService.updateUser(walletUserDto);
    }
}
