package com.servicedesk;

import com.servicedesk.model.Constants.ImpactLevel;
import com.servicedesk.model.Constants.TicketPriority;
import com.servicedesk.model.Constants.UrgencyLevel;
import com.servicedesk.model.Ticket;
import com.servicedesk.service.SlaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SlaServiceTest {

    private SlaService slaService;

    @BeforeEach
    void setUp() {
        slaService = new SlaService(4, 24, 48, 72);
    }

    @Test
    @DisplayName("Should derive correct priority from ITIL Impact x Urgency matrix")
    void testPriorityMatrix() {
        // Company-wide
        assertEquals(TicketPriority.CRITICAL, slaService.calculateSuggestedPriority(ImpactLevel.COMPANY_WIDE, UrgencyLevel.CRITICAL));
        assertEquals(TicketPriority.CRITICAL, slaService.calculateSuggestedPriority(ImpactLevel.COMPANY_WIDE, UrgencyLevel.HIGH));
        assertEquals(TicketPriority.HIGH, slaService.calculateSuggestedPriority(ImpactLevel.COMPANY_WIDE, UrgencyLevel.MEDIUM));
        assertEquals(TicketPriority.MEDIUM, slaService.calculateSuggestedPriority(ImpactLevel.COMPANY_WIDE, UrgencyLevel.LOW));

        // Multiple Departments
        assertEquals(TicketPriority.CRITICAL, slaService.calculateSuggestedPriority(ImpactLevel.MULTIPLE_DEPARTMENTS, UrgencyLevel.CRITICAL));
        assertEquals(TicketPriority.HIGH, slaService.calculateSuggestedPriority(ImpactLevel.MULTIPLE_DEPARTMENTS, UrgencyLevel.HIGH));
        assertEquals(TicketPriority.MEDIUM, slaService.calculateSuggestedPriority(ImpactLevel.MULTIPLE_DEPARTMENTS, UrgencyLevel.MEDIUM));
        assertEquals(TicketPriority.LOW, slaService.calculateSuggestedPriority(ImpactLevel.MULTIPLE_DEPARTMENTS, UrgencyLevel.LOW));

        // Department
        assertEquals(TicketPriority.HIGH, slaService.calculateSuggestedPriority(ImpactLevel.DEPARTMENT, UrgencyLevel.CRITICAL));
        assertEquals(TicketPriority.HIGH, slaService.calculateSuggestedPriority(ImpactLevel.DEPARTMENT, UrgencyLevel.HIGH));
        assertEquals(TicketPriority.MEDIUM, slaService.calculateSuggestedPriority(ImpactLevel.DEPARTMENT, UrgencyLevel.MEDIUM));
        assertEquals(TicketPriority.LOW, slaService.calculateSuggestedPriority(ImpactLevel.DEPARTMENT, UrgencyLevel.LOW));

        // Individual
        assertEquals(TicketPriority.HIGH, slaService.calculateSuggestedPriority(ImpactLevel.INDIVIDUAL, UrgencyLevel.CRITICAL));
        assertEquals(TicketPriority.MEDIUM, slaService.calculateSuggestedPriority(ImpactLevel.INDIVIDUAL, UrgencyLevel.HIGH));
        assertEquals(TicketPriority.MEDIUM, slaService.calculateSuggestedPriority(ImpactLevel.INDIVIDUAL, UrgencyLevel.MEDIUM));
        assertEquals(TicketPriority.LOW, slaService.calculateSuggestedPriority(ImpactLevel.INDIVIDUAL, UrgencyLevel.LOW));
    }

    @Test
    @DisplayName("Should return configured target hours for each priority")
    void testTargetHours() {
        assertEquals(4, slaService.getTargetHours(TicketPriority.CRITICAL));
        assertEquals(24, slaService.getTargetHours(TicketPriority.HIGH));
        assertEquals(48, slaService.getTargetHours(TicketPriority.MEDIUM));
        assertEquals(72, slaService.getTargetHours(TicketPriority.LOW));
    }

    @Test
    @DisplayName("Should evaluate SLA status: Within SLA, At Risk, Breached")
    void testSlaStatusEvaluation() {
        Instant now = Instant.now();

        // 1. Unresolved, due in 20 hours -> Within SLA
        Ticket t1 = new Ticket();
        t1.setPriority(TicketPriority.HIGH);
        t1.setDueAt(now.plus(20, ChronoUnit.HOURS));
        assertEquals("Within SLA", slaService.calculateSlaStatus(t1));

        // 2. Unresolved, due in 1 hour -> At Risk (threshold is min(4h, 24h * 0.25 = 6h) = 4h)
        Ticket t2 = new Ticket();
        t2.setPriority(TicketPriority.HIGH);
        t2.setDueAt(now.plus(1, ChronoUnit.HOURS));
        assertEquals("At Risk", slaService.calculateSlaStatus(t2));

        // 3. Unresolved, past due -> Breached
        Ticket t3 = new Ticket();
        t3.setPriority(TicketPriority.HIGH);
        t3.setDueAt(now.minus(1, ChronoUnit.HOURS));
        assertEquals("Breached", slaService.calculateSlaStatus(t3));

        // 4. Resolved before due date -> Within SLA
        Ticket t4 = new Ticket();
        t4.setPriority(TicketPriority.MEDIUM);
        t4.setDueAt(now.plus(10, ChronoUnit.HOURS));
        t4.setResolvedAt(now.plus(2, ChronoUnit.HOURS));
        assertEquals("Within SLA", slaService.calculateSlaStatus(t4));

        // 5. Resolved after due date -> Breached
        Ticket t5 = new Ticket();
        t5.setPriority(TicketPriority.MEDIUM);
        t5.setDueAt(now.minus(5, ChronoUnit.HOURS));
        t5.setResolvedAt(now.minus(1, ChronoUnit.HOURS));
        assertEquals("Breached", slaService.calculateSlaStatus(t5));
    }
}
