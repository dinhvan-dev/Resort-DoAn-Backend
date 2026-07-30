package com.example.resort.aop.event;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class DomainEvent {
    @Builder.Default
    private final String eventId = UUID.randomUUID().toString();

    private final String type;
    private final String aggregate;
    private final String aggregateId;
    private final Object payload;
    private final String username;
    private final String sourceMethod;

    @Builder.Default
    private final LocalDateTime occurredAt = LocalDateTime.now();
}
