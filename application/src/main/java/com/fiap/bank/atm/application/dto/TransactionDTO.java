package com.fiap.bank.atm.application.dto;

import java.time.LocalDateTime;

public record TransactionDTO(
        LocalDateTime timestamp,
        String typeDescription,
        String formattedAmount
) {
}