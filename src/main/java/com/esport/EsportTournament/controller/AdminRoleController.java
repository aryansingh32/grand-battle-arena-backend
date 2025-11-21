package com.esport.EsportTournament.controller;

import com.esport.EsportTournament.model.rbac.AppRole;
import com.esport.EsportTournament.service.RbacService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
public class AdminRoleController {

    private final RbacService rbacService;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_MANAGE_ROLES') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<List<RoleResponse>> getRoles() {
        List<RoleResponse> roles = rbacService.listRoles().stream()
                .map(RoleResponse::from)
                .toList();
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/users/{firebaseUID}")
    @PreAuthorize("hasAuthority('PERM_MANAGE_ROLES') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getUserRoles(@PathVariable String firebaseUID) {
        return ResponseEntity.ok(Map.of(
                "firebaseUID", firebaseUID,
                "roles", rbacService.getUserRoles(firebaseUID)
        ));
    }

    @PutMapping("/users/{firebaseUID}")
    @PreAuthorize("hasAuthority('PERM_MANAGE_ROLES') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> updateUserRoles(@PathVariable String firebaseUID,
                                                               @RequestBody RoleAssignmentRequest request) {
        rbacService.assignRoles(firebaseUID, request.getRoles());
        return ResponseEntity.ok(Map.of(
                "firebaseUID", firebaseUID,
                "roles", rbacService.getUserRoles(firebaseUID)
        ));
    }

    @Data
    public static class RoleAssignmentRequest {
        private List<String> roles = List.of();
    }

    @Data
    @AllArgsConstructor
    public static class RoleResponse {
        private String code;
        private String description;

        public static RoleResponse from(AppRole role) {
            return new RoleResponse(role.getCode(), role.getDescription());
        }
    }
}

