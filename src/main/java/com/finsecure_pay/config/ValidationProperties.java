package com.finsecure_pay.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Configuration properties for request validation.
 *
 * @param maxAmount Max acceptable amount
 * @param supportedCurrencies Set of supported currencies
 *
 * @author King Kel Vin
 */
@ConfigurationProperties(prefix = "finsecure.validation")
public record ValidationProperties (
        @DefaultValue("10000.00") BigDecimal maxAmount,
        @DefaultValue({"IDR","SGD","USD","EUR"}) Set<String> supportedCurrencies
) {}
