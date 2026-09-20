package com.aquanexus.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Stores alert threshold settings and notification preferences for an industry/facility.
 */
@Entity
@Table(name = "notification_settings")
public class NotificationSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "industry_id", nullable = false, unique = true)
    private Integer industryId = 1;

    @Column(name = "industry_name", length = 200)
    private String industryName = "Demo Dairy Industry";

    @Column(name = "registered_email", length = 255)
    private String registeredEmail = "compliance@aquanexus.ind";

    @Column(name = "registered_mobile", length = 20)
    private String registeredMobile = "+919876543210";

    @Column(name = "email_notifications_enabled")
    private Boolean emailNotificationsEnabled = true;

    @Column(name = "sms_notifications_enabled")
    private Boolean smsNotificationsEnabled = true;

    @Column(name = "notify_warning")
    private Boolean notifyWarning = true;

    @Column(name = "notify_critical")
    private Boolean notifyCritical = true;

    @Column(name = "notify_exceeded")
    private Boolean notifyExceeded = true;

    @Column(name = "notify_abnormal_usage")
    private Boolean notifyAbnormalUsage = true;

    @Column(name = "recipient_roles", length = 500)
    private String recipientRoles = "Industry Admin,Compliance Officer,Plant Manager";

    @Column(name = "monthly_report_email_enabled")
    private Boolean monthlyReportEmailEnabled = true;

    @Column(name = "unaccounted_water_threshold_pct")
    private Double unaccountedWaterThresholdPct = 3.0; // 3% of main consumption

    @Column(name = "unaccounted_water_threshold_kl")
    private Double unaccountedWaterThresholdKl = 5.0; // 5.0 kL fixed

    @Column(name = "low_reuse_target_pct")
    private Double lowReuseTargetPct = 60.0; // 60% minimum recovery target

    @Column(name = "untapped_reuse_threshold_kl")
    private Double untappedReuseThresholdKl = 15.0; // 15 kL untreated/unreused potential

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        this.updatedAt = LocalDateTime.now();
    }

    public NotificationSetting() {}

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
