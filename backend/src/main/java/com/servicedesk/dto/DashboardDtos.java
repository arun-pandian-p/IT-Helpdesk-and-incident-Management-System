package com.servicedesk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class DashboardDtos {

    public static class DashboardSummaryDto {
        @JsonProperty("total_tickets")
        private int totalTickets;
        @JsonProperty("open_tickets")
        private int openTickets;
        @JsonProperty("in_progress_tickets")
        private int inProgressTickets;
        @JsonProperty("waiting_tickets")
        private int waitingTickets;
        @JsonProperty("resolved_tickets")
        private int resolvedTickets;
        @JsonProperty("closed_tickets")
        private int closedTickets;
        @JsonProperty("critical_tickets")
        private int criticalTickets;
        @JsonProperty("sla_breached")
        private int slaBreached;
        @JsonProperty("sla_at_risk")
        private int slaAtRisk;
        @JsonProperty("average_resolution_hours")
        private double averageResolutionHours;
        @JsonProperty("average_first_response_hours")
        private double averageFirstResponseHours;
        @JsonProperty("csat_average_rating")
        private double csatAverageRating;
        @JsonProperty("total_assets")
        private int totalAssets;
        @JsonProperty("active_onboardings")
        private int activeOnboardings;
        @JsonProperty("pending_access_requests")
        private int pendingAccessRequests;

        @JsonProperty("status_distribution")
        private Map<String, Integer> statusDistribution;
        @JsonProperty("priority_distribution")
        private Map<String, Integer> priorityDistribution;
        @JsonProperty("category_distribution")
        private Map<String, Integer> categoryDistribution;
        @JsonProperty("os_distribution")
        private Map<String, Integer> osDistribution;
        @JsonProperty("daily_trend")
        private List<Map<String, Object>> dailyTrend;

        public int getTotalTickets() { return totalTickets; }
        public void setTotalTickets(int totalTickets) { this.totalTickets = totalTickets; }
        public int getOpenTickets() { return openTickets; }
        public void setOpenTickets(int openTickets) { this.openTickets = openTickets; }
        public int getInProgressTickets() { return inProgressTickets; }
        public void setInProgressTickets(int inProgressTickets) { this.inProgressTickets = inProgressTickets; }
        public int getWaitingTickets() { return waitingTickets; }
        public void setWaitingTickets(int waitingTickets) { this.waitingTickets = waitingTickets; }
        public int getResolvedTickets() { return resolvedTickets; }
        public void setResolvedTickets(int resolvedTickets) { this.resolvedTickets = resolvedTickets; }
        public int getClosedTickets() { return closedTickets; }
        public void setClosedTickets(int closedTickets) { this.closedTickets = closedTickets; }
        public int getCriticalTickets() { return criticalTickets; }
        public void setCriticalTickets(int criticalTickets) { this.criticalTickets = criticalTickets; }
        public int getSlaBreached() { return slaBreached; }
        public void setSlaBreached(int slaBreached) { this.slaBreached = slaBreached; }
        public int getSlaAtRisk() { return slaAtRisk; }
        public void setSlaAtRisk(int slaAtRisk) { this.slaAtRisk = slaAtRisk; }
        public double getAverageResolutionHours() { return averageResolutionHours; }
        public void setAverageResolutionHours(double averageResolutionHours) { this.averageResolutionHours = averageResolutionHours; }
        public double getAverageFirstResponseHours() { return averageFirstResponseHours; }
        public void setAverageFirstResponseHours(double averageFirstResponseHours) { this.averageFirstResponseHours = averageFirstResponseHours; }
        public double getCsatAverageRating() { return csatAverageRating; }
        public void setCsatAverageRating(double csatAverageRating) { this.csatAverageRating = csatAverageRating; }
        public int getTotalAssets() { return totalAssets; }
        public void setTotalAssets(int totalAssets) { this.totalAssets = totalAssets; }
        public int getActiveOnboardings() { return activeOnboardings; }
        public void setActiveOnboardings(int activeOnboardings) { this.activeOnboardings = activeOnboardings; }
        public int getPendingAccessRequests() { return pendingAccessRequests; }
        public void setPendingAccessRequests(int pendingAccessRequests) { this.pendingAccessRequests = pendingAccessRequests; }
        public Map<String, Integer> getStatusDistribution() { return statusDistribution; }
        public void setStatusDistribution(Map<String, Integer> statusDistribution) { this.statusDistribution = statusDistribution; }
        public Map<String, Integer> getPriorityDistribution() { return priorityDistribution; }
        public void setPriorityDistribution(Map<String, Integer> priorityDistribution) { this.priorityDistribution = priorityDistribution; }
        public Map<String, Integer> getCategoryDistribution() { return categoryDistribution; }
        public void setCategoryDistribution(Map<String, Integer> categoryDistribution) { this.categoryDistribution = categoryDistribution; }
        public Map<String, Integer> getOsDistribution() { return osDistribution; }
        public void setOsDistribution(Map<String, Integer> osDistribution) { this.osDistribution = osDistribution; }
        public List<Map<String, Object>> getDailyTrend() { return dailyTrend; }
        public void setDailyTrend(List<Map<String, Object>> dailyTrend) { this.dailyTrend = dailyTrend; }
    }
}
