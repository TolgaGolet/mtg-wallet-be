package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.request.PayeeCreateRequest;
import com.mtg.mtgwalletbe.entity.Payee;
import com.mtg.mtgwalletbe.entity.WalletUser;
import com.mtg.mtgwalletbe.enums.Status;
import com.mtg.mtgwalletbe.enums.TransactionType;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import com.mtg.mtgwalletbe.mapper.PayeeServiceMapper;
import com.mtg.mtgwalletbe.repository.PayeeRepository;
import com.mtg.mtgwalletbe.service.dto.CategoryDto;
import com.mtg.mtgwalletbe.service.dto.PayeeDto;
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
                .extracting(e -> ((MtgWalletGenericException) e).getError())
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
                .extracting(e -> ((MtgWalletGenericException) e).getError())
                .isEqualTo(GenericExceptionMessages.PAYEE_NOT_FOUND);
        verify(userService, never()).setDefaultPayeeForExpense(any());
    }

    @Test
    void defaultPayee_activeExpensePayee_isSet() throws Exception {
        Payee payee = new Payee();
        payee.setId(5L);
        payee.setUser(WalletUser.builder().id(1L).build());
        when(repository.findByIdAndStatus(5L, Status.ACTIVE)).thenReturn(Optional.of(payee));
        when(mapper.toPayeeDto(payee)).thenReturn(PayeeDto.builder().id(5L)
                .category(CategoryDto.builder().transactionType(TransactionType.EXPENSE).build()).build());

        service.addDefaultPayeeForExpenseToUser(5L);

        verify(userService).setDefaultPayeeForExpense(any());
        assertThat(payee.getId()).isEqualTo(5L);
    }
}
