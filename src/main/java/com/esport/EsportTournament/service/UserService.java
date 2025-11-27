package com.esport.EsportTournament.service;

import com.esport.EsportTournament.dto.UserDTO;
import com.esport.EsportTournament.exception.ResourceNotFoundException;
import com.esport.EsportTournament.model.Users;
import com.esport.EsportTournament.model.TournamentResult;
import com.esport.EsportTournament.repository.UsersRepo;
import com.esport.EsportTournament.repository.TournamentResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UsersRepo usersRepository;
    private final WalletService walletService;
    private final NotificationService notificationService;
    private final RbacService rbacService;
    private final TournamentResultRepository tournamentResultRepository;

    // Email validation pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    // Username validation pattern (alphanumeric, underscore, dash, 3-20 chars)
    private static final Pattern USERNAME_PATTERN = Pattern.compile(
            "^[A-Za-z0-9_-]{3,20}$");

    /**
     * Create new user with Firebase UID
     * Enhanced with better validation and duplicate handling
     */
    @Transactional
    public UserDTO createUser(UserDTO userDTO) {
        String firebaseUID = userDTO.getFirebaseUserUID();
        log.info("Creating new user with Firebase UID: {}", firebaseUID);
        System.out.println(userDTO.getUserName() + " " + userDTO.getEmail() + " " + userDTO.getFirebaseUserUID());

        // Enhanced validation
        validateUserInput(userDTO);

        // Check if user already exists
        Optional<Users> existingUser = usersRepository.findByFirebaseUserUID(firebaseUID);
        if (existingUser.isPresent()) {
            log.info("User already exists with Firebase UID: {}, returning existing user", firebaseUID);
            return mapToDTO(existingUser.get());
        }

        // Handle email uniqueness more gracefully
        if (usersRepository.existsByEmail(userDTO.getEmail())) {
            // Find existing user by email and update Firebase UID if needed
            Optional<Users> userByEmail = usersRepository.findByEmail(userDTO.getEmail());
            if (userByEmail.isPresent()) {
                Users existingEmailUser = userByEmail.get();
                if (existingEmailUser.getFirebaseUserUID() == null
                        || existingEmailUser.getFirebaseUserUID().isEmpty()) {
                    // Update existing user with Firebase UID
                    existingEmailUser.setFirebaseUserUID(firebaseUID);
                    Users updatedUser = usersRepository.save(existingEmailUser);
                    log.info("Updated existing user with Firebase UID: {}", firebaseUID);

                    // Ensure wallet exists
                    ensureWalletExists(firebaseUID);

                    return mapToDTO(updatedUser);
                } else {
                    throw new IllegalArgumentException(
                            "Email already exists with different Firebase account: " + userDTO.getEmail());
                }
            }
        }

        // Handle username uniqueness
        if (userDTO.getUserName() != null && !userDTO.getUserName().trim().isEmpty()) {
            if (usersRepository.existsByUserName(userDTO.getUserName())) {
                // Generate unique username suggestion
                String suggestedUsername = generateUniqueUsername(userDTO.getUserName());
                log.warn("Username {} already exists, suggested alternative: {}", userDTO.getUserName(),
                        suggestedUsername);
                throw new IllegalArgumentException("Username already exists. Suggested: " + suggestedUsername);
            }
        } else {
            // Generate username from email if not provided
            userDTO.setUserName(generateUsernameFromEmail(userDTO.getEmail()));
        }

        // Create new user
        Users user = new Users();
        user.setFirebaseUserUID(firebaseUID);
        user.setUserName(userDTO.getUserName().trim());
        user.setEmail(userDTO.getEmail().trim().toLowerCase());
        user.setRole(Users.UserRole.USER); // Default role
        user.setStatus(Users.UserStatus.ACTIVE); // Default status
        user.setCreatedAt(LocalDateTime.now());

        Users savedUser = usersRepository.save(user);
        rbacService.assignRole(savedUser.getFirebaseUserUID(), Users.UserRole.USER.name());
        log.info("User created successfully with ID: {}", savedUser.getId());

        // Auto-create wallet for new user
        ensureWalletExists(firebaseUID);

        // Send welcome notification (if notification service is available)
        try {
            sendWelcomeNotification(firebaseUID);
        } catch (Exception e) {
            log.warn("Failed to send welcome notification to user: {}", firebaseUID, e);
        }

        return mapToDTO(savedUser);
    }

    /**
     * Get user by Firebase UID with enhanced error handling
     */
    @Transactional(readOnly = true)
    public UserDTO getUserByFirebaseUID(String firebaseUID) {
        log.debug("Fetching user by Firebase UID: {}", firebaseUID);

        validateFirebaseUID(firebaseUID);

        Users user = usersRepository.findByFirebaseUserUID(firebaseUID)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with Firebase UID: " + firebaseUID));

        return mapToDTO(user);
    }

    /**
     * Get or create user by Firebase UID (for auto-registration)
     */
    @Transactional
    public UserDTO getOrCreateUserByFirebaseUID(String firebaseUID, String email, String displayName) {
        log.debug("Getting or creating user by Firebase UID: {}", firebaseUID);

        validateFirebaseUID(firebaseUID);

        Optional<Users> existingUser = usersRepository.findByFirebaseUserUID(firebaseUID);
        if (existingUser.isPresent()) {
            return mapToDTO(existingUser.get());
        }

        // Auto-create user
        UserDTO newUserDTO = new UserDTO(
                firebaseUID,
                displayName != null ? displayName : "User_" + firebaseUID.substring(0, 8),
                email != null ? email : "user_" + firebaseUID.substring(0, 8) + "@temp.com",
                Users.UserRole.USER,
                Users.UserStatus.ACTIVE,
                LocalDateTime.now());
        // newUserDTO.setFirebaseUserUID(firebaseUID);
        // newUserDTO.setEmail(email != null ? email : "user_" +
        // firebaseUID.substring(0, 8) + "@temp.com");
        // newUserDTO.setUserName(displayName != null ? displayName : "User_" +
        // firebaseUID.substring(0, 8));

        log.info("Auto-creating user for Firebase UID: {}", firebaseUID);
        return createUser(newUserDTO);
    }

    /**
     * Get all users with enhanced sorting and filtering
     */
    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        log.debug("Fetching all users");
        return usersRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Update user role with validation
     */
    @Transactional
    public UserDTO updateUserRole(String firebaseUID, Users.UserRole role) {
        log.info("Updating role for user {} to {}", firebaseUID, role);

        validateFirebaseUID(firebaseUID);
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }

        Users user = usersRepository.findByFirebaseUserUID(firebaseUID)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with Firebase UID: " + firebaseUID));

        Users.UserRole oldRole = user.getRole();
        user.setRole(role);
        Users updatedUser = usersRepository.save(user);
        rbacService.assignRoles(firebaseUID, List.of(role.name()));

        log.info("Role updated successfully for user: {} from {} to {}", firebaseUID, oldRole, role);

        // Send notification about role change
        try {
            sendRoleChangeNotification(firebaseUID, oldRole, role);
        } catch (Exception e) {
            log.warn("Failed to send role change notification", e);
        }

        return mapToDTO(updatedUser);
    }

    /**
     * Update user status with validation
     */
    @Transactional
    public UserDTO updateUserStatus(String firebaseUID, Users.UserStatus status) {
        log.info("Updating status for user {} to {}", firebaseUID, status);

        validateFirebaseUID(firebaseUID);
        if (status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }

        Users user = usersRepository.findByFirebaseUserUID(firebaseUID)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with Firebase UID: " + firebaseUID));

        Users.UserStatus oldStatus = user.getStatus();
        user.setStatus(status);
        Users updatedUser = usersRepository.save(user);

        log.info("Status updated successfully for user: {} from {} to {}", firebaseUID, oldStatus, status);

        // Send notification about status change (if not banned to avoid spam)
        if (status != Users.UserStatus.BANNED) {
            try {
                sendStatusChangeNotification(firebaseUID, oldStatus, status);
            } catch (Exception e) {
                log.warn("Failed to send status change notification", e);
            }
        }

        return mapToDTO(updatedUser);
    }

    /**
     * Check if user exists by Firebase UID
     */
    @Transactional(readOnly = true)
    public boolean userExists(String firebaseUID) {
        if (firebaseUID == null || firebaseUID.trim().isEmpty()) {
            return false;
        }
        return usersRepository.existsByFirebaseUserUID(firebaseUID);
    }

    /**
     * Get active users count
     */
    @Transactional(readOnly = true)
    public long getActiveUsersCount() {
        return usersRepository.countByStatus(Users.UserStatus.ACTIVE);
    }

    /**
     * Search users by username with case-insensitive partial matching
     */
    @Transactional(readOnly = true)
    public List<UserDTO> searchUsersByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return getAllUsers();
        }

        return usersRepository.findByUserNameContainingIgnoreCase(username.trim())
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get users by role
     */
    @Transactional(readOnly = true)
    public List<UserDTO> getUsersByRole(Users.UserRole role) {
        return usersRepository.findByRole(role)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get users by status
     */
    @Transactional(readOnly = true)
    public List<UserDTO> getUsersByStatus(Users.UserStatus status) {
        return usersRepository.findByStatus(status)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Update user profile (User can update their own username)
     */
    @Transactional
    public UserDTO updateUserProfile(String firebaseUID, String newUserName) {
        log.info("Updating profile for user: {}", firebaseUID);

        validateFirebaseUID(firebaseUID);

        Users user = usersRepository.findByFirebaseUserUID(firebaseUID)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with Firebase UID: " + firebaseUID));

        if (newUserName != null && !newUserName.trim().isEmpty()) {
            // Validate username format
            if (!USERNAME_PATTERN.matcher(newUserName.trim()).matches()) {
                throw new IllegalArgumentException(
                        "Username must be 3-20 characters long and contain only letters, numbers, underscores, and hyphens");
            }

            // Check username uniqueness (excluding current user)
            if (usersRepository.existsByUserName(newUserName.trim()) &&
                    !newUserName.trim().equals(user.getUserName())) {
                String suggestedUsername = generateUniqueUsername(newUserName.trim());
                throw new IllegalArgumentException("Username already exists. Suggested: " + suggestedUsername);
            }
            user.setUserName(newUserName.trim());
        }

        Users updatedUser = usersRepository.save(user);
        log.info("Profile updated successfully for user: {}", firebaseUID);

        return mapToDTO(updatedUser);
    }

    /**
     * Get comprehensive user statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUserStatistics() {
        List<Users> allUsers = usersRepository.findAll();

        long total = allUsers.size();
        long active = allUsers.stream().filter(u -> u.getStatus() == Users.UserStatus.ACTIVE).count();
        long banned = allUsers.stream().filter(u -> u.getStatus() == Users.UserStatus.BANNED).count();
        long inactive = allUsers.stream().filter(u -> u.getStatus() == Users.UserStatus.INACTIVE).count();
        long admins = allUsers.stream().filter(u -> u.getRole() == Users.UserRole.ADMIN).count();

        // Calculate registration trends
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);

        long recentRegistrations30d = allUsers.stream()
                .filter(u -> u.getCreatedAt().isAfter(thirtyDaysAgo))
                .count();

        long recentRegistrations7d = allUsers.stream()
                .filter(u -> u.getCreatedAt().isAfter(sevenDaysAgo))
                .count();

        long recentRegistrations1d = allUsers.stream()
                .filter(u -> u.getCreatedAt().isAfter(oneDayAgo))
                .count();

        return Map.of(
                "totalUsers", total,
                "activeUsers", active,
                "bannedUsers", banned,
                "inactiveUsers", inactive,
                "adminUsers", admins,
                "registrationTrends", Map.of(
                        "last30Days", recentRegistrations30d,
                        "last7Days", recentRegistrations7d,
                        "last24Hours", recentRegistrations1d),
                "userRoles", Map.of(
                        "users", total - admins,
                        "admins", admins),
                "userStatuses", Map.of(
                        "active", active,
                        "inactive", inactive,
                        "banned", banned));
    }

    /**
     * Ban user with reason
     */
    @Transactional
    public UserDTO banUser(String firebaseUID, String reason, String adminUID) {
        log.warn("Banning user {} by admin {} - Reason: {}", firebaseUID, adminUID, reason);

        Users user = usersRepository.findByFirebaseUserUID(firebaseUID)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with Firebase UID: " + firebaseUID));

        user.setStatus(Users.UserStatus.BANNED);
        Users bannedUser = usersRepository.save(user);

        // Log the ban for audit trail
        log.warn("AUDIT: User {} banned by admin {} - Reason: {}", firebaseUID, adminUID, reason);

        return mapToDTO(bannedUser);
    }

    /**
     * Unban user
     */
    @Transactional
    public UserDTO unbanUser(String firebaseUID, String adminUID) {
        log.info("Unbanning user {} by admin {}", firebaseUID, adminUID);

        Users user = usersRepository.findByFirebaseUserUID(firebaseUID)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with Firebase UID: " + firebaseUID));

        if (user.getStatus() != Users.UserStatus.BANNED) {
            throw new IllegalStateException("User is not currently banned");
        }

        user.setStatus(Users.UserStatus.ACTIVE);
        Users unbannedUser = usersRepository.save(user);

        // Send unban notification
        try {
            sendUnbanNotification(firebaseUID);
        } catch (Exception e) {
            log.warn("Failed to send unban notification", e);
        }

        log.info("AUDIT: User {} unbanned by admin {}", firebaseUID, adminUID);

        return mapToDTO(unbannedUser);
    }

    // ================= PRIVATE HELPER METHODS =================

    private void validateUserInput(UserDTO userDTO) {
        if (userDTO.getFirebaseUserUID() == null || userDTO.getFirebaseUserUID().trim().isEmpty()) {
            throw new IllegalArgumentException("Firebase UID cannot be null or empty");
        }

        if (userDTO.getEmail() == null || userDTO.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }

        if (!EMAIL_PATTERN.matcher(userDTO.getEmail().trim()).matches()) {
            throw new IllegalArgumentException("Invalid email format");
        }

        if (userDTO.getUserName() != null && !userDTO.getUserName().trim().isEmpty()) {
            if (!USERNAME_PATTERN.matcher(userDTO.getUserName().trim()).matches()) {
                throw new IllegalArgumentException(
                        "Username must be 3-20 characters long and contain only letters, numbers, underscores, and hyphens");
            }
        }
    }

    private void validateFirebaseUID(String firebaseUID) {
        if (firebaseUID == null || firebaseUID.trim().isEmpty()) {
            throw new IllegalArgumentException("Firebase UID cannot be null or empty");
        }
    }

    private String generateUniqueUsername(String baseUsername) {
        String username = baseUsername;
        int counter = 1;

        while (usersRepository.existsByUserName(username)) {
            username = baseUsername + "_" + counter;
            counter++;
            if (counter > 100) { // Prevent infinite loop
                username = baseUsername + "_" + System.currentTimeMillis();
                break;
            }
        }

        return username;
    }

    private String generateUsernameFromEmail(String email) {
        String baseUsername = email.split("@")[0];
        baseUsername = baseUsername.replaceAll("[^A-Za-z0-9_-]", "_");

        if (baseUsername.length() > 20) {
            baseUsername = baseUsername.substring(0, 20);
        } else if (baseUsername.length() < 3) {
            baseUsername = "user_" + baseUsername;
        }

        return generateUniqueUsername(baseUsername);
    }

    private void ensureWalletExists(String firebaseUID) {
        try {
            walletService.getWalletByFirebaseUID(firebaseUID);
        } catch (ResourceNotFoundException e) {
            try {
                walletService.createWalletForUser(firebaseUID);
                log.info("Wallet created for user: {}", firebaseUID);
            } catch (Exception walletError) {
                log.error("Failed to create wallet for user: {}", firebaseUID, walletError);
            }
        }
    }

    private void sendWelcomeNotification(String firebaseUID) {
        try {
            // This would need to be implemented based on your notification requirements
            log.debug("Sending welcome notification to user: {}", firebaseUID);
        } catch (Exception e) {
            log.warn("Failed to send welcome notification", e);
        }
    }

    /**
     * Complete user registration (for users who were auto-created)
     */
    @Transactional
    public UserDTO completeUserRegistration(String firebaseUID, String preferredUsername, String email) {
        log.info("Completing user registration for Firebase UID: {}", firebaseUID);

        validateFirebaseUID(firebaseUID);

        Users user = usersRepository.findByFirebaseUserUID(firebaseUID)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with Firebase UID: " + firebaseUID));

        boolean needsUpdate = false;

        // Update username if provided and different
        if (preferredUsername != null && !preferredUsername.trim().isEmpty() &&
                !preferredUsername.equals(user.getUserName())) {

            if (!USERNAME_PATTERN.matcher(preferredUsername.trim()).matches()) {
                throw new IllegalArgumentException(
                        "Username must be 3-20 characters long and contain only letters, numbers, underscores, and hyphens");
            }

            if (usersRepository.existsByUserName(preferredUsername.trim())) {
                String suggestedUsername = generateUniqueUsername(preferredUsername.trim());
                throw new IllegalArgumentException("Username already exists. Suggested: " + suggestedUsername);
            }

            user.setUserName(preferredUsername.trim());
            needsUpdate = true;
        }

        // Update email if provided and different
        if (email != null && !email.trim().isEmpty() && !email.equals(user.getEmail())) {
            if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
                throw new IllegalArgumentException("Invalid email format");
            }

            if (usersRepository.existsByEmail(email.trim().toLowerCase())) {
                throw new IllegalArgumentException("Email already exists: " + email);
            }

            user.setEmail(email.trim().toLowerCase());
            needsUpdate = true;
        }

        if (needsUpdate) {
            user = usersRepository.save(user);
            log.info("User registration completed for Firebase UID: {}", firebaseUID);
        }

        // Ensure wallet exists
        ensureWalletExists(firebaseUID);

        return mapToDTO(user);
    }

    private void sendRoleChangeNotification(String firebaseUID, Users.UserRole oldRole, Users.UserRole newRole) {
        try {
            String message = String.format("Your account role has been updated from %s to %s.", oldRole, newRole);
            // notificationService.sendNotificationToUser(firebaseUID, "Role Updated",
            // message, "SYSTEM");
            log.debug("Role change notification sent to user: {}", firebaseUID);
        } catch (Exception e) {
            log.warn("Failed to send role change notification", e);
        }
    }

    private void sendStatusChangeNotification(String firebaseUID, Users.UserStatus oldStatus,
            Users.UserStatus newStatus) {
        try {
            String message = String.format("Your account status has been updated from %s to %s.", oldStatus, newStatus);
            // notificationService.sendNotificationToUser(firebaseUID, "Account Status
            // Updated", message, "SYSTEM");
            log.debug("Status change notification sent to user: {}", firebaseUID);
        } catch (Exception e) {
            log.warn("Failed to send status change notification", e);
        }
    }

    private void sendUnbanNotification(String firebaseUID) {
        try {
            String message = "Your account has been reactivated. Welcome back to the platform!";
            // notificationService.sendNotificationToUser(firebaseUID, "Account
            // Reactivated", message, "SYSTEM");
            log.debug("Unban notification sent to user: {}", firebaseUID);
        } catch (Exception e) {
            log.warn("Failed to send unban notification", e);
        }
    }

    /**
     * Get user tournament history
     */
    @Transactional(readOnly = true)
    public List<TournamentResult> getUserHistory(String firebaseUID) {
        log.debug("Fetching history for user: {}", firebaseUID);
        validateFirebaseUID(firebaseUID);
        return tournamentResultRepository.findByFirebaseUserUIDOrderByCreatedAtDesc(firebaseUID);
    }

    private UserDTO mapToDTO(Users user) {
        return new UserDTO(
                user.getFirebaseUserUID(),
                user.getUserName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt());
    }
}