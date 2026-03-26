package com.Job.Posting.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<APIError> HandleUsernameNotFoundException(UsernameNotFoundException ex)
    {
        APIError apiError = new APIError("Username not found with username: "+ex.getMessage(), HttpStatus.NOT_FOUND);
        return new ResponseEntity<>(apiError,apiError.getStatusCode());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<APIError> handleBadCredentials(BadCredentialsException ex) {
        APIError apiError = new APIError("Invalid username or password", HttpStatus.UNAUTHORIZED);
        return new ResponseEntity<>(apiError, apiError.getStatusCode());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<APIError> handleIllegalArgument(IllegalArgumentException ex) {
        boolean isUnauthorized = ex.getMessage().contains("expired")
                || ex.getMessage().contains("reuse detected");
        HttpStatus status = isUnauthorized ? HttpStatus.UNAUTHORIZED : HttpStatus.BAD_REQUEST;
        APIError apiError = new APIError(ex.getMessage(), status);
        return new ResponseEntity<>(apiError, apiError.getStatusCode());
    }
}
