package com.quickbite.delivery.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;


@Slf4j
@Aspect
@Component
public class LoggingAspect {

    // Adding Aspect logs for all controller methods to track incoming API requests
    @Pointcut("execution(* com.quickbite.delivery.controller..*(..))")
    public void controllerMethods() {
    }

    // Adding Aspect logs for all service methods to track business logic execution
    @Pointcut("execution(* com.quickbite.delivery.service..*(..))")
    public void serviceMethods() {
    }

    // Adding Aspect logs for all repository methods to track database operations
    @Pointcut("execution(* com.quickbite.delivery.repository..*(..))")
    public void repositoryMethods() {
    }

    // Actual logging logic around all controllers, services, and repository methods
    @Around("controllerMethods() || serviceMethods() || repositoryMethods()")
    public Object logMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {

        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();

        long startTime = System.currentTimeMillis();

        log.info("Started: {}.{}()", className, methodName);

        try {
            Object result = joinPoint.proceed();

            long endTime = System.currentTimeMillis();
            log.info("Completed: {}.{}() in {} ms",
                    className,
                    methodName,
                    endTime - startTime);

            return result;
        } catch(Exception exception) {

            long endTime = System.currentTimeMillis();
            log.error("Failed: {}.{}() in {} ms. Error: {}",
                    className,
                    methodName,
                    endTime - startTime,
                    exception.getMessage(),
                    exception);

            throw exception;
        }
    }
}