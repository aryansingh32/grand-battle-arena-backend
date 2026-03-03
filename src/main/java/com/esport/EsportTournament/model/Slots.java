package com.esport.EsportTournament.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;



@Entity
@NoArgsConstructor
@AllArgsConstructor
@Component
@Getter
@Setter
// Slot Object creating and assign to tournament Object id in tournament id linkage
public class Slots {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "tournament_id", referencedColumnName = "id")
    private Tournaments tournaments; //tournament id which user is registering

    private int slotNumber;

    @ManyToOne
    @JoinColumn(name = "firebase_userUID", referencedColumnName = "firebaseUserUID")
    private Users user; // who booked the slot

    private String playerName; // registration Namee

    @Enumerated(EnumType.STRING)
    private SlotStatus status;


    private LocalDateTime bookedAt;

    /**
     * JPA optimistic locking — defense-in-depth.
     * If two transactions try to update the same slot row concurrently,
     * the second one gets an OptimisticLockException instead of silently overwriting.
     */
    @Version
    private Long version;

    public enum SlotStatus{
        AVAILABLE,
        BOOKED
    }


}
