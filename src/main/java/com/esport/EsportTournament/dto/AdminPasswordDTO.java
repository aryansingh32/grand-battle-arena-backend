package com.esport.EsportTournament.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO for password verification
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminPasswordDTO {

    @NotBlank(message = "Admin password is required")
    private String adminPassword;
}
