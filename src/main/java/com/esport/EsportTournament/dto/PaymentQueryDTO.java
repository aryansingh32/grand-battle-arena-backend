package com.esport.EsportTournament.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO for user requests to get QR code
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentQueryDTO {

    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount must be greater than 0")
    private Integer amount;
}
