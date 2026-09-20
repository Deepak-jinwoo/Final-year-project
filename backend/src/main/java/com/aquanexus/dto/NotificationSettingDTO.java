package com.aquanexus.dto;

import com.aquanexus.model.NotificationSetting;

public class NotificationSettingDTO {

    private Integer industryId;
    private String industryName;
    private String registeredEmail;
    private String registeredMobile;
    private Boolean emailNotificationsEnabled;
    private Boolean smsNotificationsEnabled;
    private Boolean notifyWarning;
    private Boolean notifyCritical;
    private Boolean notifyExceeded;
    private Boolean notifyAbnormalUsage;
    private String recipientRoles;
    private Boolean monthlyReportEmailEnabled;
    private Double unaccountedWaterThresholdPct;
    private Double unaccountedWaterThresholdKl;
    private Double lowReuseTargetPct;
    private Double untappedReuseThresholdKl;

    public NotificationSettingDTO() {}

    public static NotificationSettingDTO fromEntity(NotificationSetting s) {
        if (s == null) return null;
        NotificationSettingDTO dto = new NotificationSettingDTO();
        dto.setIndustryId(s.getIndustryId());
        dto.setIndustryName(s.getIndustryName());
        dto.setRegisteredEmail(s.getRegisteredEmail());
        dto.setRegisteredMobile(s.getRegisteredMobile());
        dto.setEmailNotificationsEnabled(s.getEmailNotificationsEnabled());
        dto.setSmsNotificationsEnabled(s.getSmsNotificationsEnabled());
        dto.setNotifyWarning(s.getNotifyWarning());
        dto.setNotifyCritical(s.getNotifyCritical());
        dto.setNotifyExceeded(s.getNotifyExceeded());
        dto.setNotifyAbnormalUsage(s.getNotifyAbnormalUsage());
        dto.setRecipientRoles(s.getRecipientRoles());
        dto.setMonthlyReportEmailEnabled(s.getMonthlyReportEmailEnabled());
        dto.setUnaccountedWaterThresholdPct(s.getUnaccountedWaterThresholdPct());
        dto.setUnaccountedWaterThresholdKl(s.getUnaccountedWaterThresholdKl());
        dto.setLowReuseTargetPct(s.getLowReuseTargetPct());
        dto.setUntappedReuseThresholdKl(s.getUntappedReuseThresholdKl());
        return dto;
    }

    public void updateEntity(NotificationSetting s) {
        if (s == null) return;
        if (this.industryName != null) s.setIndustryName(this.industryName);
        if (this.registeredEmail != null) s.setRegisteredEmail(this.registeredEmail);
        if (this.registeredMobile != null) s.setRegisteredMobile(this.registeredMobile);
        if (this.emailNotificationsEnabled != null) s.setEmailNotificationsEnabled(this.emailNotificationsEnabled);
        if (this.smsNotificationsEnabled != null) s.setSmsNotificationsEnabled(this.smsNotificationsEnabled);
        if (this.notifyWarning != null) s.setNotifyWarning(this.notifyWarning);
        if (this.notifyCritical != null) s.setNotifyCritical(this.notifyCritical);
        if (this.notifyExceeded != null) s.setNotifyExceeded(this.notifyExceeded);
        if (this.notifyAbnormalUsage != null) s.setNotifyAbnormalUsage(this.notifyAbnormalUsage);
        if (this.recipientRoles != null) s.setRecipientRoles(this.recipientRoles);
        if (this.monthlyReportEmailEnabled != null) s.setMonthlyReportEmailEnabled(this.monthlyReportEmailEnabled);
        if (this.unaccountedWaterThresholdPct != null) s.setUnaccountedWaterThresholdPct(this.unaccountedWaterThresholdPct);
        if (this.unaccountedWaterThresholdKl != null) s.setUnaccountedWaterThresholdKl(this.unaccountedWaterThresholdKl);
        if (this.lowReuseTargetPct != null) s.setLowReuseTargetPct(this.lowReuseTargetPct);
        if (this.untappedReuseThresholdKl != null) s.setUntappedReuseThresholdKl(this.untappedReuseThresholdKl);
    }

    // Getters and Setters
    public Integer getIndustryId() { return industryId; }
    public void setIndustryId(Integer industryId) { this.industryId = industryId; }

    public String getIndustryName() { return industryName; }
    public void setIndustryName(String industryName) { this.industryName = industryName; }

    public String getRegisteredEmail() { return registeredEmail; }
    public void setRegisteredEmail(String registeredEmail) { this.registeredEmail = registeredEmail; }

    public String getRegisteredMobile() { return registeredMobile; }
    public void setRegisteredMobile(String registeredMobile) { this.registeredMobile = registeredMobile; }

    public Boolean getEmailNotificationsEnabled() { return emailNotificationsEnabled; }
    public void setEmailNotificationsEnabled(Boolean emailNotificationsEnabled) { this.emailNotificationsEnabled = emailNotificationsEnabled; }

    public Boolean getSmsNotificationsEnabled() { return smsNotificationsEnabled; }
    public void setSmsNotificationsEnabled(Boolean smsNotificationsEnabled) { this.smsNotificationsEnabled = smsNotificationsEnabled; }

    public Boolean getNotifyWarning() { return notifyWarning; }
    public void setNotifyWarning(Boolean notifyWarning) { this.notifyWarning = notifyWarning; }

    public Boolean getNotifyCritical() { return notifyCritical; }
    public void setNotifyCritical(Boolean notifyCritical) { this.notifyCritical = notifyCritical; }

    public Boolean getNotifyExceeded() { return notifyExceeded; }
    public void setNotifyExceeded(Boolean notifyExceeded) { this.notifyExceeded = notifyExceeded; }

    public Boolean getNotifyAbnormalUsage() { return notifyAbnormalUsage; }
    public void setNotifyAbnormalUsage(Boolean notifyAbnormalUsage) { this.notifyAbnormalUsage = notifyAbnormalUsage; }

    public String getRecipientRoles() { return recipientRoles; }
    public void setRecipientRoles(String recipientRoles) { this.recipientRoles = recipientRoles; }

    public Boolean getMonthlyReportEmailEnabled() { return monthlyReportEmailEnabled; }
    public void setMonthlyReportEmailEnabled(Boolean monthlyReportEmailEnabled) { this.monthlyReportEmailEnabled = monthlyReportEmailEnabled; }

    public Double getUnaccountedWaterThresholdPct() { return unaccountedWaterThresholdPct; }
    public void setUnaccountedWaterThresholdPct(Double unaccountedWaterThresholdPct) { this.unaccountedWaterThresholdPct = unaccountedWaterThresholdPct; }

    public Double getUnaccountedWaterThresholdKl() { return unaccountedWaterThresholdKl; }
    public void setUnaccountedWaterThresholdKl(Double unaccountedWaterThresholdKl) { this.unaccountedWaterThresholdKl = unaccountedWaterThresholdKl; }

    public Double getLowReuseTargetPct() { return lowReuseTargetPct; }
    public void setLowReuseTargetPct(Double lowReuseTargetPct) { this.lowReuseTargetPct = lowReuseTargetPct; }

    public Double getUntappedReuseThresholdKl() { return untappedReuseThresholdKl; }
    public void setUntappedReuseThresholdKl(Double untappedReuseThresholdKl) { this.untappedReuseThresholdKl = untappedReuseThresholdKl; }
}
