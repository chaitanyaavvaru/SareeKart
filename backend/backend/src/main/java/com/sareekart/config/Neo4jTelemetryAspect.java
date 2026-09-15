package com.sareekart.config;

import com.sareekart.dto.response.customer.CustomerEventResponse;
import com.sareekart.service.Neo4jEventProjector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Phase 7: Non-invasive Telemetry Projection Aspect.
 * 
 * Intercepts customer behavior events recorded in Phase 6 without modifying
 * frozen Phase 6 service code.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class Neo4jTelemetryAspect {

    private final Neo4jEventProjector eventProjector;

    @AfterReturning(
            pointcut = "execution(* com.sareekart.service.CustomerBehaviorService.recordEvent(..))",
            returning = "response"
    )
    public void afterRecordEvent(CustomerEventResponse response) {
        if (response != null) {
            try {
                eventProjector.projectEventAsync(response);
            } catch (Exception e) {
                log.warn("Non-blocking error dispatching telemetry event to Neo4j projector: {}", e.getMessage());
            }
        }
    }

    @AfterReturning(
            pointcut = "execution(* com.sareekart.service.CustomerBehaviorService.recordBatch(..))",
            returning = "responses"
    )
    public void afterRecordBatch(List<CustomerEventResponse> responses) {
        if (responses != null && !responses.isEmpty()) {
            for (CustomerEventResponse resp : responses) {
                try {
                    eventProjector.projectEventAsync(resp);
                } catch (Exception e) {
                    log.warn("Non-blocking error dispatching batch telemetry event to Neo4j projector: {}", e.getMessage());
                }
            }
        }
    }

    @AfterReturning(
            pointcut = "execution(* com.sareekart.service.CustomerBehaviorService.identifySession(..))",
            returning = "linkedCount"
    )
    public void afterIdentifySession(JoinPoint joinPoint, int linkedCount) {
        if (linkedCount > 0) {
            try {
                Object[] args = joinPoint.getArgs();
                if (args.length >= 2 && args[0] instanceof String sessionId && args[1] instanceof Long userId) {
                    eventProjector.projectSessionIdentifiedAsync(sessionId, userId);
                }
            } catch (Exception e) {
                log.warn("Non-blocking error projecting identified session to Neo4j: {}", e.getMessage());
            }
        }
    }
}
