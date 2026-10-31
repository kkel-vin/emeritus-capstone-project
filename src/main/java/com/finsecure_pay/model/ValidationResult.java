package com.finsecure_pay.model;

import java.util.List;

/**
 * Validates a transaction.
 *
 * @param valid {@code true} if the transaction passed every rule.
 * @param transactionId ID of the transaction that was checked.
 * @param errors reasons the validation failed. Empty when {@code valid} is {@code true}.
 *
 * @author King Kel Vin
 */
public record ValidationResult (boolean valid, String transactionId, List<String> errors) {

    /**
     * Builds a result from a list of rule violations. Result is valid when the list is empty.
     *
     * @param transactionId ID of the transaction that was checked.
     * @param errors reasons the validation failed. Empty when {@code valid} is {@code true}.
     * @return {@code ValidationResult}
     */
    public static ValidationResult of (String transactionId, List<String> errors) {
        return new ValidationResult(errors.isEmpty(), transactionId, List.copyOf(errors));
    }
}
