package com.mtg.mtgwalletbe.api.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

// Regression for CQ-06: amounts must be >= 0.01 (same rule as the frontend form)
class TransactionRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @ParameterizedTest
    @CsvSource({"-5, false", "0, false", "0.009, false", "0.01, true", "100.50, true"})
    void createRequest_amountRule(String amount, boolean valid) {
        TransactionCreateRequest request = new TransactionCreateRequest();
        request.setAmount(new BigDecimal(amount));
        assertThat(validator.validateProperty(request, "amount").isEmpty()).isEqualTo(valid);
    }

    @ParameterizedTest
    @CsvSource({"-5, false", "0, false", "0.01, true"})
    void updateRequest_amountRule(String amount, boolean valid) {
        TransactionUpdateRequest request = new TransactionUpdateRequest();
        request.setAmount(new BigDecimal(amount));
        assertThat(validator.validateProperty(request, "amount").isEmpty()).isEqualTo(valid);
    }
}
