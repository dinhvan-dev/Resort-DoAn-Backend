package com.example.resort.aop.logging;

import com.example.resort.entity.AuditLog;
import com.example.resort.exception.AppException;
import com.example.resort.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditLoggingAspect {
    private final AuditLogService auditLogService;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        Object result = null;

        try {
            result = joinPoint.proceed();
            auditLogService.log(
                    auditable.action(),
                    auditable.entity(),
                    resolveExpression(auditable.entityId(), joinPoint, result, null),
                    resolveSuccessDetail(auditable, joinPoint, result),
                    AuditLog.AuditStatus.SUCCESS
            );
            return result;
        } catch (Throwable throwable) {
            auditLogService.log(
                    auditable.action(),
                    auditable.entity(),
                    resolveExpression(auditable.entityId(), joinPoint, result, throwable),
                    resolveFailureDetail(auditable, joinPoint, throwable),
                    AuditLog.AuditStatus.FAILED
            );
            throw throwable;
        }
    }

    private String resolveSuccessDetail(Auditable auditable, ProceedingJoinPoint joinPoint, Object result) {
        String detail = resolveExpression(auditable.detail(), joinPoint, result, null);
        if (!detail.isBlank()) {
            return detail;
        }

        return auditable.action() + " " + auditable.entity() + " succeeded";
    }

    private String resolveFailureDetail(Auditable auditable, ProceedingJoinPoint joinPoint, Throwable throwable) {
        String detail = resolveExpression(auditable.detail(), joinPoint, null, throwable);
        String reason = resolveErrorMessage(throwable);
        if (!detail.isBlank()) {
            return detail + " - Failed: " + reason;
        }

        return auditable.action() + " " + auditable.entity() + " failed - " + reason;
    }

    private String resolveExpression(
            String expression,
            ProceedingJoinPoint joinPoint,
            Object result,
            Throwable throwable
    ) {
        if (expression == null || expression.isBlank()) {
            return "";
        }

        if (!expression.contains("#")) {
            return expression;
        }

        try {
            StandardEvaluationContext context = buildContext(joinPoint, result, throwable);
            Object value = expressionParser.parseExpression(expression).getValue(context);
            return value == null ? "" : String.valueOf(value);
        } catch (Exception ignored) {
            return "";
        }
    }

    private StandardEvaluationContext buildContext(
            ProceedingJoinPoint joinPoint,
            Object result,
            Throwable throwable
    ) {
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
        context.setVariable("error", throwable);
        context.setVariable("methodName", method.getName());
        context.setVariable("args", Arrays.stream(args)
                .map(this::safeArgText)
                .collect(Collectors.joining(", ")));
        return context;
    }

    private String safeArgText(Object value) {
        if (value == null) {
            return "null";
        }

        String text = String.valueOf(value);
        return text.length() <= 160 ? text : text.substring(0, 157) + "...";
    }

    private String resolveErrorMessage(Throwable throwable) {
        if (throwable instanceof AppException appException) {
            return appException.getErrorCode().getMessage();
        }

        return throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage();
    }
}
