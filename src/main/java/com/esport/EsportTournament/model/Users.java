package com.esport.EsportTournament.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


import java.time.LocalDateTime;


@Entity
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(unique = true, nullable = false)
    private String firebaseUserUID;

    @Column(unique = true, nullable = false)
    private String email;

    private String userName;

    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    private UserStatus status = UserStatus.ACTIVE;

    private LocalDateTime createdAt;
    @Column(name = "device_token", length = 500)
    private String deviceToken;

    @Column(name = "device_token_updated_at")
    private LocalDateTime deviceTokenUpdatedAt;
    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;


    public enum UserRole { USER, ADMIN }
    public enum UserStatus { ACTIVE, INACTIVE, BANNED }
}

