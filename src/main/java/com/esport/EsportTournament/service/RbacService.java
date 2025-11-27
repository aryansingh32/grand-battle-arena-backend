package com.esport.EsportTournament.service;

import com.esport.EsportTournament.model.Users;
import com.esport.EsportTournament.model.rbac.AppRole;
import com.esport.EsportTournament.model.rbac.UserRole;
import com.esport.EsportTournament.repository.UsersRepo;
import com.esport.EsportTournament.repository.rbac.AppRoleRepository;
import com.esport.EsportTournament.repository.rbac.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RbacService {

    private static final String ROLE_PREFIX = "ROLE_";
    private static final String PERMISSION_PREFIX = "PERM_";

    private final AppRoleRepository appRoleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UsersRepo usersRepo;

    /**
     * Return granted authorities (roles + permissions) for the given user.
     */
    @Transactional(readOnly = true)
    public Set<String> getGrantedAuthorities(String firebaseUID) {
        Users user = usersRepo.findByFirebaseUserUID(firebaseUID)
                .orElseThrow(() -> new IllegalStateException("User not found: " + firebaseUID));

        List<UserRole> assignments = userRoleRepository.findByUser(user);
        if (assignments.isEmpty()) {
            log.debug("No RBAC assignments for {}, defaulting to USER", firebaseUID);
            return Set.of(ROLE_PREFIX + "USER");
        }

        Set<String> authorities = new HashSet<>();
        for (UserRole assignment : assignments) {
            AppRole role = assignment.getRole();
            authorities.add(ROLE_PREFIX + role.getCode());
            role.getPermissions().forEach(permission -> authorities.add(PERMISSION_PREFIX + permission.getCode()));
        }

        // Role hierarchy: add implied roles
        if (authorities.contains(ROLE_PREFIX + "SUPER_ADMIN")) {
            authorities.addAll(List.of(
                    ROLE_PREFIX + "ADMIN",
                    ROLE_PREFIX + "MANAGER",
                    ROLE_PREFIX + "OPERATOR",
                    ROLE_PREFIX + "USER"));
        } else if (authorities.contains(ROLE_PREFIX + "ADMIN")) {
            authorities.addAll(List.of(
                    ROLE_PREFIX + "MANAGER",
                    ROLE_PREFIX + "OPERATOR",
                    ROLE_PREFIX + "USER"));
        } else if (authorities.contains(ROLE_PREFIX + "MANAGER")
                || authorities.contains(ROLE_PREFIX + "OPERATOR")) {
            authorities.add(ROLE_PREFIX + "USER");
        }

        // 🔥 CRITICAL FIX: Fetch permissions for ALL roles (assigned + implied)
        // The previous loop only fetched permissions for DIRECTLY assigned roles.
        // Now that we have the full list of roles in 'authorities', we need to get
        // their permissions.
        Set<String> allRoles = authorities.stream()
                .filter(a -> a.startsWith(ROLE_PREFIX))
                .map(a -> a.substring(ROLE_PREFIX.length()))
                .collect(Collectors.toSet());

        List<AppRole> impliedAppRoles = appRoleRepository.findAll().stream()
                .filter(r -> allRoles.contains(r.getCode()))
                .toList();

        for (AppRole role : impliedAppRoles) {
            role.getPermissions().forEach(permission -> authorities.add(PERMISSION_PREFIX + permission.getCode()));
        }

        return authorities;
    }

    @Transactional
    public void assignRole(String firebaseUID, String roleCode) {
        Users user = usersRepo.findByFirebaseUserUID(firebaseUID)
                .orElseThrow(() -> new IllegalStateException("User not found: " + firebaseUID));
        AppRole role = appRoleRepository.findByCode(roleCode)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleCode));

        if (userRoleRepository.existsByUserAndRole_Code(user, roleCode)) {
            return;
        }
        userRoleRepository.save(UserRole.builder()
                .user(user)
                .role(role)
                .build());
    }

    @Transactional
    public void assignRoles(String firebaseUID, Collection<String> roleCodes) {
        Set<String> target = roleCodes.stream()
                .map(String::toUpperCase)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Users user = usersRepo.findByFirebaseUserUID(firebaseUID)
                .orElseThrow(() -> new IllegalStateException("User not found: " + firebaseUID));

        // Remove roles not in target
        List<UserRole> current = userRoleRepository.findByUser(user);
        current.stream()
                .filter(assignment -> !target.contains(assignment.getRole().getCode()))
                .forEach(userRoleRepository::delete);

        // Add missing
        target.forEach(roleCode -> assignRole(firebaseUID, roleCode));
    }

    @Transactional(readOnly = true)
    public List<String> getUserRoles(String firebaseUID) {
        return userRoleRepository.findByUser_FirebaseUserUID(firebaseUID).stream()
                .map(assignment -> assignment.getRole().getCode())
                .sorted()
                .toList();
    }

    @Transactional
    public void ensureDefaultRole(Users user) {
        if (userRoleRepository.findByUser(user).isEmpty()) {
            assignRole(user.getFirebaseUserUID(), "USER");
        }
    }

    @Transactional(readOnly = true)
    public List<AppRole> listRoles() {
        return appRoleRepository.findAll();
    }
}
