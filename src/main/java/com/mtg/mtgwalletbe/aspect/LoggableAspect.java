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

    @Pointcut("@annotation(com.mtg.mtgwalletbe.annotation.Loggable)")
    public void loggableMethods() {
    }

    @Around("loggableMethods()")
    public Object logMethodCall(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long endTime = System.currentTimeMillis();

        ServiceLog serviceLog = ServiceLog.builder()
                .serviceName(className + "." + methodName)
                .request(args != null ? Arrays.toString(args) : null)
                .response(result != null ? result.toString() : null)
                .startTime(startTime)
                .endTime(endTime)
                .executionTime(endTime - startTime)
                .build();
        serviceLogRepository.save(serviceLog);

        return result;
    }
}

