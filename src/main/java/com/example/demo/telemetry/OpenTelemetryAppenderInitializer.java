package com.example.demo.telemetry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public final class OpenTelemetryAppenderInitializer
        implements InitializingBean {

    private final OpenTelemetry openTelemetry;

    public OpenTelemetryAppenderInitializer(
            OpenTelemetry openTelemetry
    ) {
        this.openTelemetry = Objects.requireNonNull(
                openTelemetry,
                "openTelemetry must not be null"
        );
    }

    @Override
    public void afterPropertiesSet() {
        OpenTelemetryAppender.install(openTelemetry);
    }
}