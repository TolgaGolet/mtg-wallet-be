package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.request.*;
import com.mtg.mtgwalletbe.api.response.CategoryResponse;
import com.mtg.mtgwalletbe.api.response.PayeeCreateScreenEnumResponse;
import com.mtg.mtgwalletbe.api.response.PayeeResponse;
import com.mtg.mtgwalletbe.entity.Payee;
import com.mtg.mtgwalletbe.enums.Status;
import com.mtg.mtgwalletbe.enums.TransactionType;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import com.mtg.mtgwalletbe.mapper.PayeeServiceMapper;
import com.mtg.mtgwalletbe.repository.PayeeRepository;
import com.mtg.mtgwalletbe.service.dto.CategoryDto;
import com.mtg.mtgwalletbe.service.dto.PayeeDto;
import com.mtg.mtgwalletbe.service.dto.WalletUserBasicDto;
import com.mtg.mtgwalletbe.specification.PayeeSpecification;
import com.mtg.mtgwalletbe.util.Names;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.mtg.mtgwalletbe.service.DomainLimits.MAX_CATEGORIES;
import static com.mtg.mtgwalletbe.service.DomainLimits.MAX_PAYEES;

@Service
@RequiredArgsConstructor
@Transactional
public class PayeeServiceImpl implements PayeeService {
    private final PayeeRepository repository;
    private final PayeeServiceMapper mapper;
    private final CategoryService categoryService;
    private final UserService userService;
    private final DefaultDataService defaultDataService;

    @Override
    public PayeeDto create(PayeeCreateRequest payeeCreateRequest) throws MtgWalletGenericException {
        WalletUserBasicDto walletUserDto = userService.getCurrentLoggedInUser();
        if (repository.countByUserIdAndStatus(walletUserDto.getId(), Status.ACTIVE) >= MAX_PAYEES) {
            throw new MtgWalletGenericException(GenericExceptionMessages.PAYEES_LIMIT_EXCEEDED);
        }
        if (nameTaken(walletUserDto.getId(), payeeCreateRequest.getName(), -1L)) {
            throw new MtgWalletGenericException(GenericExceptionMessages.PAYEE_NAME_ALREADY_EXISTS);
        }
        if (payeeCreateRequest.getCategoryId() == null) { // e.g. a new payee from a transaction without a category
            throw new MtgWalletGenericException(GenericExceptionMessages.CATEGORY_NOT_FOUND);
        }
        CategoryDto categoryDto = categoryService.getActiveCategory(payeeCreateRequest.getCategoryId());
        PayeeDto payeeDtoToSave = PayeeDto.builder().name(payeeCreateRequest.getName())
                .category(categoryDto).userId(walletUserDto.getId()).status(Status.ACTIVE).build();
        return mapper.toPayeeDto(repository.save(mapper.toPayeeEntity(payeeDtoToSave)));
    }

    @Override
    public void createDefaults() {
        defaultDataService.createDefaultsForCurrentUser();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PayeeResponse> search(PayeeSearchRequest request, Status status, Pageable pageable) {
        WalletUserBasicDto walletUserDto = userService.getCurrentLoggedInUser();
        request.setUserId(walletUserDto.getId());
        Specification<Payee> specification = PayeeSpecification.search(request, status);
        Page<Payee> payees = repository.findAll(specification, pageable);
        return payees.map(mapper::toPayeeResponse);
    }

    @Override
    public PayeeDto update(PayeeUpdateRequest payeeUpdateRequest, Long id) throws MtgWalletGenericException {
        Payee payee = owned(repository.findByIdAndStatus(id, Status.ACTIVE));
        if (nameTaken(payee.getUser().getId(), payeeUpdateRequest.getName(), id)) {
            throw new MtgWalletGenericException(GenericExceptionMessages.PAYEE_NAME_ALREADY_EXISTS);
        }
        payee.setName(payeeUpdateRequest.getName());
        return mapper.toPayeeDto(repository.save(payee));
    }

    @Override
    public void delete(Long id) throws MtgWalletGenericException {
        Payee payee = owned(repository.findById(id));
        payee.setName("Deleted Payee");
        payee.setStatus(Status.DELETED);
        repository.save(payee);
    }

    @Override
    @Transactional(readOnly = true)
    public PayeeDto getPayee(Long id) throws MtgWalletGenericException {
        return mapper.toPayeeDto(owned(repository.findById(id)));
    }

    @Override
    @Transactional(readOnly = true)
    public PayeeDto getActivePayee(Long id) throws MtgWalletGenericException {
        return mapper.toPayeeDto(owned(repository.findByIdAndStatus(id, Status.ACTIVE)));
    }

    @Override
    @Transactional(readOnly = true)
    public PayeeCreateScreenEnumResponse getPayeeCreateScreenEnums() {
        PayeeCreateScreenEnumResponse response = new PayeeCreateScreenEnumResponse();
        Page<CategoryResponse> categoriesPage = categoryService.search(CategorySearchRequest.builder().build(), Status.ACTIVE, PageRequest.of(0, MAX_CATEGORIES, Sort.by("name").ascending()));
        response.setCategoryList(categoriesPage.getContent());
        return response;
    }

    @Override
    public void addDefaultPayeeForExpenseToUser(Long payeeId) throws MtgWalletGenericException {
        PayeeDto payeeDto = getActivePayee(payeeId);
        if (payeeDto.getCategory().getTransactionType() != TransactionType.EXPENSE) {
            throw new MtgWalletGenericException(GenericExceptionMessages.PAYEE_CATEGORY_TRANSACTION_TYPE_NOT_VALID);
        }
        userService.setDefaultPayeeForExpense(mapper.toPayeeEntity(payeeDto));
    }

    @Override
    public void addDefaultPayeeForIncomeToUser(Long payeeId) throws MtgWalletGenericException {
        PayeeDto payeeDto = getActivePayee(payeeId);
        if (payeeDto.getCategory().getTransactionType() != TransactionType.INCOME) {
            throw new MtgWalletGenericException(GenericExceptionMessages.PAYEE_CATEGORY_TRANSACTION_TYPE_NOT_VALID);
        }
        userService.setDefaultPayeeForIncome(mapper.toPayeeEntity(payeeDto));
    }

    /**
     * Case-insensitive (Turkish rules) name check against the user's other ACTIVE payees; loads names only.
     */
    private boolean nameTaken(Long userId, String name, Long excludeId) {
        return Names.containsName(repository.findNamesByUserIdAndStatus(userId, Status.ACTIVE, excludeId), name);
    }

    /**
     * Unwraps a lookup: PAYEE_NOT_FOUND when empty, and the payee must belong to the current user.
     */
    private Payee owned(Optional<Payee> lookup) throws MtgWalletGenericException {
        Payee payee = lookup.orElseThrow(() -> new MtgWalletGenericException(GenericExceptionMessages.PAYEE_NOT_FOUND));
        userService.validateUserIdIfItsTheCurrentUser(payee.getUser().getId());
        return payee;
    }
}
