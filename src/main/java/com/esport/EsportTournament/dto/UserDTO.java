package com.esport.EsportTournament.dto;

import com.esport.EsportTournament.model.Users.UserRole;
import com.esport.EsportTournament.model.Users.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class UserDTO {
    private String firebaseUserUID;
    private String userName;
    private String email;
    private UserRole role;
    private UserStatus status;
    private LocalDateTime createdAt;

    @Override
    public String toString() {
        return "UserDTO{" +
                "firebaseUserUID='" + firebaseUserUID + '\'' +
                ", userName='" + userName + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}
