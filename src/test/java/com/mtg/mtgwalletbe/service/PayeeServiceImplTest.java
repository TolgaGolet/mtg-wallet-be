package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.request.PayeeCreateRequest;
import com.mtg.mtgwalletbe.entity.Category;
import com.mtg.mtgwalletbe.entity.Payee;
import com.mtg.mtgwalletbe.entity.WalletUser;
import com.mtg.mtgwalletbe.enums.Status;
import com.mtg.mtgwalletbe.enums.TransactionType;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import com.mtg.mtgwalletbe.mapper.PayeeServiceMapper;
import com.mtg.mtgwalletbe.repository.PayeeRepository;
import com.mtg.mtgwalletbe.service.dto.CategoryDto;
import com.mtg.mtgwalletbe.service.dto.WalletUserBasicDto;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

// Regression: payees accepted DELETED categories, and DELETED payees could become the user's default
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PayeeServiceImplTest {

    @Mock private PayeeRepository repository;
    @Mock private PayeeServiceMapper mapper;
    @Mock private CategoryService categoryService;
    @Mock private UserService userService;
    @Mock private DefaultDataService defaultDataService;
    @Mock private EntityManager entityManager;
    @InjectMocks private PayeeServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        WalletUserBasicDto user = new WalletUserBasicDto();
        user.setId(1L);
        when(userService.getCurrentLoggedInUser()).thenReturn(user);
        when(repository.findNamesByUserIdAndStatus(anyLong(), any(), anyLong())).thenReturn(List.of());
        when(categoryService.getActiveCategory(anyLong())).thenReturn(CategoryDto.builder().id(7L).transactionType(TransactionType.EXPENSE).build());
    }

    @Test
    void create_requiresAnActiveCategory() throws Exception {
        service.create(PayeeCreateRequest.builder().name("Shop").categoryId(7L).build());

        verify(categoryService).getActiveCategory(7L);
        verify(categoryService, never()).getCategory(anyLong());
    }

    @Test
    void create_withoutCategory_isRejected() {
        assertThatThrownBy(() -> service.create(PayeeCreateRequest.builder().name("Shop").build()))
                .extracting(PayeeServiceImplTest::errorOf)
                .isEqualTo(GenericExceptionMessages.CATEGORY_NOT_FOUND);
        verify(repository, never()).save(any());
    }

    @Test
    void defaultPayee_mustBeActive() throws Exception {
        Payee payee = new Payee();
        payee.setId(5L);
        payee.setUser(WalletUser.builder().id(1L).build());
        when(repository.findByIdAndStatus(5L, Status.ACTIVE)).thenReturn(Optional.empty());
        when(repository.findById(5L)).thenReturn(Optional.of(payee));

        assertThatThrownBy(() -> service.addDefaultPayeeForExpenseToUser(5L))
                .extracting(PayeeServiceImplTest::errorOf)
                .isEqualTo(GenericExceptionMessages.PAYEE_NOT_FOUND);
        verify(userService, never()).setDefaultPayeeForExpense(any());
    }

    @Test
    void defaultPayee_activeExpensePayee_isSet() throws Exception {
        Payee payee = new Payee();
        payee.setId(5L);
        payee.setUser(WalletUser.builder().id(1L).build());
        payee.setCategory(category(TransactionType.EXPENSE));
        when(repository.findByIdAndStatus(5L, Status.ACTIVE)).thenReturn(Optional.of(payee));

        service.addDefaultPayeeForExpenseToUser(5L);

        verify(userService).setDefaultPayeeForExpense(payee); // the managed row, not a copy rebuilt from a DTO
    }

    @Test
    void defaultPayee_ofTheOtherType_isRejected() {
        Payee payee = new Payee();
        payee.setId(5L);
        payee.setUser(WalletUser.builder().id(1L).build());
        payee.setCategory(category(TransactionType.INCOME));
        when(repository.findByIdAndStatus(5L, Status.ACTIVE)).thenReturn(Optional.of(payee));

        assertThatThrownBy(() -> service.addDefaultPayeeForExpenseToUser(5L))
                .extracting(PayeeServiceImplTest::errorOf)
                .isEqualTo(GenericExceptionMessages.PAYEE_CATEGORY_TRANSACTION_TYPE_NOT_VALID);
        verify(userService, never()).setDefaultPayeeForExpense(any());
    }

    private Payee storedPayee(long id, long ownerId, TransactionType type) {
        Payee payee = new Payee();
        payee.setId(id);
        payee.setName("Shop");
        payee.setUser(WalletUser.builder().id(ownerId).build());
        payee.setCategory(category(type));
        payee.setStatus(Status.ACTIVE);
        when(repository.findByIdAndStatus(id, Status.ACTIVE)).thenReturn(Optional.of(payee));
        when(repository.findById(id)).thenReturn(Optional.of(payee));
        return payee;
    }

    private static GenericExceptionMessages errorOf(Throwable e) {
        return ((MtgWalletGenericException) e).getError();
    }

    @Test
    void create_atLimit_isRejected() {
        when(repository.countByUserIdAndStatus(1L, Status.ACTIVE)).thenReturn((long) DomainLimits.MAX_PAYEES);

        assertThatThrownBy(() -> service.create(PayeeCreateRequest.builder().name("Shop").categoryId(7L).build()))
                .satisfies(e -> assertThat(errorOf(e)).isEqualTo(GenericExceptionMessages.PAYEES_LIMIT_EXCEEDED));
        verify(repository, never()).save(any());
    }

    @Test
    void create_justBelowLimit_isAllowed() throws Exception {
        when(repository.countByUserIdAndStatus(1L, Status.ACTIVE)).thenReturn((long) DomainLimits.MAX_PAYEES - 1);

        service.create(PayeeCreateRequest.builder().name("Shop").categoryId(7L).build());

        verify(repository).save(any(Payee.class));
    }

    @Test
    void create_nameDifferingOnlyInCase_isDuplicate() {
        when(repository.findNamesByUserIdAndStatus(1L, Status.ACTIVE, -1L)).thenReturn(List.of("MARKET")); // no "I": Turkish rules lowercase it to "ı"

        assertThatThrownBy(() -> service.create(PayeeCreateRequest.builder().name("market").categoryId(7L).build()))
                .satisfies(e -> assertThat(errorOf(e)).isEqualTo(GenericExceptionMessages.PAYEE_NAME_ALREADY_EXISTS));
        verify(repository, never()).save(any());
    }

    @Test
    void create_savesActivePayeeLinkedToCategoryAndCurrentUser() throws Exception {
        Category categoryRef = category(TransactionType.EXPENSE);
        WalletUser userRef = WalletUser.builder().id(1L).build();
        when(entityManager.getReference(Category.class, 7L)).thenReturn(categoryRef);
        when(entityManager.getReference(WalletUser.class, 1L)).thenReturn(userRef);
        org.mockito.ArgumentCaptor<Payee> saved = org.mockito.ArgumentCaptor.forClass(Payee.class);

        service.create(PayeeCreateRequest.builder().name("Shop").categoryId(7L).build());

        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo("Shop");
        assertThat(saved.getValue().getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(saved.getValue().getCategory()).isSameAs(categoryRef);
        assertThat(saved.getValue().getUser()).isSameAs(userRef);
    }

    @Test
    void update_renamesThePayee_excludingItselfFromTheNameCheck() throws Exception {
        Payee payee = storedPayee(5L, 1L, TransactionType.EXPENSE);
        when(repository.findNamesByUserIdAndStatus(1L, Status.ACTIVE, 5L)).thenReturn(List.of("Other"));

        service.update(payeeUpdate("SHOP"), 5L); // only the case changes: not a clash with itself

        assertThat(payee.getName()).isEqualTo("SHOP");
        verify(repository).save(payee);
    }

    @Test
    void update_toAnotherPayeesName_isRejected() {
        Payee payee = storedPayee(5L, 1L, TransactionType.EXPENSE);
        when(repository.findNamesByUserIdAndStatus(1L, Status.ACTIVE, 5L)).thenReturn(List.of("Market"));

        assertThatThrownBy(() -> service.update(payeeUpdate("market"), 5L))
                .satisfies(e -> assertThat(errorOf(e)).isEqualTo(GenericExceptionMessages.PAYEE_NAME_ALREADY_EXISTS));
        assertThat(payee.getName()).isEqualTo("Shop");
    }

    @Test
    void update_ofOtherUsersPayee_isRejected() throws Exception {
        Payee payee = storedPayee(5L, 2L, TransactionType.EXPENSE);
        doThrow(new MtgWalletGenericException(GenericExceptionMessages.NOT_AUTHORIZED_TO_PERFORM))
                .when(userService).validateUserIdIfItsTheCurrentUser(2L);

        assertThatThrownBy(() -> service.update(payeeUpdate("Mine now"), 5L))
                .satisfies(e -> assertThat(errorOf(e)).isEqualTo(GenericExceptionMessages.NOT_AUTHORIZED_TO_PERFORM));
        assertThat(payee.getName()).isEqualTo("Shop");
        verify(repository, never()).save(any());
    }

    @Test
    void delete_isSoft_andKeepsTheName() throws Exception {
        Payee payee = storedPayee(5L, 1L, TransactionType.EXPENSE);

        service.delete(5L);

        assertThat(payee.getStatus()).isEqualTo(Status.DELETED);
        assertThat(payee.getName()).isEqualTo("Shop");
        verify(repository).save(payee);
    }

    @Test
    void delete_missingPayee_isNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
                .satisfies(e -> assertThat(errorOf(e)).isEqualTo(GenericExceptionMessages.PAYEE_NOT_FOUND));
    }

    @Test
    void delete_ofOtherUsersPayee_isRejected() throws Exception {
        Payee payee = storedPayee(5L, 2L, TransactionType.EXPENSE);
        doThrow(new MtgWalletGenericException(GenericExceptionMessages.NOT_AUTHORIZED_TO_PERFORM))
                .when(userService).validateUserIdIfItsTheCurrentUser(2L);

        assertThatThrownBy(() -> service.delete(5L)).isInstanceOf(MtgWalletGenericException.class);
        assertThat(payee.getStatus()).isEqualTo(Status.ACTIVE);
    }

    @Test
    void defaultIncomePayee_activeIncomePayee_isSet() throws Exception {
        Payee payee = storedPayee(5L, 1L, TransactionType.INCOME);

        service.addDefaultPayeeForIncomeToUser(5L);

        verify(userService).setDefaultPayeeForIncome(payee);
    }

    @Test
    void defaultIncomePayee_withExpenseCategory_isRejected() {
        storedPayee(5L, 1L, TransactionType.EXPENSE);

        assertThatThrownBy(() -> service.addDefaultPayeeForIncomeToUser(5L))
                .satisfies(e -> assertThat(errorOf(e)).isEqualTo(GenericExceptionMessages.PAYEE_CATEGORY_TRANSACTION_TYPE_NOT_VALID));
        verify(userService, never()).setDefaultPayeeForIncome(any());
    }

    private static com.mtg.mtgwalletbe.api.request.PayeeUpdateRequest payeeUpdate(String name) {
        com.mtg.mtgwalletbe.api.request.PayeeUpdateRequest request = new com.mtg.mtgwalletbe.api.request.PayeeUpdateRequest();
        request.setName(name);
        return request;
    }

    private static Category category(TransactionType type) {
        Category category = new Category();
        category.setId(7L);
        category.setTransactionType(type);
        return category;
    }
}
