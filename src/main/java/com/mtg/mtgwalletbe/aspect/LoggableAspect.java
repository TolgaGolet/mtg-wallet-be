package com.mtg.mtgwalletbe.aspect;

import com.mtg.mtgwalletbe.entity.ServiceLog;
import com.mtg.mtgwalletbe.repository.ServiceLogRepository;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@RequiredArgsConstructor
public class LoggableAspect {

    private final ServiceLogRepository serviceLogRepository;
    private static final int MAX_CHARS_REQUEST = 1000;
    private static final int MAX_CHARS_RESPONSE = 1000;

    @Pointcut("@annotation(com.mtg.mtgwalletbe.annotation.Loggable)")
    public void loggableMethods() {
    }

    @Around("loggableMethods()")
    public Object logMethodCall(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        String status = "S";

        long startTime = System.currentTimeMillis();
        Object result = null;

        try {
            result = joinPoint.proceed();
        } catch (Throwable ex) {
            status = "E";
            result = ex.getMessage() + Arrays.toString(ex.getStackTrace());
            throw ex;
        } finally {
            long endTime = System.currentTimeMillis();
            String request = args != null ? Arrays.toString(args) : null;
            String response = result != null ? result.toString() : null;
            ServiceLog serviceLog = ServiceLog.builder()
                    .serviceName(className + "." + methodName)
                    .status(status)
                    .request(request != null ? request.substring(0, Math.min(request.length(), MAX_CHARS_REQUEST)) : null)
                    .response(response != null ? response.substring(0, Math.min(response.length(), MAX_CHARS_RESPONSE)) : null)
                    .startTime(startTime)
                    .endTime(endTime)
                    .executionTime(endTime - startTime)
                    .build();
            serviceLogRepository.save(serviceLog);
        }
        return result;
    }
}

