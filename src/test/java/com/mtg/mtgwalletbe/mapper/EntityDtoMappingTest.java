package com.mtg.mtgwalletbe.mapper;

import com.mtg.mtgwalletbe.api.response.CategoryResponse;
import com.mtg.mtgwalletbe.api.response.CategorySelectResponse;
import com.mtg.mtgwalletbe.entity.Account;
import com.mtg.mtgwalletbe.entity.Category;
import com.mtg.mtgwalletbe.entity.WalletUser;
import com.mtg.mtgwalletbe.enums.AccountType;
import com.mtg.mtgwalletbe.enums.Currency;
import com.mtg.mtgwalletbe.enums.Status;
import com.mtg.mtgwalletbe.enums.TransactionType;
import com.mtg.mtgwalletbe.service.dto.AccountDto;
import com.mtg.mtgwalletbe.service.dto.CategoryDto;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

// The generated mappers (no Spring needed for these two): checks the hand-written mapping rules, i.e. user.id <-> userId,
// parent id -> parent reference, and the "without user" variant that must not touch the lazy user
class EntityDtoMappingTest {

    private final CategoryServiceMapper categoryMapper = new CategoryServiceMapperImpl();
    private final AccountServiceMapper accountMapper = new AccountServiceMapperImpl();

    @Test
    void categoryDtoToEntity_mapsUserIdAndParentIdToReferences() {
        CategoryDto dto = CategoryDto.builder().name("Groceries").transactionType(TransactionType.EXPENSE)
                .userId(3L).parentCategoryId(9L).isParent(false).status(Status.ACTIVE).build();

        Category entity = categoryMapper.toCategoryEntity(dto);

        assertThat(entity.getUser().getId()).isEqualTo(3L);
        assertThat(entity.getParentCategory().getId()).isEqualTo(9L);
        assertThat(entity.getName()).isEqualTo("Groceries");
        assertThat(entity.getTransactionType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(entity.getStatus()).isEqualTo(Status.ACTIVE);
    }

    @Test
    void categoryDtoToEntity_withoutParentId_hasNoParent() {
        Category entity = categoryMapper.toCategoryEntity(CategoryDto.builder().name("Food").userId(3L).build());

        assertThat(entity.getParentCategory()).isNull();
    }

    @Test
    void categoryEntityToDto_flattensUserAndParent() {
        Category parent = new Category();
        parent.setId(9L);
        parent.setName("Food");
        Category category = new Category();
        category.setId(10L);
        category.setName("Groceries");
        category.setParentCategory(parent);
        category.setUser(WalletUser.builder().id(3L).username("alice").build());

        CategoryDto dto = categoryMapper.toCategoryDto(category);

        assertThat(dto.getUserId()).isEqualTo(3L);
        assertThat(dto.getUsername()).isEqualTo("alice");
        assertThat(dto.getParentCategoryId()).isEqualTo(9L);
        assertThat(dto.getParentCategoryName()).isEqualTo("Food");
    }

    // A category nested in a payee/transaction: the lazy user must not be read (N+1 in transaction search)
    @Test
    void categoryEntityToDtoWithoutUser_neverReadsTheUser() {
        CategoryDto dto = categoryMapper.toCategoryDtoWithoutUser(categoryWithUntouchableUser(10L, "Groceries"));

        assertThat(dto.getName()).isEqualTo("Groceries");
        assertThat(dto.getUserId()).isNull();
        assertThat(dto.getParentCategoryId()).isNull();
    }

    @Test
    void categorySelectOption_usesIdAsValueAndNameAsLabel() {
        CategoryResponse response = new CategoryResponse();
        response.setId(10L);
        response.setName("Groceries");
        response.setParentCategoryId(9L);
        response.setTransactionType(TransactionType.EXPENSE);

        CategorySelectResponse option = categoryMapper.toCategorySelectResponse(response);

        assertThat(option.getValue()).isEqualTo("10");
        assertThat(option.getLabel()).isEqualTo("Groceries");
        assertThat(option.getParentCategoryId()).isEqualTo(9L);
        assertThat(option.getTransactionType()).isEqualTo(TransactionType.EXPENSE);
    }

    // Payee/transaction mappers get their nested mappers injected by Spring; wired by hand here
    private PayeeServiceMapper payeeMapper() {
        PayeeServiceMapperImpl mapper = new PayeeServiceMapperImpl();
        ReflectionTestUtils.setField(mapper, "categoryServiceMapper", categoryMapper);
        return mapper;
    }

    private TransactionServiceMapper transactionMapper() {
        TransactionServiceMapperImpl mapper = new TransactionServiceMapperImpl();
        ReflectionTestUtils.setField(mapper, "accountServiceMapper", accountMapper);
        ReflectionTestUtils.setField(mapper, "payeeServiceMapper", payeeMapper());
        return mapper;
    }

    /** A category whose lazy user must not be read (it'd be one query per row in a transaction search). */
    private static Category categoryWithUntouchableUser(long id, String name) {
        Category category = new Category() {
            @Override
            public WalletUser getUser() {
                throw new AssertionError("category.user must not be loaded");
            }
        };
        category.setId(id);
        category.setName(name);
        category.setTransactionType(TransactionType.EXPENSE);
        return category;
    }

    private static com.mtg.mtgwalletbe.entity.Payee payee(Category category) {
        com.mtg.mtgwalletbe.entity.Payee payee = new com.mtg.mtgwalletbe.entity.Payee();
        payee.setId(5L);
        payee.setName("Market");
        payee.setCategory(category);
        payee.setStatus(Status.ACTIVE);
        payee.setUser(WalletUser.builder().id(3L).build());
        return payee;
    }

    private static Account account(long id, String name) {
        return Account.builder().id(id).name(name).type(AccountType.CASH).currency(Currency.TRY)
                .balance(BigDecimal.TEN).status(Status.ACTIVE).build();
    }

    @Test
    void payeeEntityToDto_keepsCategoryName_withoutLoadingTheCategorysUser() {
        var dto = payeeMapper().toPayeeDto(payee(categoryWithUntouchableUser(7L, "Groceries")));

        assertThat(dto.getUserId()).isEqualTo(3L);
        assertThat(dto.getCategory().getName()).isEqualTo("Groceries");
        assertThat(dto.getCategory().getUserId()).isNull();
    }

    @Test
    void payeeResponse_flattensTheCategory_andToleratesAMissingOne() {
        var mapper = payeeMapper();

        var withCategory = mapper.toPayeeResponse(payee(categoryWithUntouchableUser(7L, "Groceries")));
        var withoutCategory = mapper.toPayeeResponse(payee(null));

        assertThat(withCategory.getCategoryId()).isEqualTo(7L);
        assertThat(withCategory.getCategoryName()).isEqualTo("Groceries");
        assertThat(withoutCategory.getCategoryId()).isNull();
        assertThat(withoutCategory.getCategoryName()).isNull();
    }

    @Test
    void transactionToListResponse_carriesNestedNames_andHandlesNoTargetAccount() {
        com.mtg.mtgwalletbe.entity.Transaction transaction = new com.mtg.mtgwalletbe.entity.Transaction();
        transaction.setId(100L);
        transaction.setType(TransactionType.EXPENSE);
        transaction.setAmount(new BigDecimal("12.50"));
        transaction.setNotes("note");
        transaction.setUser(WalletUser.builder().id(3L).build());
        transaction.setSourceAccount(account(10L, "Cash"));
        transaction.setPayee(payee(categoryWithUntouchableUser(7L, "Groceries")));
        TransactionServiceMapper mapper = transactionMapper();

        var dto = mapper.toTransactionDto(transaction);
        var list = mapper.toTransactionListResponse(dto);

        assertThat(dto.getUserId()).isEqualTo(3L);
        assertThat(list.getPayee().getName()).isEqualTo("Market");
        assertThat(list.getPayee().getCategory().getName()).isEqualTo("Groceries");
        assertThat(list.getSourceAccount().getName()).isEqualTo("Cash");
        assertThat(list.getTargetAccount()).isNull();
        assertThat(list.getAmount()).isEqualByComparingTo("12.50");
    }

    @Test
    void transactionResponse_flattensIdsOfPayeeAndBothAccounts() {
        com.mtg.mtgwalletbe.entity.Transaction transfer = new com.mtg.mtgwalletbe.entity.Transaction();
        transfer.setId(100L);
        transfer.setType(TransactionType.TRANSFER);
        transfer.setAmount(BigDecimal.ONE);
        transfer.setSourceAccount(account(10L, "Cash"));
        transfer.setTargetAccount(account(11L, "Bank"));
        transfer.setPayee(payee(categoryWithUntouchableUser(7L, "Moves")));
        TransactionServiceMapper mapper = transactionMapper();

        var response = mapper.toTransactionResponse(mapper.toTransactionDto(transfer));

        assertThat(response.getTransactionId()).isEqualTo(100L);
        assertThat(response.getTransactionType()).isEqualTo(TransactionType.TRANSFER);
        assertThat(response.getPayeeId()).isEqualTo(5L);
        assertThat(response.getSourceAccountId()).isEqualTo(10L);
        assertThat(response.getTargetAccountId()).isEqualTo(11L);
    }

    @Test
    void transactionWithoutPayeeOrAccounts_mapsToNulls_notAnNpe() {
        var mapper = transactionMapper();

        var response = mapper.toTransactionResponse(mapper.toTransactionDto(new com.mtg.mtgwalletbe.entity.Transaction()));

        assertThat(response.getPayeeId()).isNull();
        assertThat(response.getSourceAccountId()).isNull();
        assertThat(response.getTargetAccountId()).isNull();
    }

    @Test
    void accountRoundTrip_keepsFieldsAndMapsUserId() {
        AccountDto dto = AccountDto.builder().name("Cash").type(AccountType.CASH).currency(Currency.USD)
                .balance(new BigDecimal("12.34")).status(Status.ACTIVE).userId(3L).build();

        Account entity = accountMapper.toAccountEntity(dto);
        AccountDto back = accountMapper.toAccountDto(entity);

        assertThat(entity.getUser().getId()).isEqualTo(3L);
        assertThat(back.getUserId()).isEqualTo(3L);
        assertThat(back.getName()).isEqualTo("Cash");
        assertThat(back.getType()).isEqualTo(AccountType.CASH);
        assertThat(back.getCurrency()).isEqualTo(Currency.USD);
        assertThat(back.getBalance()).isEqualByComparingTo("12.34");
        assertThat(back.getStatus()).isEqualTo(Status.ACTIVE);
    }
}
