package com.Job.Posting.error;

import com.Job.Posting.exception.AccessDeniedException;
import com.Job.Posting.exception.DuplicateResourceException;
import com.Job.Posting.exception.ResourceNotFoundException;
import com.Job.Posting.exception.UserNotVerifiedException;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<APIError> handleUsernameNotFoundException(UsernameNotFoundException ex) {
        APIError apiError = new APIError("User not found: " + ex.getMessage(), HttpStatus.NOT_FOUND);
        return new ResponseEntity<>(apiError, apiError.getStatusCode());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<APIError> handleResourceNotFoundException(ResourceNotFoundException ex) {
        APIError apiError = new APIError(ex.getMessage(), HttpStatus.NOT_FOUND);
        return new ResponseEntity<>(apiError, apiError.getStatusCode());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<APIError> handleDuplicateResourceException(DuplicateResourceException ex) {
        APIError apiError = new APIError(ex.getMessage(), HttpStatus.CONFLICT);
        return new ResponseEntity<>(apiError, apiError.getStatusCode());
    }

    // Ownership / permission violations — returns 403 not 401
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<APIError> handleAccessDeniedException(AccessDeniedException ex) {
        APIError apiError = new APIError(ex.getMessage(), HttpStatus.FORBIDDEN);
        return new ResponseEntity<>(apiError, apiError.getStatusCode());
    }

    @ExceptionHandler(UserNotVerifiedException.class)
    public ResponseEntity<APIError> handleUserNotVerifiedException(UserNotVerifiedException ex) {
        APIError apiError = new APIError(ex.getMessage(), HttpStatus.FORBIDDEN);
        return new ResponseEntity<>(apiError, apiError.getStatusCode());
    }


    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<APIError> handleBadCredentials(BadCredentialsException ex) {
        APIError apiError = new APIError("Invalid username or password", HttpStatus.UNAUTHORIZED);
        return new ResponseEntity<>(apiError, apiError.getStatusCode());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<APIError> handleIllegalArgument(IllegalArgumentException ex) {
        boolean isUnauthorized = ex.getMessage().contains("expired") || ex.getMessage().contains("reuse detected");
        HttpStatus status = isUnauthorized ? HttpStatus.UNAUTHORIZED : HttpStatus.BAD_REQUEST;
        APIError apiError = new APIError(ex.getMessage(), status);
        return new ResponseEntity<>(apiError, apiError.getStatusCode());
    }

    // JWT specific
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<APIError> handleExpiredJwt(ExpiredJwtException ex) {
        APIError apiError = new APIError("JWT token has expired. Please login again.", HttpStatus.UNAUTHORIZED);
        return new ResponseEntity<>(apiError, apiError.getStatusCode());
    }

    @ExceptionHandler({MalformedJwtException.class, SignatureException.class})
    public ResponseEntity<APIError> handleInvalidJwt(Exception ex) {
        APIError apiError = new APIError("Invalid JWT token.", HttpStatus.UNAUTHORIZED);
        return new ResponseEntity<>(apiError, apiError.getStatusCode());
    }

    // Validation
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<APIError> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        APIError apiError = new APIError("Validation failed: " + fieldErrors, HttpStatus.BAD_REQUEST);
        return new ResponseEntity<>(apiError, apiError.getStatusCode());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<APIError> handleConstraintViolation(ConstraintViolationException ex) {
        APIError apiError = new APIError("Constraint violation: " + ex.getMessage(), HttpStatus.BAD_REQUEST);
        return new ResponseEntity<>(apiError, apiError.getStatusCode());
    }

    // DB
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<APIError> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        APIError apiError = new APIError("Data integrity violation. A record with this value already exists.", HttpStatus.CONFLICT);
        return new ResponseEntity<>(apiError, apiError.getStatusCode());
    }

    // Redis
    @ExceptionHandler(RedisConnectionFailureException.class)
    public ResponseEntity<APIError> handleRedisConnectionFailure(RedisConnectionFailureException ex) {
        log.error("Redis connection failed: {}", ex.getMessage());
        APIError apiError = new APIError("Cache service temporarily unavailable. Please try again.", HttpStatus.SERVICE_UNAVAILABLE);
        return new ResponseEntity<>(apiError, apiError.getStatusCode());
    }

    // Fallback
    @ExceptionHandler(Exception.class)
    public ResponseEntity<APIError> handleGenericException(Exception ex) {
        log.error("Unexpected error: ", ex);
        APIError apiError = new APIError("An unexpected error occurred. Please try again later.", HttpStatus.INTERNAL_SERVER_ERROR);
        return new ResponseEntity<>(apiError, apiError.getStatusCode());
    }
}