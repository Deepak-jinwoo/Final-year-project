package com.aquanexus.dto;

public class AlertActionDTO {

    private String action; // ACKNOWLEDGE, RESOLVE
    private String resolutionNotes;
    private String resolvedBy;

    public AlertActionDTO() {}

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }

    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }
}
