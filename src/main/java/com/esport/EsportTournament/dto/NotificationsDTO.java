package com.esport.EsportTournament.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class NotificationsDTO {
    private int id;
    private String firebaseUserUID;
    private String message;
    private LocalDateTime sentAt;
}
