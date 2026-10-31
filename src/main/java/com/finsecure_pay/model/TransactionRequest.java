package com.finsecure_pay.model;

import java.math.BigDecimal;

/**
 * Incoming transaction.
 *
 * @param transactionId ID of the transaction.
 * @param amount amount to transfer
 * @param currency currency code such as {@code IDR}
 * @param fromAccount account the transaction comes from
 * @param toAccount account the transaction goes to
 *
 * @author King Kel Vin
 */
public record TransactionRequest (
        String transactionId,
        BigDecimal amount,
        String currency,
        String fromAccount,
        String toAccount
) {}
