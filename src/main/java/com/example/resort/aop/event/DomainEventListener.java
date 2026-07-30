package com.example.resort.aop.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DomainEventListener {
    @EventListener
    public void handle(DomainEvent event) {
        log.info(
                "Domain event published - type={}, aggregate={}, aggregateId={}, username={}, source={}",
                event.getType(),
                event.getAggregate(),
                event.getAggregateId(),
                event.getUsername(),
                event.getSourceMethod()
        );
    }
}
