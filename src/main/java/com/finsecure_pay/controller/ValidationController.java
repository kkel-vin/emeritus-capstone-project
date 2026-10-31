package com.finsecure_pay.controller;

import com.finsecure_pay.model.TransactionRequest;
import com.finsecure_pay.model.ValidationResult;
import com.finsecure_pay.service.TransactionValidator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the service validation endpoint.
 *
 * @author King Kel Vin
 */
@RestController
public class ValidationController {

    private final TransactionValidator transactionValidator;

    public ValidationController(TransactionValidator transactionValidator) {
        this.transactionValidator = transactionValidator;
    }

    /**
     * Validates the transaction.
     *
     * <p> Responds with {@code 200 OK} when validation passes or
     * {@code 422 Unprocessable Content} with the list of failed rules.
     *
     * @param request the transaction to validate.
     * @return validation result.
     */
    @PostMapping("/validate")
    public ResponseEntity<ValidationResult> validate(@RequestBody TransactionRequest request) {
        ValidationResult result = transactionValidator.validate(request);
        HttpStatus status = result.valid() ? HttpStatus.OK : HttpStatus.UNPROCESSABLE_CONTENT;
        return ResponseEntity.status(status).body(result);
    }
}
