package com.esport.EsportTournament.repository.rbac;

import com.esport.EsportTournament.model.rbac.AppRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppRoleRepository extends JpaRepository<AppRole, Long> {

    Optional<AppRole> findByCode(String code);
}

