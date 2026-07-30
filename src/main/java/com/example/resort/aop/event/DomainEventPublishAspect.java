package com.example.resort.aop.event;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
public class DomainEventPublishAspect {
    private final ApplicationEventPublisher eventPublisher;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(publishDomainEvent)")
    public Object publishAfterSuccess(
            ProceedingJoinPoint joinPoint,
            PublishDomainEvent publishDomainEvent
    ) throws Throwable {
        Object result = joinPoint.proceed();

        if (matchesCondition(publishDomainEvent.condition(), joinPoint, result)) {
            publishAfterCommit(toDomainEvent(publishDomainEvent, joinPoint, result));
        }

        return result;
    }

    private DomainEvent toDomainEvent(
            PublishDomainEvent publishDomainEvent,
            ProceedingJoinPoint joinPoint,
            Object result
    ) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        return DomainEvent.builder()
                .type(publishDomainEvent.type())
                .aggregate(publishDomainEvent.aggregate())
                .aggregateId(resolveString(publishDomainEvent.aggregateId(), joinPoint, result))
                .payload(resolvePayload(publishDomainEvent.payload(), joinPoint, result))
                .username(currentUsername())
                .sourceMethod(method.getDeclaringClass().getSimpleName() + "." + method.getName())
                .build();
    }

    private void publishAfterCommit(DomainEvent event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            eventPublisher.publishEvent(event);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventPublisher.publishEvent(event);
            }
        });
    }

    private boolean matchesCondition(String condition, ProceedingJoinPoint joinPoint, Object result) {
        if (condition == null || condition.isBlank()) {
            return true;
        }

        Object value = resolveExpression(condition, joinPoint, result);
        return Boolean.TRUE.equals(value);
    }

    private String resolveString(String expression, ProceedingJoinPoint joinPoint, Object result) {
        Object value = resolveExpression(expression, joinPoint, result);
        return value == null ? "" : String.valueOf(value);
    }

    private Object resolvePayload(String expression, ProceedingJoinPoint joinPoint, Object result) {
        if (expression == null || expression.isBlank()) {
            return null;
        }

        return resolveExpression(expression, joinPoint, result);
    }

    private Object resolveExpression(String expression, ProceedingJoinPoint joinPoint, Object result) {
        if (expression == null || expression.isBlank()) {
            return null;
        }

        if (!expression.contains("#")) {
            return expression;
        }

        try {
            return expressionParser.parseExpression(expression).getValue(buildContext(joinPoint, result));
        } catch (Exception ignored) {
            return null;
        }
    }

    private StandardEvaluationContext buildContext(ProceedingJoinPoint joinPoint, Object result) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        Object[] args = joinPoint.getArgs();
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);

        for (int index = 0; index < args.length; index++) {
            context.setVariable("p" + index, args[index]);
            context.setVariable("a" + index, args[index]);
            if (parameterNames != null && index < parameterNames.length) {
                context.setVariable(parameterNames[index], args[index]);
            }
        }

        context.setVariable("result", result);
        return context;
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "anonymous";
        }

        return authentication.getName();
    }
}
