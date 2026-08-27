package com.example.demo.generate.api;

import com.example.demo.agent.core.AgentExecutionException;
import com.example.demo.works.WorkStorageException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.time.Instant;

@RestControllerAdvice(basePackageClasses = GenerateAdvertisementApiController.class)
public final class GenerateApiExceptionHandler {

    @ExceptionHandler({WorkStorageException.class, DataAccessException.class})
    public ResponseEntity<ApiError> handleWorkStorageFailure(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ApiError(
                "WORK_SAVE_FAILED", "The work could not be completed and saved. Please try again later.",
                null, null, Instant.now()));
    }

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
        return ResponseEntity.status(
                HttpStatus.BAD_GATEWAY
        ).body(
                new ApiError(
                        "AGENT_EXECUTION_FAILED",
                        "The generation service could not complete this request. Please try again later.",
                        exception.workflowId(),
                        exception.generationId(),
                        Instant.now()
                )
        );
    }
}
