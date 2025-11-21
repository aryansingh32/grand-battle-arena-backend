package com.esport.EsportTournament.repository.rbac;

import com.esport.EsportTournament.model.rbac.AppPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppPermissionRepository extends JpaRepository<AppPermission, Long> {

    Optional<AppPermission> findByCode(String code);
}

