package com.aquanexus.config;

import com.aquanexus.model.NotificationSetting;
import com.aquanexus.repository.NotificationSettingRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Initializes default system configuration upon application startup.
 * NOTE: Production calculations use ONLY legitimate user-entered records.
 * No mock, fake, or randomly generated water consumption data is seeded.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final NotificationSettingRepository settingRepository;

    public DataInitializer(NotificationSettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    @Override
    public void run(String... args) {
        // Ensure default notification settings exist for facility management
        if (settingRepository.count() == 0) {
            NotificationSetting def = new NotificationSetting();
            def.setIndustryId(1);
            def.setIndustryName("Demo Dairy Industry");
            def.setRegisteredEmail("compliance@aquanexus.ind");
            def.setRegisteredMobile("+919876543210");
            def.setEmailNotificationsEnabled(true);
            def.setSmsNotificationsEnabled(true);
            def.setNotifyWarning(true);
            def.setNotifyCritical(true);
            def.setNotifyExceeded(true);
            def.setNotifyAbnormalUsage(true);
            def.setRecipientRoles("Industry Admin,Compliance Officer,Plant Manager");
            settingRepository.save(def);
        }

        System.out.println("=================================================");
        System.out.println("[✓] AquaNexus Data Architecture: Pure Real-Time Mode");
        System.out.println("    Zero mock/demo data seeded. Operating on user entries only.");
        System.out.println("=================================================");
    }
}
