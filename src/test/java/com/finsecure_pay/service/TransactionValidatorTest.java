package com.finsecure_pay.service;

import com.finsecure_pay.config.ValidationProperties;
import com.finsecure_pay.model.TransactionRequest;
import com.finsecure_pay.model.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TransactionValidatorTest {

    private TransactionValidator transactionValidator;
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("10000.00");

    @BeforeEach
    void setUp() {
        ValidationProperties properties = new ValidationProperties(
                MAX_AMOUNT,
                Set.of("IDR","SGD","USD","EUR")
        );
        transactionValidator = new TransactionValidator(properties);
    }

    // Success cases

    @Test
    @DisplayName("Valid transaction passes validation")
    void validTransactionPassesValidation() {
        TransactionRequest validRequest = new TransactionRequest(
                "TXN-0001",
                new BigDecimal("750.50"),
                "SGD",
                "123456789012",
                "123456789013"
        );

        ValidationResult result = transactionValidator.validate(validRequest);

        assertTrue(result.valid());
        assertTrue(result.errors().isEmpty());
        assertEquals("TXN-0001", result.transactionId());
    }

    @Test
    @DisplayName("Amount exactly at the limit passes validation")
    void amountAtLimitPassesValidation() {
        TransactionRequest amountAtLimitRequest = new TransactionRequest(
                "TXN-0001",
                MAX_AMOUNT,
                "SGD",
                "123456789012",
                "123456789013"
        );
        ValidationResult result = transactionValidator.validate(amountAtLimitRequest);

        assertTrue(result.valid());
        assertTrue(result.errors().isEmpty());
        assertEquals("TXN-0001", result.transactionId());
    }

    @Test
    @DisplayName("Currency check is case-insensitive")
    void lowercaseCurrencyPasses() {
        TransactionRequest lowercaseCurrencyRequest = new TransactionRequest(
                "TXN-0001",
                new BigDecimal("750.50"),
                "sgd",
                "123456789012",
                "123456789013"
        );

        assertTrue(transactionValidator.validate(lowercaseCurrencyRequest).valid());
    }

    // Failure cases

    @Test
    @DisplayName("Null request is rejected")
    void nullRequestFails(){
        ValidationResult result = transactionValidator.validate(null);

        assertFalse(result.valid());
        assertTrue(result.errors().contains("Transaction Request is required"));
    }

    @Test
    @DisplayName("Missing fields are all reported together")
    void emptyRequestReportsEveryMissingField() {
        TransactionRequest emptyRequest = new TransactionRequest(
                null,
                null,
                null,
                null,
                null
        );

        ValidationResult result = transactionValidator.validate(emptyRequest);

        assertFalse(result.valid());
        assertEquals(5, result.errors().size());
    }

    @Test
    @DisplayName("Blank transactionId is rejected")
    void blankTransactionIdFails() {
        TransactionRequest blankTransactionIdRequest = new TransactionRequest(
                "  ",
                new BigDecimal("10"),
                "SGD",
                "123456789012",
                "098765432112"
        );

        assertTrue(transactionValidator.validate(blankTransactionIdRequest).errors().contains("transactionId is required"));
    }

    @Test
    @DisplayName("Overlong transactionId is rejected")
    void longTransactionIdFails() {
        TransactionRequest longTransactionIdRequest = new TransactionRequest(
                "X".repeat(100),
                new BigDecimal("10"),
                "SGD",
                "123456789012",
                "098765432112"
        );

        assertFalse(transactionValidator.validate(longTransactionIdRequest).valid());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-5", "-0.01"})
    @DisplayName("Zero or negative amounts are rejected")
    void nonPositiveAmountFails(String amount) {
        TransactionRequest nonPositiveAmountRequest = new TransactionRequest(
                "TXN-1",
                new BigDecimal(amount),
                "SGD",
                "123456789012",
                "098765432112"
        );

        ValidationResult result = transactionValidator.validate(nonPositiveAmountRequest);

        assertFalse(result.valid());
        assertTrue(result.errors().contains("amount must be greater than zero"));
    }

    @Test
    @DisplayName("An amount over the limit is rejected")
    void amountOverLimitFails() {
        TransactionRequest amountOverLimitRequest = new TransactionRequest(
                "TXN-1",
                new BigDecimal("10000.01"),
                "SGD",
                "123456789012",
                "098765432112"
        );

        ValidationResult result = transactionValidator.validate(amountOverLimitRequest);

        assertFalse(result.valid());
        assertTrue(result.errors().getFirst().startsWith("amount exceeds the maximum"));
    }

    @Test
    @DisplayName("More than two decimal places is rejected")
    void tooManyDecimalPlacesFails() {
        TransactionRequest tooManyDecimalPlacesRequest = new TransactionRequest(
                "TXN-1",
                new BigDecimal("10.005"),
                "SGD",
                "123456789012",
                "098765432112"
        );

        assertTrue(transactionValidator.validate(tooManyDecimalPlacesRequest).errors().contains("amount must have at most 2 decimal places"));
    }

    @Test
    @DisplayName("An unsupported currency is rejected")
    void unsupportedCurrencyFails() {
        TransactionRequest unsupportedCurrencyRequest = new TransactionRequest(
                "TXN-1",
                new BigDecimal("10"),
                "XYZ",
                "123456789012",
                "098765432112"
        );

        assertTrue(transactionValidator.validate(unsupportedCurrencyRequest).errors().contains("currency 'XYZ' is not supported"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "123456789012345", "12345abcde", "123 456 7890"})
    @DisplayName("Malformed account numbers are rejected")
    void malformedAccountFails(String account) {
        TransactionRequest malformedAccountRequest = new TransactionRequest(
                "TXN-1",
                new BigDecimal("10"),
                "SGD",
                account,
                "098765432112"
        );

        ValidationResult result = transactionValidator.validate(malformedAccountRequest);

        assertFalse(result.valid());
        assertTrue(result.errors().contains("fromAccount must be 12 digits"));
    }

    @Test
    @DisplayName("Sending money to the same account is rejected")
    void sameAccountFails() {
        TransactionRequest sameAccountRequest = new TransactionRequest(
                "TXN-1",
                new BigDecimal("10"),
                "SGD",
                "123456789012",
                "123456789012"
        );

        ValidationResult result = transactionValidator.validate(sameAccountRequest);

        assertFalse(result.valid());
        assertTrue(result.errors().contains("fromAccount and toAccount must be different"));
    }

    @Test
    @DisplayName("Multiple broken rules are all reported in one response")
    void multipleErrorsReported() {
        TransactionRequest multipleErrorsRequest = new TransactionRequest(
                "",
                new BigDecimal("-1"),
                "XYZ",
                "abc",
                "123456789012"
        );

        assertEquals(4, transactionValidator.validate(multipleErrorsRequest).errors().size());
    }
}
  