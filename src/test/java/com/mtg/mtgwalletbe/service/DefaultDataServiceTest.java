package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.entity.Category;
import com.mtg.mtgwalletbe.entity.Payee;
import com.mtg.mtgwalletbe.entity.WalletUser;
import com.mtg.mtgwalletbe.enums.Status;
import com.mtg.mtgwalletbe.enums.TransactionType;
import com.mtg.mtgwalletbe.repository.CategoryRepository;
import com.mtg.mtgwalletbe.repository.PayeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

// CQ-09: defaults are created with two batched inserts instead of ~55 checked service calls
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DefaultDataServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private PayeeRepository payeeRepository;
    @Mock private UserService userService;
    @InjectMocks private DefaultDataService service;

    private WalletUser user;

    @BeforeEach
    void setUp() {
        user = WalletUser.builder().id(1L).isDefaultsCreated(Boolean.FALSE).build();
        when(userService.getCurrentUserEntity()).thenReturn(user);
        when(categoryRepository.findAllByUserIdAndStatus(1L, Status.ACTIVE)).thenReturn(List.of());
        when(payeeRepository.findNamesByUserIdAndStatus(1L, Status.ACTIVE, -1L)).thenReturn(List.of());
    }

    @SuppressWarnings("unchecked")
    private List<Category> savedCategories() {
        ArgumentCaptor<List<Category>> captor = ArgumentCaptor.forClass(List.class);
        verify(categoryRepository, times(1)).saveAll(captor.capture());
        return captor.getValue();
    }

    @SuppressWarnings("unchecked")
    private List<Payee> savedPayees() {
        ArgumentCaptor<List<Payee>> captor = ArgumentCaptor.forClass(List.class);
        verify(payeeRepository, times(1)).saveAll(captor.capture());
        return captor.getValue();
    }

    private static Category named(List<Category> categories, String name, TransactionType type) {
        return categories.stream().filter(c -> c.getName().equals(name) && c.getTransactionType() == type).findFirst().orElseThrow();
    }

    @Test
    void createsAllDefaultsInOneBatchPerTable_withHierarchy() {
        service.createDefaultsForCurrentUser();

        List<Category> categories = savedCategories();
        assertThat(categories).hasSize(DefaultDataService.CATEGORIES.size());
        Category auto = named(categories, "Auto", TransactionType.EXPENSE);
        Category gas = named(categories, "Gas", TransactionType.EXPENSE);
        assertThat(gas.getParentCategory()).isSameAs(auto);
        assertThat(auto.getIsParent()).isTrue();
        assertThat(gas.getIsParent()).isFalse();
        // same name, different type: separate categories
        assertThat(named(categories, "Investment", TransactionType.INCOME)).isNotSameAs(named(categories, "Investment", TransactionType.TRANSFER));
        assertThat(categories).allMatch(c -> c.getUser() == user && c.getStatus() == Status.ACTIVE);

        List<Payee> payees = savedPayees();
        assertThat(payees).hasSize(DefaultDataService.PAYEES.size());
        Payee shell = payees.stream().filter(p -> p.getName().equals("Shell")).findFirst().orElseThrow();
        assertThat(shell.getCategory()).isSameAs(gas);
        assertThat(user.getIsDefaultsCreated()).isTrue();
        verify(categoryRepository, never()).countByUserIdAndStatus(anyLong(), any());
    }

    @Test
    void skipsExistingNames_andReusesExistingParent() {
        Category existingAuto = new Category();
        existingAuto.setId(50L);
        existingAuto.setName("auto");
        existingAuto.setTransactionType(TransactionType.EXPENSE);
        existingAuto.setIsParent(Boolean.FALSE);
        when(categoryRepository.findAllByUserIdAndStatus(1L, Status.ACTIVE)).thenReturn(new ArrayList<>(List.of(existingAuto)));
        when(payeeRepository.findNamesByUserIdAndStatus(1L, Status.ACTIVE, -1L)).thenReturn(List.of("SHELL"));

        service.createDefaultsForCurrentUser();

        List<Category> categories = savedCategories();
        assertThat(categories).hasSize(DefaultDataService.CATEGORIES.size() - 1);
        assertThat(named(categories, "Gas", TransactionType.EXPENSE).getParentCategory()).isSameAs(existingAuto);
        assertThat(existingAuto.getIsParent()).isTrue();
        assertThat(savedPayees()).extracting(Payee::getName).doesNotContain("Shell");
    }

    // Regression: defaults bypassed the per-user caps that CategoryService/PayeeService.create enforce
    @Test
    void stopsAtThePerUserLimits() {
        List<Category> existing = new ArrayList<>();
        for (int i = 0; i < DomainLimits.MAX_CATEGORIES - 2; i++) {
            Category category = new Category();
            category.setName("Existing " + i);
            category.setTransactionType(TransactionType.EXPENSE);
            existing.add(category);
        }
        List<String> existingPayees = new ArrayList<>();
        for (int i = 0; i < DomainLimits.MAX_PAYEES - 1; i++) {
            existingPayees.add("Payee " + i);
        }
        when(categoryRepository.findAllByUserIdAndStatus(1L, Status.ACTIVE)).thenReturn(existing);
        when(payeeRepository.findNamesByUserIdAndStatus(1L, Status.ACTIVE, -1L)).thenReturn(existingPayees);

        service.createDefaultsForCurrentUser();

        assertThat(savedCategories()).hasSize(2);
        assertThat(savedPayees()).hasSizeLessThanOrEqualTo(1);
    }

    @Test
    void doesNothingWhenDefaultsAlreadyCreated() {
        user.setIsDefaultsCreated(Boolean.TRUE);

        service.createDefaultsForCurrentUser();

        verify(categoryRepository, never()).saveAll(any());
        verify(payeeRepository, never()).saveAll(any());
    }
}
