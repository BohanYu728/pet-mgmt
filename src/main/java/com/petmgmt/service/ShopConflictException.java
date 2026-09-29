package com.petmgmt.service;

/** A concurrent stock change aborts the entire checkout or cancellation transaction. */
public class ShopConflictException extends RuntimeException {
    public ShopConflictException(String message) { super(message); }
}
