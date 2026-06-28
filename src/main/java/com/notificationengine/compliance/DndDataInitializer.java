package com.notificationengine.compliance;

import com.notificationengine.entity.ComplianceRecord;
import com.notificationengine.entity.ComplianceRecord.ConsentStatus;
import com.notificationengine.entity.ComplianceRecord.ConsentType;
import com.notificationengine.entity.DndRegistry;
import com.notificationengine.repository.ComplianceRecordRepository;
import com.notificationengine.repository.DndRegistryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Seeds test data for TRAI DND Registry and Consent Records on startup.
 */
@Slf4j
@Component
@Order(2) // Run after UserProfileDataInitializer
@RequiredArgsConstructor
public class DndDataInitializer implements CommandLineRunner {

    private final DndRegistryRepository dndRegistryRepository;
    private final ComplianceRecordRepository complianceRecordRepository;

    @Override
    public void run(String... args) {
        if (dndRegistryRepository.count() > 0) {
            log.info("[DND-SEEDER] DND registry already seeded, skipping initialization.");
            return;
        }

        log.info("[DND-SEEDER] Seeding initial DND registry and consent records...");

        // Note: The phone numbers match the hashes generated in NotificationEventServiceImpl.resolveRecipientAddress
        // For simplicity, we just block arbitrary numbers here that we know we'll use in testing

        // user_hi_001 -> +120255501XX
        // Let's just block the explicit mock numbers used in our Walkthroughs

        List<DndRegistry> dndRecords = List.of(
                // 1. Fully blocked
                DndRegistry.builder()
                        .phoneNumber("+919876543210")
                        .fullyBlocked(true)
                        .allowedCategories("")
                        .build(),

                // 2. Partially blocked (Allows Banking and Education promos only)
                DndRegistry.builder()
                        .phoneNumber("+919876543211")
                        .fullyBlocked(false)
                        .allowedCategories("BANKING,EDUCATION")
                        .build(),

                // 3. User HI 001's SMS number (fully blocked)
                DndRegistry.builder()
                        .phoneNumber("+12025550186") // user_hi_001 SMS hash
                        .fullyBlocked(true)
                        .build()
        );

        dndRegistryRepository.saveAll(dndRecords);

        // Seed some WhatsApp consents
        List<ComplianceRecord> consents = List.of(
                ComplianceRecord.builder()
                        .userId("user_hi_001")
                        .consentType(ConsentType.WHATSAPP_EXPLICIT)
                        .consentStatus(ConsentStatus.GRANTED)
                        .consentGivenAt(LocalDateTime.now())
                        .build(),
                ComplianceRecord.builder()
                        .userId("user_mr_002")
                        .consentType(ConsentType.MARKETING)
                        .consentStatus(ConsentStatus.GRANTED)
                        .consentGivenAt(LocalDateTime.now())
                        .build()
        );

        complianceRecordRepository.saveAll(consents);

        log.info("[DND-SEEDER] Successfully seeded {} DND records and {} consent records.", dndRecords.size(), consents.size());
    }
}
