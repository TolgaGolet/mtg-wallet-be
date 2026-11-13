package com.mtg.mtgwalletbe.aspect;

import com.mtg.mtgwalletbe.annotation.Loggable;
import com.mtg.mtgwalletbe.entity.ServiceLog;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.StringJoiner;

import static com.mtg.mtgwalletbe.util.LogMasking.maskEmail;

@Aspect
@Component
// Outermost advice: wraps @Transactional, so commit-time failures (e.g. optimistic locking) are logged too
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class LoggableAspect {

    private final ServiceLogWriter serviceLogWriter;
    public static final int MAX_CHARS_REQUEST = 10000;
    public static final int MAX_CHARS_RESPONSE = 10000;
    public static final String REDACTED = "[redacted]";

    @Around("@annotation(loggable)")
    public Object logMethodCall(ProceedingJoinPoint joinPoint, Loggable loggable) throws Throwable {
        String className = joinPoint.getTarget().getClass().getName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        String status = "S";

        long startTime = System.currentTimeMillis();
        Object result = null;
        String response = null;

        try {
            result = joinPoint.proceed();
            if (result != null) {
                response = loggable.logResult() ? result.toString() : REDACTED;
            }
        } catch (Throwable ex) {
            status = "E";
            // Exception messages can echo arguments, so only keep details when args may be logged.
            response = loggable.logArgs()
                    ? ex.getMessage() + " " + ex.getCause() + " " + Arrays.toString(ex.getStackTrace())
                    : ex.getClass().getName();
            throw ex;
        } finally {
            long endTime = System.currentTimeMillis();
            String request = null;
            if (args != null) {
                request = loggable.logArgs() ? Arrays.toString(args) : redactArgs(args, loggable);
            }
            ServiceLog serviceLog = ServiceLog.builder()
                    .serviceName(className + "." + methodName)
                    .status(status)
                    .request(request != null ? request.substring(0, Math.min(request.length(), MAX_CHARS_REQUEST)) : null)
                    .response(response != null ? response.substring(0, Math.min(response.length(), MAX_CHARS_RESPONSE)) : null)
                    .startTime(startTime)
                    .endTime(endTime)
                    .executionTime(endTime - startTime)
                    .build();
            serviceLogWriter.save(serviceLog);
        }
        return result;
    }

    private static String redactArgs(Object[] args, Loggable loggable) {
        StringJoiner joiner = new StringJoiner(", ", "[", "]");
        for (int i = 0; i < args.length; i++) {
            if (contains(loggable.emailArgs(), i)) {
                joiner.add(args[i] == null ? "null" : maskEmail(args[i].toString()));
            } else if (contains(loggable.safeArgs(), i)) {
                joiner.add(String.valueOf(args[i]));
            } else {
                joiner.add(REDACTED);
            }
        }
        return joiner.toString();
    }

    private static boolean contains(int[] indexes, int index) {
        return Arrays.stream(indexes).anyMatch(i -> i == index);
    }
}
