package com.esport.EsportTournament.repository;

import com.esport.EsportTournament.model.TournamentResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TournamentResultRepository extends JpaRepository<TournamentResult, Long> {

    List<TournamentResult> findByFirebaseUserUIDOrderByCreatedAtDesc(String firebaseUserUID);

    List<TournamentResult> findByTournament_Id(int tournamentId);

    void deleteByTournament_Id(int tournamentId);
}
