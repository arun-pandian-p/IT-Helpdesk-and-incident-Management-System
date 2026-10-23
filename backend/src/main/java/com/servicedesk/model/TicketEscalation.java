package com.servicedesk.model;

import java.time.Instant;

public class TicketEscalation {
    private String id;
    private String ticketId;
    private String fromEngineer;
    private String fromEngineerName;
    private String toEngineer;
    private String toEngineerName;
    private String reason;
    private int escalationLevel;
    private Instant createdAt;
    private Instant resolvedAt;

    public TicketEscalation() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTicketId() { return ticketId; }
    public void setTicketId(String ticketId) { this.ticketId = ticketId; }

    public String getFromEngineer() { return fromEngineer; }
    public void setFromEngineer(String fromEngineer) { this.fromEngineer = fromEngineer; }

    public String getFromEngineerName() { return fromEngineerName; }
    public void setFromEngineerName(String fromEngineerName) { this.fromEngineerName = fromEngineerName; }

    public String getToEngineer() { return toEngineer; }
    public void setToEngineer(String toEngineer) { this.toEngineer = toEngineer; }

    public String getToEngineerName() { return toEngineerName; }
    public void setToEngineerName(String toEngineerName) { this.toEngineerName = toEngineerName; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public int getEscalationLevel() { return escalationLevel; }
    public void setEscalationLevel(int escalationLevel) { this.escalationLevel = escalationLevel; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
}
