package com.esport.EsportTournament.repository.rbac;

import com.esport.EsportTournament.model.Users;
import com.esport.EsportTournament.model.rbac.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    List<UserRole> findByUser(Users user);

    List<UserRole> findByUser_FirebaseUserUID(String firebaseUID);

    boolean existsByUserAndRole_Code(Users user, String roleCode);

    Optional<UserRole> findByUserAndRole_Code(Users user, String roleCode);
}

