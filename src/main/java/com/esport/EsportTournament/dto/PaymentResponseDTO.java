package com.esport.EsportTournament.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// DTO for responses to users (without sensitive info)
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentResponseDTO {

    private Long id;
    private Integer amount;
    private String upiIdQrLink;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isActive;

    // Constructor for user response (limited info)
    public PaymentResponseDTO(Integer amount, String upiIdQrLink) {
        this.amount = amount;
        this.upiIdQrLink = upiIdQrLink;
    }
}
