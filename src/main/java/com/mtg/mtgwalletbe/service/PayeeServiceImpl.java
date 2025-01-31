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
    public void createDefaults() throws MtgWalletGenericException {
        if (Objects.equals(userService.getCurrentLoggedInUser().getIsDefaultsCreated(), Boolean.TRUE)) {
            return;
        }
        // Expense
        CategoryDto autoCategoryDto = categoryService.create(CategoryCreateRequest.builder().name("Auto").transactionTypeValue(TransactionType.EXPENSE.getValue()).build());
        CategoryDto gasCategoryDto = categoryService.create(CategoryCreateRequest.builder().name("Gas").transactionTypeValue(TransactionType.EXPENSE.getValue()).parentCategoryId(autoCategoryDto.getId()).build());
        create(PayeeCreateRequest.builder().name("Shell").categoryId(gasCategoryDto.getId()).build());
        categoryService.create(CategoryCreateRequest.builder().name("Service").transactionTypeValue(TransactionType.EXPENSE.getValue()).parentCategoryId(autoCategoryDto.getId()).build());
        CategoryDto billCategoryDto = categoryService.create(CategoryCreateRequest.builder().name("Bill").transactionTypeValue(TransactionType.EXPENSE.getValue()).build());
        categoryService.create(CategoryCreateRequest.builder().name("Electricity").transactionTypeValue(TransactionType.EXPENSE.getValue()).parentCategoryId(billCategoryDto.getId()).build());
        categoryService.create(CategoryCreateRequest.builder().name("Internet").transactionTypeValue(TransactionType.EXPENSE.getValue()).parentCategoryId(billCategoryDto.getId()).build());
        CategoryDto phoneCategoryDto = categoryService.create(CategoryCreateRequest.builder().name("Phone").transactionTypeValue(TransactionType.EXPENSE.getValue()).parentCategoryId(billCategoryDto.getId()).build());
        create(PayeeCreateRequest.builder().name("Türk Telekom").categoryId(phoneCategoryDto.getId()).build());
        categoryService.create(CategoryCreateRequest.builder().name("Water").transactionTypeValue(TransactionType.EXPENSE.getValue()).parentCategoryId(billCategoryDto.getId()).build());
        categoryService.create(CategoryCreateRequest.builder().name("Charity").transactionTypeValue(TransactionType.EXPENSE.getValue()).build());
        CategoryDto clothingCategoryDto = categoryService.create(CategoryCreateRequest.builder().name("Clothing").transactionTypeValue(TransactionType.EXPENSE.getValue()).build());
        create(PayeeCreateRequest.builder().name("Defacto").categoryId(clothingCategoryDto.getId()).build());
        categoryService.create(CategoryCreateRequest.builder().name("Education").transactionTypeValue(TransactionType.EXPENSE.getValue()).build());
        CategoryDto eatingOutCategoryDto = categoryService.create(CategoryCreateRequest.builder().name("Eating Out").transactionTypeValue(TransactionType.EXPENSE.getValue()).build());
        CategoryDto fastFoodCategoryDto = categoryService.create(CategoryCreateRequest.builder().name("Fast Food").transactionTypeValue(TransactionType.EXPENSE.getValue()).parentCategoryId(eatingOutCategoryDto.getId()).build());
        create(PayeeCreateRequest.builder().name("Burger King").categoryId(fastFoodCategoryDto.getId()).build());
        categoryService.create(CategoryCreateRequest.builder().name("Electronics").transactionTypeValue(TransactionType.EXPENSE.getValue()).build());
        CategoryDto entertainmentCategoryDto = categoryService.create(CategoryCreateRequest.builder().name("Entertainment").transactionTypeValue(TransactionType.EXPENSE.getValue()).build());
        categoryService.create(CategoryCreateRequest.builder().name("Concert").transactionTypeValue(TransactionType.EXPENSE.getValue()).parentCategoryId(entertainmentCategoryDto.getId()).build());
        CategoryDto digitalServicesCategoryDto = categoryService.create(CategoryCreateRequest.builder().name("Digital Services").transactionTypeValue(TransactionType.EXPENSE.getValue()).parentCategoryId(entertainmentCategoryDto.getId()).build());
        create(PayeeCreateRequest.builder().name("Spotify").categoryId(digitalServicesCategoryDto.getId()).build());
        create(PayeeCreateRequest.builder().name("Amazon Prime").categoryId(digitalServicesCategoryDto.getId()).build());
        categoryService.create(CategoryCreateRequest.builder().name("Gifts").transactionTypeValue(TransactionType.EXPENSE.getValue()).build());
        CategoryDto groceriesCategoryDto = categoryService.create(CategoryCreateRequest.builder().name("Groceries").transactionTypeValue(TransactionType.EXPENSE.getValue()).build());
        create(PayeeCreateRequest.builder().name("Migros").categoryId(groceriesCategoryDto.getId()).build());
        categoryService.create(CategoryCreateRequest.builder().name("Household").transactionTypeValue(TransactionType.EXPENSE.getValue()).build());
        CategoryDto medicalCategoryDto = categoryService.create(CategoryCreateRequest.builder().name("Medical").transactionTypeValue(TransactionType.EXPENSE.getValue()).build());
        categoryService.create(CategoryCreateRequest.builder().name("Hospital").transactionTypeValue(TransactionType.EXPENSE.getValue()).parentCategoryId(medicalCategoryDto.getId()).build());
        categoryService.create(CategoryCreateRequest.builder().name("Pharmacy").transactionTypeValue(TransactionType.EXPENSE.getValue()).parentCategoryId(medicalCategoryDto.getId()).build());
        categoryService.create(CategoryCreateRequest.builder().name("Others").transactionTypeValue(TransactionType.EXPENSE.getValue()).build());
        CategoryDto personalCareCategoryDto = categoryService.create(CategoryCreateRequest.builder().name("Personal Care").transactionTypeValue(TransactionType.EXPENSE.getValue()).build());
        categoryService.create(CategoryCreateRequest.builder().name("Hairdresser").transactionTypeValue(TransactionType.EXPENSE.getValue()).parentCategoryId(personalCareCategoryDto.getId()).build());
        categoryService.create(CategoryCreateRequest.builder().name("Rent").transactionTypeValue(TransactionType.EXPENSE.getValue()).build());
        categoryService.create(CategoryCreateRequest.builder().name("Tax").transactionTypeValue(TransactionType.EXPENSE.getValue()).build());
        CategoryDto transportCategoryDto = categoryService.create(CategoryCreateRequest.builder().name("Transport").transactionTypeValue(TransactionType.EXPENSE.getValue()).build());
        categoryService.create(CategoryCreateRequest.builder().name("Bus").transactionTypeValue(TransactionType.EXPENSE.getValue()).parentCategoryId(transportCategoryDto.getId()).build());
        CategoryDto transportationCardCategoryDto = categoryService.create(CategoryCreateRequest.builder().name("Transportation Card").transactionTypeValue(TransactionType.EXPENSE.getValue()).parentCategoryId(transportCategoryDto.getId()).build());
        create(PayeeCreateRequest.builder().name("Istanbul Card").categoryId(transportationCardCategoryDto.getId()).build());
        categoryService.create(CategoryCreateRequest.builder().name("Travel").transactionTypeValue(TransactionType.EXPENSE.getValue()).build());
        // Income
        categoryService.create(CategoryCreateRequest.builder().name("Bonus").transactionTypeValue(TransactionType.INCOME.getValue()).build());
        categoryService.create(CategoryCreateRequest.builder().name("Interest").transactionTypeValue(TransactionType.INCOME.getValue()).build());
        CategoryDto investmentCategoryDto = categoryService.create(CategoryCreateRequest.builder().name("Investment").transactionTypeValue(TransactionType.INCOME.getValue()).build());
        CategoryDto cryptocurrencyCategoryDto = categoryService.create(CategoryCreateRequest.builder().name("Cryptocurrency").transactionTypeValue(TransactionType.INCOME.getValue()).parentCategoryId(investmentCategoryDto.getId()).build());
        create(PayeeCreateRequest.builder().name("Binance").categoryId(cryptocurrencyCategoryDto.getId()).build());
        categoryService.create(CategoryCreateRequest.builder().name("Gold").transactionTypeValue(TransactionType.INCOME.getValue()).parentCategoryId(investmentCategoryDto.getId()).build());
        CategoryDto stocksCategoryDto = categoryService.create(CategoryCreateRequest.builder().name("Stocks").transactionTypeValue(TransactionType.INCOME.getValue()).parentCategoryId(investmentCategoryDto.getId()).build());
        create(PayeeCreateRequest.builder().name("Midas").categoryId(stocksCategoryDto.getId()).build());
        categoryService.create(CategoryCreateRequest.builder().name("Others").transactionTypeValue(TransactionType.INCOME.getValue()).build());
        categoryService.create(CategoryCreateRequest.builder().name("Pocket Money").transactionTypeValue(TransactionType.INCOME.getValue()).build());
        categoryService.create(CategoryCreateRequest.builder().name("Salary").transactionTypeValue(TransactionType.INCOME.getValue()).build());
        // Transfer
        categoryService.create(CategoryCreateRequest.builder().name("Credit Card Debt").transactionTypeValue(TransactionType.TRANSFER.getValue()).build());
        categoryService.create(CategoryCreateRequest.builder().name("Investment").transactionTypeValue(TransactionType.TRANSFER.getValue()).build());
        WalletUserDto walletUserDto = userService.getCurrentLoggedInUserFull();
        walletUserDto.setIsDefaultsCreated(Boolean.TRUE);
        userService.updateUser(walletUserDto);
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
        userService.validateUserIdIfItsTheCurrentUser(payee.getUser().getId());
        List<PayeeDto> userPayees = findAllByCurrentUserByStatus(Status.ACTIVE);
        if (userPayees.stream().anyMatch(existingPayee -> !Objects.equals(existingPayee.getId(), id) && existingPayee.getName().equals(payeeUpdateRequest.getName()))) {
            throw new MtgWalletGenericException(GenericExceptionMessages.PAYEE_NAME_ALREADY_EXISTS.getMessage());
        }
        payee.setName(payeeUpdateRequest.getName());
        return mapper.toPayeeDto(repository.save(payee));
    }

    @Override
    public void delete(Long id) throws MtgWalletGenericException {
        Payee payee = repository.findById(id).orElseThrow(() -> new MtgWalletGenericException(GenericExceptionMessages.PAYEE_NOT_FOUND.getMessage()));
        userService.validateUserIdIfItsTheCurrentUser(payee.getUser().getId());
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
            userService.validateUserIdIfItsTheCurrentUser(payee.get().getUser().getId());
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
