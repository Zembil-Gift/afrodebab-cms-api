package com.afrodebab.cms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record MarkEmployeePaymentPaidRequest(
        @NotBlank(message = "transactionReference is required") String transactionReference,
        @Positive(message = "paidAmountMinor must be greater than zero") Long paidAmountMinor
) {}
