package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.request.CategoryCreateRequest;
import com.mtg.mtgwalletbe.api.request.CategoryUpdateRequest;
import com.mtg.mtgwalletbe.entity.Category;
import com.mtg.mtgwalletbe.entity.WalletUser;
import com.mtg.mtgwalletbe.enums.Status;
import com.mtg.mtgwalletbe.enums.TransactionType;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import com.mtg.mtgwalletbe.mapper.CategoryServiceMapper;
import com.mtg.mtgwalletbe.mapper.UserServiceMapper;
import com.mtg.mtgwalletbe.repository.CategoryRepository;
import com.mtg.mtgwalletbe.service.dto.WalletUserBasicDto;
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
import static org.mockito.Mockito.*;

// Regression for CQ-03: category hierarchy invariants
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CategoryServiceImplTest {

    @Mock private CategoryRepository repository;
    @Mock private CategoryServiceMapper mapper;
    @Mock private UserService userService;
    @Mock private UserServiceMapper userServiceMapper;
    @InjectMocks private CategoryServiceImpl service;

    private static final WalletUser USER = WalletUser.builder().id(1L).build();

    @BeforeEach
    void setUp() {
        WalletUserBasicDto user = new WalletUserBasicDto();
        user.setId(1L);
        when(userService.getCurrentLoggedInUser()).thenReturn(user);
        when(repository.saveAndFlush(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Category category(long id, TransactionType type, Category parent, boolean isParent) {
        Category category = new Category();
        category.setId(id);
        category.setName("Category " + id);
        category.setTransactionType(type);
        category.setParentCategory(parent);
        category.setIsParent(isParent);
        category.setStatus(Status.ACTIVE);
        category.setUser(USER);
        when(repository.findByIdAndStatus(id, Status.ACTIVE)).thenReturn(Optional.of(category));
        return category;
    }

    private GenericExceptionMessages errorOf(ThrowingRunnable call) {
        try {
            call.run();
        } catch (MtgWalletGenericException e) {
            return e.getError();
        }
        throw new AssertionError("expected MtgWalletGenericException");
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws MtgWalletGenericException;
    }

    private static CategoryUpdateRequest update(Long parentId) {
        return CategoryUpdateRequest.builder().name("Renamed").parentCategoryId(parentId).build();
    }

    @Test
    void update_selfParent_isRejected() {
        category(5, TransactionType.EXPENSE, null, false);

        assertThat(errorOf(() -> service.update(update(5L), 5L))).isEqualTo(GenericExceptionMessages.CATEGORY_CANT_BE_ITS_OWN_PARENT);
    }

    @Test
    void update_unknownOrDeletedParent_isNotFound_notNpe() {
        category(5, TransactionType.EXPENSE, null, false);
        when(repository.findByIdAndStatus(99L, Status.ACTIVE)).thenReturn(Optional.empty());

        assertThat(errorOf(() -> service.update(update(99L), 5L))).isEqualTo(GenericExceptionMessages.CATEGORY_NOT_FOUND);
    }

    @Test
    void update_parentWithDifferentTransactionType_isRejected() {
        category(5, TransactionType.EXPENSE, null, false);
        category(7, TransactionType.INCOME, null, false);

        assertThat(errorOf(() -> service.update(update(7L), 5L))).isEqualTo(GenericExceptionMessages.PARENT_CATEGORY_TRANSACTION_TYPE_MISMATCH);
    }

    @Test
    void update_parentThatHasAParent_isRejected() {
        Category grandParent = category(1, TransactionType.EXPENSE, null, true);
        category(7, TransactionType.EXPENSE, grandParent, false);
        category(5, TransactionType.EXPENSE, null, false);

        assertThat(errorOf(() -> service.update(update(7L), 5L))).isEqualTo(GenericExceptionMessages.CATEGORY_WITH_PARENT_CATEGORY_NOT_ALLOWED_AS_PARENT_CATEGORY);
    }

    @Test
    void update_switchingParent_refreshesOldParentAndMarksNewOne() throws Exception {
        Category oldParent = category(1, TransactionType.EXPENSE, null, true);
        Category newParent = category(2, TransactionType.EXPENSE, null, false);
        Category child = category(5, TransactionType.EXPENSE, oldParent, false);
        when(repository.existsByParentCategoryIdAndStatus(1L, Status.ACTIVE)).thenReturn(false);

        service.update(update(2L), 5L);

        assertThat(child.getParentCategory()).isSameAs(newParent);
        assertThat(newParent.getIsParent()).isTrue();
        assertThat(oldParent.getIsParent()).isFalse();
    }

    @Test
    void update_clearingParent_keepsOldParentFlagWhenItHasOtherChildren() throws Exception {
        Category oldParent = category(1, TransactionType.EXPENSE, null, true);
        Category child = category(5, TransactionType.EXPENSE, oldParent, false);
        when(repository.existsByParentCategoryIdAndStatus(1L, Status.ACTIVE)).thenReturn(true);

        service.update(update(null), 5L);

        assertThat(child.getParentCategory()).isNull();
        assertThat(oldParent.getIsParent()).isTrue();
    }

    @Test
    void create_withParentOfOtherType_isRejected() {
        category(7, TransactionType.INCOME, null, false);
        CategoryCreateRequest request = CategoryCreateRequest.builder().name("Gas").transactionTypeValue("EXP").parentCategoryId(7L).build();

        assertThat(errorOf(() -> service.create(request))).isEqualTo(GenericExceptionMessages.PARENT_CATEGORY_TRANSACTION_TYPE_MISMATCH);
    }

    // CQ-18: "Cash" and "cash" are the same name
    @Test
    void create_nameDifferingOnlyInCase_isDuplicate() {
        when(repository.findNamesByUserIdAndStatusAndType(1L, Status.ACTIVE, TransactionType.EXPENSE, -1L)).thenReturn(List.of("Market"));
        CategoryCreateRequest request = CategoryCreateRequest.builder().name("MARKET").transactionTypeValue("EXP").build();

        assertThat(errorOf(() -> service.create(request))).isEqualTo(GenericExceptionMessages.CATEGORY_NAME_ALREADY_EXISTS);
    }

    @Test
    void delete_parentWithActiveChildren_isRefused() {
        Category parent = category(1, TransactionType.EXPENSE, null, true);
        when(repository.existsByParentCategoryIdAndStatus(1L, Status.ACTIVE)).thenReturn(true);

        assertThat(errorOf(() -> service.delete(1L))).isEqualTo(GenericExceptionMessages.CATEGORY_HAS_ACTIVE_SUBCATEGORIES);
        assertThat(parent.getStatus()).isEqualTo(Status.ACTIVE);
    }

    @Test
    void delete_lastChild_clearsParentFlag() throws Exception {
        Category parent = category(1, TransactionType.EXPENSE, null, true);
        Category child = category(5, TransactionType.EXPENSE, parent, false);
        when(repository.existsByParentCategoryIdAndStatus(5L, Status.ACTIVE)).thenReturn(false);
        when(repository.existsByParentCategoryIdAndStatus(1L, Status.ACTIVE)).thenReturn(false);

        service.delete(5L);

        assertThat(child.getStatus()).isEqualTo(Status.DELETED);
        assertThat(parent.getIsParent()).isFalse();
    }

    // CQ-17: not found throws instead of returning null
    @Test
    void getCategory_missing_throwsNotFound() {
        when(repository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCategory(42L))
                .isInstanceOf(MtgWalletGenericException.class)
                .hasMessage(GenericExceptionMessages.CATEGORY_NOT_FOUND.getMessage());
    }
}
