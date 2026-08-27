package com.example.demo.works;

public class WorkStorageException extends RuntimeException {
    public WorkStorageException(String message, Throwable cause) { super(message, cause); }
    public WorkStorageException(String message) { super(message); }
}
