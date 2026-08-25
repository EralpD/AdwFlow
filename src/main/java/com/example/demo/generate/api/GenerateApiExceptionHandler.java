package com.example.demo.generate.api;

import com.example.demo.agent.core.AgentExecutionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.time.Instant;

@RestControllerAdvice
public final class GenerateApiExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableRequest(
            HttpMessageNotReadableException exception
    ) {
        return ResponseEntity.badRequest().body(
                new ApiError(
                        "INVALID_JSON",
                        "Request body is missing or contains invalid JSON.",
                        null,
                        null,
                        Instant.now()
                )
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadRequest(
            IllegalArgumentException exception
    ) {
        return ResponseEntity.badRequest().body(
                new ApiError(
                        "INVALID_REQUEST",
                        exception.getMessage(),
                        null,
                        null,
                        Instant.now()
                )
        );
    }

    @ExceptionHandler(AgentExecutionException.class)
    public ResponseEntity<ApiError> handleAgentFailure(
            AgentExecutionException exception
    ) {
        Throwable cause = exception.getCause();
        String detail = cause == null
                ? exception.getMessage()
                : exception.getMessage()
                        + ": "
                        + cause.getMessage();

        return ResponseEntity.status(
                HttpStatus.BAD_GATEWAY
        ).body(
                new ApiError(
                        "AGENT_EXECUTION_FAILED",
                        detail,
                        exception.workflowId(),
                        exception.generationId(),
                        Instant.now()
                )
        );
    }
}
