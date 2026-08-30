package com.ticketmesh.exception;

/**
 * Raised when a request conflicts with the current state of a resource, such as
 * attempting to book a sold-out journey, paying an already-paid booking, or
 * registering a username that already exists.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
