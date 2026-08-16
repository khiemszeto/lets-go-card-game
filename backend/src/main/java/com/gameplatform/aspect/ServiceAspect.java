package com.gameplatform.aspect;

import com.gameplatform.annotation.TrackExecutionTime;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class ServiceAspect {

    @Around("@annotation(trackExecutionTime)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint,
                                   TrackExecutionTime trackExecutionTime) throws Throwable {
        long startTime = System.currentTimeMillis();

        try {
             return joinPoint.proceed();
         } finally {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            String operation = trackExecutionTime.operation();

            if (operation.isEmpty()) {
                operation = joinPoint.getSignature().getName();
            }

            long warningThreshold = trackExecutionTime.warnAfter();

            if (duration > warningThreshold) {
                System.out.println("SLOW Operation " + operation + " : took " + duration + " ms");
            } else {
                System.out.println("Operation " + operation + " : took " + duration + " ms");
            }
         }
    }
}
