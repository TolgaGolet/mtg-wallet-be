package com.mtg.mtgwalletbe.api.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.mtg.mtgwalletbe.api.request.ValidationPatterns.NAME_REGULAR_EXPRESSION;

/**
 * Fields and validation shared by transaction create and update, so the rules exist once. The JSON shape of both
 * requests is this class.
 */
@Data
@NoArgsConstructor // Jackson 3 needs it to deserialize request bodies
public abstract class TransactionWriteRequest {
    /**
     * payeeId value meaning "create a new payee from payeeName" (sent by the frontend for create and update)
     */
    public static final long NEW_PAYEE_ID = -1L;

    @NotNull
    @Size(min = 1, max = 15)
    private String typeValue;
    @NotNull
    private Long payeeId;
    // When creating a new payee
    @Size(min = 3, max = 50)
    @Pattern(regexp = NAME_REGULAR_EXPRESSION)
    private String payeeName;
    // When creating a new payee
    private Long categoryId;
    @NotNull
    @DecimalMin("0.01") // same rule as the frontend transaction form
    @Digits(integer = 16, fraction = 2)
    private BigDecimal amount;
    @NotNull
    private LocalDateTime dateTime;
    @NotNull
    private Long sourceAccountId;
    private Long targetAccountId;
    @Size(min = 1, max = 50)
    private String notes;
}
