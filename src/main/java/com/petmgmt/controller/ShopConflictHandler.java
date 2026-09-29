package com.petmgmt.controller;

import com.petmgmt.dto.ApiResponse;
import com.petmgmt.service.ShopConflictException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ShopConflictHandler {
    @ExceptionHandler(ShopConflictException.class)
    public ResponseEntity<ApiResponse<Void>> conflict(ShopConflictException exception) {
        return ResponseEntity.status(409).body(ApiResponse.error(exception.getMessage()));
    }
}
