package com.servicedesk.service;

import com.servicedesk.model.Constants.*;
import com.servicedesk.model.ServiceRequest;
import com.servicedesk.model.Ticket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class SlaService {

    private final int criticalHours;
    private final int highHours;
    private final int mediumHours;
    private final int lowHours;

    private static final Map<String, Map<String, String>> PRIORITY_MATRIX = new HashMap<>();

    static {
        // (Impact, Urgency) -> Priority
        Map<String, String> companyWide = new HashMap<>();
        companyWide.put(UrgencyLevel.CRITICAL, TicketPriority.CRITICAL);
        companyWide.put(UrgencyLevel.HIGH, TicketPriority.CRITICAL);
        companyWide.put(UrgencyLevel.MEDIUM, TicketPriority.HIGH);
        companyWide.put(UrgencyLevel.LOW, TicketPriority.MEDIUM);
        PRIORITY_MATRIX.put(ImpactLevel.COMPANY_WIDE, companyWide);

        Map<String, String> multDept = new HashMap<>();
        multDept.put(UrgencyLevel.CRITICAL, TicketPriority.CRITICAL);
        multDept.put(UrgencyLevel.HIGH, TicketPriority.HIGH);
        multDept.put(UrgencyLevel.MEDIUM, TicketPriority.MEDIUM);
        multDept.put(UrgencyLevel.LOW, TicketPriority.LOW);
        PRIORITY_MATRIX.put(ImpactLevel.MULTIPLE_DEPARTMENTS, multDept);

        Map<String, String> dept = new HashMap<>();
        dept.put(UrgencyLevel.CRITICAL, TicketPriority.HIGH);
        dept.put(UrgencyLevel.HIGH, TicketPriority.HIGH);
        dept.put(UrgencyLevel.MEDIUM, TicketPriority.MEDIUM);
        dept.put(UrgencyLevel.LOW, TicketPriority.LOW);
        PRIORITY_MATRIX.put(ImpactLevel.DEPARTMENT, dept);

        Map<String, String> team = new HashMap<>();
        team.put(UrgencyLevel.CRITICAL, TicketPriority.HIGH);
        team.put(UrgencyLevel.HIGH, TicketPriority.MEDIUM);
        team.put(UrgencyLevel.MEDIUM, TicketPriority.MEDIUM);
        team.put(UrgencyLevel.LOW, TicketPriority.LOW);
        PRIORITY_MATRIX.put(ImpactLevel.TEAM, team);

        Map<String, String> individual = new HashMap<>();
        individual.put(UrgencyLevel.CRITICAL, TicketPriority.HIGH);
        individual.put(UrgencyLevel.HIGH, TicketPriority.MEDIUM);
        individual.put(UrgencyLevel.MEDIUM, TicketPriority.MEDIUM);
        individual.put(UrgencyLevel.LOW, TicketPriority.LOW);
        PRIORITY_MATRIX.put(ImpactLevel.INDIVIDUAL, individual);
    }

    public SlaService(
            @Value("${app.sla.critical-hours:4}") int criticalHours,
            @Value("${app.sla.high-hours:24}") int highHours,
            @Value("${app.sla.medium-hours:48}") int mediumHours,
            @Value("${app.sla.low-hours:72}") int lowHours) {
        this.criticalHours = criticalHours;
        this.highHours = highHours;
        this.mediumHours = mediumHours;
        this.lowHours = lowHours;
    }

    public String calculateSuggestedPriority(String impact, String urgency) {
        if (impact == null || urgency == null) {
            return TicketPriority.MEDIUM;
        }
        Map<String, String> row = PRIORITY_MATRIX.get(impact);
        if (row != null) {
            String p = row.get(urgency);
            if (p != null) return p;
        }
        return TicketPriority.MEDIUM;
    }

    public int getTargetHours(String priority) {
        if (priority == null) return mediumHours;
        return switch (priority) {
            case TicketPriority.CRITICAL -> criticalHours;
            case TicketPriority.HIGH -> highHours;
            case TicketPriority.MEDIUM -> mediumHours;
            case TicketPriority.LOW -> lowHours;
            default -> mediumHours;
        };
    }

    public String calculateSlaStatus(Ticket ticket) {
        if (ticket == null || ticket.getDueAt() == null) {
            return "Within SLA";
        }
        Instant now = Instant.now();
        Instant dueAt = ticket.getDueAt();

        // If ticket was resolved
        if (ticket.getResolvedAt() != null) {
            return ticket.getResolvedAt().isAfter(dueAt) ? "Breached" : "Within SLA";
        }

        // For open/unresolved tickets
        if (now.isAfter(dueAt)) {
            return "Breached";
        }

        int targetHours = getTargetHours(ticket.getPriority());
        long timeRemainingSeconds = Duration.between(now, dueAt).getSeconds();
        long atRiskThresholdSeconds = (long) Math.min(4 * 3600, targetHours * 3600 * 0.25);

        if (timeRemainingSeconds <= atRiskThresholdSeconds) {
            return "At Risk";
        }

        return "Within SLA";
    }

    public String calculateSrSlaStatus(ServiceRequest sr) {
        if (sr == null || sr.getDueAt() == null) {
            return "Within SLA";
        }
        Instant now = Instant.now();
        Instant dueAt = sr.getDueAt();

        // If fulfilled or closed
        if (sr.getFulfilledAt() != null) {
            return sr.getFulfilledAt().isAfter(dueAt) ? "Breached" : "Within SLA";
        }

        if (now.isAfter(dueAt)) {
            return "Breached";
        }

        int targetHours = getTargetHours(sr.getPriority());
        long timeRemainingSeconds = Duration.between(now, dueAt).getSeconds();
        long atRiskThresholdSeconds = (long) Math.min(4 * 3600, targetHours * 3600 * 0.25);

        if (timeRemainingSeconds <= atRiskThresholdSeconds) {
            return "At Risk";
        }

        return "Within SLA";
    }
}
