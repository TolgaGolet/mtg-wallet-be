package com.mtg.mtgwalletbe.service;

import com.mtg.mtgwalletbe.api.request.AccountCreateRequest;
import com.mtg.mtgwalletbe.api.request.AccountUpdateRequest;
import com.mtg.mtgwalletbe.entity.Account;
import com.mtg.mtgwalletbe.entity.WalletUser;
import com.mtg.mtgwalletbe.enums.AccountType;
import com.mtg.mtgwalletbe.enums.Currency;
import com.mtg.mtgwalletbe.enums.Status;
import com.mtg.mtgwalletbe.exception.MtgWalletGenericException;
import com.mtg.mtgwalletbe.exception.enums.GenericExceptionMessages;
import com.mtg.mtgwalletbe.mapper.AccountServiceMapper;
import com.mtg.mtgwalletbe.repository.AccountRepository;
import com.mtg.mtgwalletbe.service.dto.AccountDto;
import com.mtg.mtgwalletbe.service.dto.WalletUserBasicDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.mtg.mtgwalletbe.service.DomainLimits.MAX_ACCOUNTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// F7: AccountServiceImpl had no tests (ownership, limits, names, balances)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccountServiceImplTest {

    private static final long CURRENT_USER = 1L;
    private static final long OTHER_USER = 2L;

    @Mock private AccountRepository repository;
    @Mock private AccountServiceMapper mapper;
    @Mock private UserService userService;
    @InjectMocks private AccountServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        WalletUserBasicDto user = new WalletUserBasicDto();
        user.setId(CURRENT_USER);
        when(userService.getCurrentLoggedInUser()).thenReturn(user);
        when(userService.getCurrentUserId()).thenReturn(CURRENT_USER);
        doThrow(new MtgWalletGenericException(GenericExceptionMessages.NOT_AUTHORIZED_TO_PERFORM))
                .when(userService).validateUserIdIfItsTheCurrentUser(OTHER_USER);
        when(repository.findNamesByUserIdAndStatus(anyLong(), any(), anyLong())).thenReturn(List.of());
        when(repository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toAccountEntity(any(AccountDto.class))).thenReturn(new Account());
    }

    private static Account account(long id, long ownerId, String balance) {
        Account account = new Account();
        account.setId(id);
        account.setUser(WalletUser.builder().id(ownerId).build());
        account.setBalance(new BigDecimal(balance));
        account.setName("Wallet");
        account.setStatus(Status.ACTIVE);
        return account;
    }

    private static AccountCreateRequest createRequest(String name) {
        AccountCreateRequest request = new AccountCreateRequest();
        request.setName(name);
        request.setTypeValue("CASH");
        request.setCurrencyValue("TRY");
        request.setBalance(new BigDecimal("10"));
        return request;
    }

    @Test
    void create_atLimit_isRejected() {
        when(repository.countByUserIdAndStatus(CURRENT_USER, Status.ACTIVE)).thenReturn((long) MAX_ACCOUNTS);

        assertThatThrownBy(() -> service.create(createRequest("Wallet")))
                .extracting(e -> ((MtgWalletGenericException) e).getError())
                .isEqualTo(GenericExceptionMessages.ACCOUNTS_LIMIT_EXCEEDED);
        verify(repository, never()).save(any());
    }

    @Test
    void create_duplicateNameIgnoringCase_isRejected() {
        when(repository.findNamesByUserIdAndStatus(CURRENT_USER, Status.ACTIVE, -1L)).thenReturn(List.of("WALLET"));

        assertThatThrownBy(() -> service.create(createRequest("wallet")))
                .extracting(e -> ((MtgWalletGenericException) e).getError())
                .isEqualTo(GenericExceptionMessages.ACCOUNT_NAME_ALREADY_EXISTS);
    }

    @Test
    void create_savesActiveAccountOfCurrentUser_withoutMutatingTheRequest() throws Exception {
        AccountCreateRequest request = createRequest("Wallet");
        ArgumentCaptor<AccountDto> dto = ArgumentCaptor.forClass(AccountDto.class);

        service.create(request);

        verify(mapper).toAccountEntity(dto.capture());
        assertThat(dto.getValue().getUserId()).isEqualTo(CURRENT_USER);
        assertThat(dto.getValue().getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(dto.getValue().getType()).isEqualTo(AccountType.CASH);
        assertThat(dto.getValue().getCurrency()).isEqualTo(Currency.TRY);
        assertThat(dto.getValue().getBalance()).isEqualByComparingTo("10");
        assertThat(request.getBalance()).isEqualByComparingTo("10");
    }

    @Test
    void adjustBalance_addsDeltaToTheManagedAccount() throws Exception {
        Account account = account(10L, CURRENT_USER, "100");
        when(repository.findById(10L)).thenReturn(Optional.of(account));

        service.adjustBalance(10L, new BigDecimal("-30.50"));

        assertThat(account.getBalance()).isEqualByComparingTo("69.50");
        assertThat(account.getUser().getId()).isEqualTo(CURRENT_USER); // F3: the owner is never rewritten
    }

    @Test
    void adjustBalance_ofOtherUsersAccount_isRejected() {
        when(repository.findById(10L)).thenReturn(Optional.of(account(10L, OTHER_USER, "100")));

        assertThatThrownBy(() -> service.adjustBalance(10L, BigDecimal.ONE))
                .extracting(e -> ((MtgWalletGenericException) e).getError())
                .isEqualTo(GenericExceptionMessages.NOT_AUTHORIZED_TO_PERFORM);
    }

    @Test
    void adjustBalance_missingAccount_isNotFound() {
        when(repository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.adjustBalance(10L, BigDecimal.ONE))
                .extracting(e -> ((MtgWalletGenericException) e).getError())
                .isEqualTo(GenericExceptionMessages.ACCOUNT_NOT_FOUND);
    }

    @Test
    void update_ofOtherUsersAccount_isRejected() {
        when(repository.findByIdAndStatus(10L, Status.ACTIVE)).thenReturn(Optional.of(account(10L, OTHER_USER, "0")));
        AccountUpdateRequest request = new AccountUpdateRequest();
        request.setName("New name");
        request.setTypeValue("CASH");

        assertThatThrownBy(() -> service.update(request, 10L))
                .extracting(e -> ((MtgWalletGenericException) e).getError())
                .isEqualTo(GenericExceptionMessages.NOT_AUTHORIZED_TO_PERFORM);
    }

    // Soft delete keeps the name, so old transactions still show which account they used
    @Test
    void delete_marksDeleted_andKeepsTheName() throws Exception {
        Account account = account(10L, CURRENT_USER, "0");
        when(repository.findById(10L)).thenReturn(Optional.of(account));

        service.delete(10L);

        assertThat(account.getStatus()).isEqualTo(Status.DELETED);
        assertThat(account.getName()).isEqualTo("Wallet");
    }

    @Test
    void totalBalance_isSummedByTheDatabase_forActiveAccountsOfCurrentUser() {
        when(repository.sumBalanceByUserIdAndStatusAndCurrency(CURRENT_USER, Status.ACTIVE, Currency.USD)).thenReturn(new BigDecimal("42"));

        assertThat(service.getTotalBalanceByCurrentUserAndCurrency(Currency.USD)).isEqualByComparingTo("42");
    }
}
