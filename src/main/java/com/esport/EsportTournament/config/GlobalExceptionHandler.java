package com.esport.EsportTournament.config;

import com.esport.EsportTournament.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle authentication failures (invalid or missing tokens)
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {

        log.warn("Authentication failed: {}", ex.getMessage());

        Map<String, Object> errorResponse = createErrorResponse(
                "AUTHENTICATION_FAILED",
                "Authentication required. Please provide a valid token.",
                HttpStatus.UNAUTHORIZED,
                request.getDescription(false)
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    /**
     * Handle authorization failures (insufficient permissions)
     */
    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(
            Exception ex, WebRequest request) {

        log.warn("Access denied: {}", ex.getMessage());

        // Check if user might be unregistered
        String path = request.getDescription(false);
        Map<String, Object> errorResponse;

        if (path.contains("/api/users/me") && "POST".equals(getHttpMethod(request))) {
            // This is likely an unregistered user trying to register
            errorResponse = createErrorResponse(
                    "REGISTRATION_REQUIRED",
                    "Please complete your registration first.",
                    HttpStatus.FORBIDDEN,
                    path
            );
            errorResponse.put("action", "COMPLETE_REGISTRATION");
            errorResponse.put("registrationEndpoint", "/api/users/me");
        } else {
            errorResponse = createErrorResponse(
                    "ACCESS_DENIED",
                    "You don't have permission to access this resource. You may need to complete registration or have insufficient privileges.",
                    HttpStatus.FORBIDDEN,
                    path
            );
            errorResponse.put("possibleCauses", java.util.List.of(
                    "Account not fully registered",
                    "Insufficient user role permissions",
                    "Account status restrictions"
            ));
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Handle resource not found exceptions
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {

        log.warn("Resource not found: {}", ex.getMessage());

        Map<String, Object> errorResponse = createErrorResponse(
                "RESOURCE_NOT_FOUND",
                ex.getMessage(),
                HttpStatus.NOT_FOUND,
                request.getDescription(false)
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handle validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {

        log.warn("Validation failed: {}", ex.getMessage());

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            if (error instanceof FieldError) {
                FieldError fieldError = (FieldError) error;
                fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
            } else {
                fieldErrors.put("global", error.getDefaultMessage());
            }
        });

        Map<String, Object> errorResponse = createErrorResponse(
                "VALIDATION_FAILED",
                "Input validation failed",
                HttpStatus.BAD_REQUEST,
                request.getDescription(false)
        );
        errorResponse.put("fieldErrors", fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle constraint violations
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {

        log.warn("Constraint violation: {}", ex.getMessage());

        Map<String, Object> errorResponse = createErrorResponse(
                "CONSTRAINT_VIOLATION",
                "Input constraints were violated: " + ex.getMessage(),
                HttpStatus.BAD_REQUEST,
                request.getDescription(false)
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {

        log.warn("Illegal argument: {}", ex.getMessage());

        Map<String, Object> errorResponse = createErrorResponse(
                "INVALID_INPUT",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST,
                request.getDescription(false)
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle illegal state exceptions
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(
            IllegalStateException ex, WebRequest request) {

        log.warn("Illegal state: {}", ex.getMessage());

        Map<String, Object> errorResponse = createErrorResponse(
                "INVALID_OPERATION",
                ex.getMessage(),
                HttpStatus.CONFLICT,
                request.getDescription(false)
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handle data integrity violations (like unique constraint violations)
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, WebRequest request) {

        log.warn("Data integrity violation: {}", ex.getMessage());

        String userMessage = "Data integrity violation occurred";
        if (ex.getMessage() != null) {
            if (ex.getMessage().contains("email")) {
                userMessage = "Email address already exists";
            } else if (ex.getMessage().contains("username")) {
                userMessage = "Username already exists";
            } else if (ex.getMessage().contains("firebase")) {
                userMessage = "Account already exists";
            }
        }

        Map<String, Object> errorResponse = createErrorResponse(
                "DATA_INTEGRITY_VIOLATION",
                userMessage,
                HttpStatus.CONFLICT,
                request.getDescription(false)
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handle method argument type mismatch
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex, WebRequest request) {

        log.warn("Method argument type mismatch: {}", ex.getMessage());

        String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s",
                ex.getValue(), ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

        Map<String, Object> errorResponse = createErrorResponse(
                "INVALID_PARAMETER_TYPE",
                message,
                HttpStatus.BAD_REQUEST,
                request.getDescription(false)
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle runtime exceptions
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(
            RuntimeException ex, WebRequest request) {

        log.error("Runtime exception occurred", ex);

        Map<String, Object> errorResponse = createErrorResponse(
                "RUNTIME_ERROR",
                "An unexpected error occurred. Please try again later.",
                HttpStatus.INTERNAL_SERVER_ERROR,
                request.getDescription(false)
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGlobalException(
            Exception ex, WebRequest request) {

        log.error("Unexpected exception occurred", ex);

        Map<String, Object> errorResponse = createErrorResponse(
                "INTERNAL_SERVER_ERROR",
                "An internal server error occurred. Please contact support if the problem persists.",
                HttpStatus.INTERNAL_SERVER_ERROR,
                request.getDescription(false)
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Create standardized error response
     */
    private Map<String, Object> createErrorResponse(String errorCode, String message,
                                                    HttpStatus status, String path) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", status.value());
        errorResponse.put("error", status.getReasonPhrase());
        errorResponse.put("errorCode", errorCode);
        errorResponse.put("message", message);
        errorResponse.put("path", path.replace("uri=", ""));

        return errorResponse;
    }

    /**
     * Extract HTTP method from WebRequest (utility method)
     */
    private String getHttpMethod(WebRequest request) {
        try {
            // This is a simplified way to get HTTP method
            // In a real implementation, you might need to cast to HttpServletRequest
            return "UNKNOWN";
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
}