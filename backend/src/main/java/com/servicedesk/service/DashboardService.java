package com.servicedesk.service;

import com.servicedesk.dto.DashboardDtos.DashboardSummaryDto;
import com.servicedesk.model.Constants.TicketPriority;
import com.servicedesk.model.Constants.TicketStatus;
import com.servicedesk.model.Ticket;
import com.servicedesk.model.TicketCategory;
import com.servicedesk.model.TicketFeedback;
import com.servicedesk.repository.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DashboardService {

    private final TicketRepository ticketRepository;
    private final AssetRepository assetRepository;
    private final OnboardingRepository onboardingRepository;
    private final AccessRequestRepository accessRequestRepository;
    private final CategoryRepository categoryRepository;
    private final SlaService slaService;
    private final JdbcTemplate jdbcTemplate;

    public DashboardService(TicketRepository ticketRepository,
                            AssetRepository assetRepository,
                            OnboardingRepository onboardingRepository,
                            AccessRequestRepository accessRequestRepository,
                            CategoryRepository categoryRepository,
                            SlaService slaService,
                            JdbcTemplate jdbcTemplate) {
        this.ticketRepository = ticketRepository;
        this.assetRepository = assetRepository;
        this.onboardingRepository = onboardingRepository;
        this.accessRequestRepository = accessRequestRepository;
        this.categoryRepository = categoryRepository;
        this.slaService = slaService;
        this.jdbcTemplate = jdbcTemplate;
    }

    public DashboardSummaryDto getSummary() {
        List<Ticket> allTickets = ticketRepository.getAllTicketsForMetrics();
        int totalTickets = allTickets.size();

        int openTickets = 0;
        int inProgress = 0;
        int waitingTickets = 0;
        int resolvedTickets = 0;
        int closedTickets = 0;
        int criticalTickets = 0;
        int slaBreached = 0;
        int slaAtRisk = 0;

        List<Double> resolutionHours = new ArrayList<>();
        List<Double> firstResponseHours = new ArrayList<>();

        Map<String, Integer> statusDist = new LinkedHashMap<>();
        statusDist.put("Open", 0);
        statusDist.put("In Progress", 0);
        statusDist.put("Waiting", 0);
        statusDist.put("Resolved", 0);
        statusDist.put("Closed", 0);

        Map<String, Integer> priorityDist = new LinkedHashMap<>();
        priorityDist.put("Critical", 0);
        priorityDist.put("High", 0);
        priorityDist.put("Medium", 0);
        priorityDist.put("Low", 0);

        Map<String, Integer> catDist = new HashMap<>();
        Map<String, Integer> osDist = new HashMap<>();

        for (Ticket t : allTickets) {
            String status = t.getStatus();
            String priority = t.getPriority();
            String sla = slaService.calculateSlaStatus(t);

            if ("Breached".equals(sla)) slaBreached++;
            if ("At Risk".equals(sla)) slaAtRisk++;

            if (TicketStatus.OPEN.equals(status)) {
                openTickets++;
                statusDist.put("Open", statusDist.get("Open") + 1);
            } else if (TicketStatus.IN_PROGRESS.equals(status)) {
                inProgress++;
                statusDist.put("In Progress", statusDist.get("In Progress") + 1);
            } else if (TicketStatus.WAITING_FOR_USER.equals(status) || TicketStatus.WAITING_FOR_VENDOR.equals(status)) {
                waitingTickets++;
                statusDist.put("Waiting", statusDist.get("Waiting") + 1);
            } else if (TicketStatus.RESOLVED.equals(status)) {
                resolvedTickets++;
                statusDist.put("Resolved", statusDist.get("Resolved") + 1);
            } else if (TicketStatus.CLOSED.equals(status)) {
                closedTickets++;
                statusDist.put("Closed", statusDist.get("Closed") + 1);
            }

            if (TicketPriority.CRITICAL.equals(priority)) {
                priorityDist.put("Critical", priorityDist.get("Critical") + 1);
                if (!TicketStatus.RESOLVED.equals(status) && !TicketStatus.CLOSED.equals(status)) {
                    criticalTickets++;
                }
            } else if (TicketPriority.HIGH.equals(priority)) {
                priorityDist.put("High", priorityDist.get("High") + 1);
            } else if (TicketPriority.MEDIUM.equals(priority)) {
                priorityDist.put("Medium", priorityDist.get("Medium") + 1);
            } else if (TicketPriority.LOW.equals(priority)) {
                priorityDist.put("Low", priorityDist.get("Low") + 1);
            }

            String catName = t.getCategoryName() != null ? t.getCategoryName() : "General";
            catDist.put(catName, catDist.getOrDefault(catName, 0) + 1);

            String osName = t.getOsName() != null ? t.getOsName() : "Windows 11";
            osDist.put(osName, osDist.getOrDefault(osName, 0) + 1);

            if (t.getResolvedAt() != null && t.getCreatedAt() != null) {
                double hours = Duration.between(t.getCreatedAt(), t.getResolvedAt()).toSeconds() / 3600.0;
                resolutionHours.add(hours);
            }

            if (t.getFirstResponseAt() != null && t.getCreatedAt() != null) {
                double hours = Duration.between(t.getCreatedAt(), t.getFirstResponseAt()).toSeconds() / 3600.0;
                firstResponseHours.add(hours);
            }
        }

        double avgResolution = resolutionHours.isEmpty() ? 0.0 :
                Math.round((resolutionHours.stream().mapToDouble(Double::doubleValue).sum() / resolutionHours.size()) * 10.0) / 10.0;

        double avgFirstResponse = firstResponseHours.isEmpty() ? 0.0 :
                Math.round((firstResponseHours.stream().mapToDouble(Double::doubleValue).sum() / firstResponseHours.size()) * 10.0) / 10.0;

        // CSAT Average
        Double avgCsatVal = jdbcTemplate.queryForObject("SELECT AVG(rating) FROM ticket_feedback", Double.class);
        double avgCsat = avgCsatVal != null ? Math.round(avgCsatVal * 100.0) / 100.0 : 4.8;

        // Assets, onboardings, access requests
        int totalAssets = assetRepository.count();
        int activeOnboardings = onboardingRepository.countActive();
        int pendingAccess = accessRequestRepository.countPending();

        // 14-day daily trend
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        DateTimeFormatter trendFormat = DateTimeFormatter.ofPattern("MMM dd", Locale.ENGLISH);
        List<Map<String, Object>> dailyTrend = new ArrayList<>();

        for (int i = 13; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            String dayStr = d.format(trendFormat);

            int createdCount = 0;
            int resolvedCount = 0;

            for (Ticket t : allTickets) {
                if (t.getCreatedAt() != null) {
                    LocalDate createdDate = LocalDate.ofInstant(t.getCreatedAt(), ZoneOffset.UTC);
                    if (createdDate.equals(d)) createdCount++;
                }
                if (t.getResolvedAt() != null) {
                    LocalDate resDate = LocalDate.ofInstant(t.getResolvedAt(), ZoneOffset.UTC);
                    if (resDate.equals(d)) resolvedCount++;
                }
            }

            Map<String, Object> dayMap = new HashMap<>();
            dayMap.put("date", dayStr);
            dayMap.put("created", createdCount);
            dayMap.put("resolved", resolvedCount);
            dailyTrend.add(dayMap);
        }

        DashboardSummaryDto dto = new DashboardSummaryDto();
        dto.setTotalTickets(totalTickets);
        dto.setOpenTickets(openTickets);
        dto.setInProgressTickets(inProgress);
        dto.setWaitingTickets(waitingTickets);
        dto.setResolvedTickets(resolvedTickets);
        dto.setClosedTickets(closedTickets);
        dto.setCriticalTickets(criticalTickets);
        dto.setSlaBreached(slaBreached);
        dto.setSlaAtRisk(slaAtRisk);
        dto.setAverageResolutionHours(avgResolution);
        dto.setAverageFirstResponseHours(avgFirstResponse);
        dto.setCsatAverageRating(avgCsat);
        dto.setTotalAssets(totalAssets);
        dto.setActiveOnboardings(activeOnboardings);
        dto.setPendingAccessRequests(pendingAccess);
        dto.setStatusDistribution(statusDist);
        dto.setPriorityDistribution(priorityDist);
        dto.setCategoryDistribution(catDist);
        dto.setOsDistribution(osDist);
        dto.setDailyTrend(dailyTrend);

        return dto;
    }
}
