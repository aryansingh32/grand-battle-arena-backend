// SlotsDTO.java - FIXED for Flutter compatibility
// ✅ Fixed: JSON field naming (booked_at → bookedAt)

package com.esport.EsportTournament.dto;

import com.esport.EsportTournament.model.Slots;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SlotsDTO {
    private int id;
    private int tournamentId;
    private int slotNumber;

    @JsonProperty("firebaseUserUID")
    private String firebaseUserUID;

    @JsonProperty("playerName")
    private String playerName;

    @JsonProperty("status")
    private Slots.SlotStatus status;

    // 🔥 CRITICAL FIX: Use bookedAt (camelCase) not booked_at (snake_case)
    // Flutter model expects: bookedAt
    @JsonProperty("bookedAt")
    private LocalDateTime bookedAt;
}