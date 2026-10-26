package com.oussema.cloud.exception;

import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Log4j2
public class GlobalExceptionHandler {


    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Map<String, String>> handleMissingServletRequestPartException(
            MissingServletRequestPartException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    // Exception handler for ConflictException
    @ExceptionHandler(StorageException.class)
    public ResponseEntity<Object> handleConflictException(StorageException ex) {
        log.error("Conflict: {}", ex.getMessage(), ex);
        return new ResponseEntity<>(new ExceptionResponse(ex.getTitleKey(), ex.getMessageKey(), ex), HttpStatus.BAD_REQUEST);
    }
    
}
