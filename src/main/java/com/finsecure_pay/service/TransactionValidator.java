package com.finsecure_pay.service;

import com.finsecure_pay.config.ValidationProperties;
import com.finsecure_pay.model.TransactionRequest;
import com.finsecure_pay.model.ValidationResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates transaction against business rules.
 *
 * @author King Kel Vin
 */
@Service
public class TransactionValidator {

    private static final int MAX_TRANSACTION_ID_LENGTH = 64;
    private static final Pattern ACCOUNT_FORMAT = Pattern.compile("^[0-9]{12}$");

    private final ValidationProperties validationProperties;

    public TransactionValidator(ValidationProperties validationProperties) {
        this.validationProperties = validationProperties;
    }

    public ValidationResult validate(TransactionRequest request) {
        List<String> errors = new ArrayList<>();

        if (request == null) {
            errors.add("Transaction Request is required");
            return ValidationResult.of(null, errors);
        }

        validateTransactionId(request.transactionId(), errors);
        validateAmount(request.amount(), errors);
        validateCurrency(request.currency(), errors);
        validateAccounts(request.fromAccount(), request.toAccount(), errors);

        return ValidationResult.of(request.transactionId(), errors);
    }

    private void validateTransactionId(String transactionId, List<String> errors) {
        if (transactionId == null || transactionId.isBlank()) {
            errors.add("transactionId is required");
        } else if (transactionId.length() > MAX_TRANSACTION_ID_LENGTH) {
            errors.add("transactionId is too long. Must not exceed " + MAX_TRANSACTION_ID_LENGTH + " characters");
        }
    }

    private void validateAmount(BigDecimal amount, List<String> errors) {
        if (amount == null) {
            errors.add("amount is required");
            return;
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("amount must be greater than zero");
            return;
        }

        if (amount.stripTrailingZeros().scale() > 2) {
            errors.add("amount must have at most 2 decimal places");
        }

        if (amount.compareTo(validationProperties.maxAmount()) > 0) {
            errors.add("amount exceeds the maximum allowed of " + validationProperties.maxAmount().toPlainString());
        }
    }

    private void validateCurrency(String currency, List<String> errors) {
        if (currency == null || currency.isBlank()) {
            errors.add("currency is required");
        } else if (!validationProperties.supportedCurrencies().contains(currency.toUpperCase())) {
            errors.add("currency '" + currency + "' is not supported");
        }
    }

    private void validateAccounts(String fromAccount, String toAccount, List<String> errors) {
        boolean fromAccountOk = checkAccount("fromAccount", fromAccount, errors);
        boolean toAccountOk = checkAccount("toAccount", toAccount, errors);

        if (fromAccountOk && toAccountOk && fromAccount.equals(toAccount)) {
            errors.add("fromAccount and toAccount must be different");
        }
    }

    private boolean checkAccount(String field, String value, List<String> errors) {
        if (value == null || value.isBlank()) {
            errors.add(field + " is required");
            return false;
        }

        if (!ACCOUNT_FORMAT.matcher(value).matches()) {
            errors.add(field + " must be 12 digits");
            return false;
        }

        return true;
    }
}
