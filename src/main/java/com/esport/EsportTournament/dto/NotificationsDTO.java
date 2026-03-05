package com.esport.EsportTournament.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationsDTO {
    private int id;
    private String firebaseUserUID;
    private String title;
    private String message;
    private LocalDateTime sentAt;
    private boolean isRead;
    private String type;
}
