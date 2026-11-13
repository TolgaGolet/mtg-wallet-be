package com.mtg.mtgwalletbe.api.response;

import com.mtg.mtgwalletbe.enums.AccountType;
import com.mtg.mtgwalletbe.enums.Currency;
import com.mtg.mtgwalletbe.enums.Status;
import com.mtg.mtgwalletbe.enums.TransactionType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Row of POST /transaction/search. Field names and nesting match what the frontend reads
 * (transaction.payee.category.name, transaction.sourceAccount.currency.value, ...); internal fields such as
 * userId, username and version are deliberately left out.
 */
@Data
public class TransactionListResponse {
    private Long id;
    private TransactionType type;
    private PayeeSummary payee;
    private BigDecimal amount;
    private LocalDateTime dateTime;
    private AccountSummary sourceAccount;
    private BigDecimal sourceAccountNewBalance;
    private BigDecimal targetAccountNewBalance;
    private AccountSummary targetAccount;
    private String notes;

    @Data
    public static class PayeeSummary {
        private Long id;
        private String name;
        private CategorySummary category;
        private Status status;
    }

    @Data
    public static class CategorySummary {
        private Long id;
        private String name;
        private TransactionType transactionType;
        private Boolean isParent;
        private Long parentCategoryId;
        private String parentCategoryName;
        private Status status;
    }

    @Data
    public static class AccountSummary {
        private Long id;
        private String name;
        private AccountType type;
        private BigDecimal balance;
        private Currency currency;
        private Status status;
    }
}
