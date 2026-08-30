package com.sareekart.exception;

/** 503 — an optional subsystem (e.g. Razorpay) is disabled or unreachable. */
public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message) {
        super(message);
    }
}