package com.example.resort.aop.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PublishDomainEvent {
    String type();

    String aggregate();

    String aggregateId() default "";

    String payload() default "#result";

    String condition() default "";
}
