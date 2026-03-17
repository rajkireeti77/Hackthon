package com.phishguardai.backend.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class WindowsNotificationIngestServiceTest {

  @Test
  void recognizesPhishGuardGeneratedNotifications() {
    assertTrue(
        WindowsNotificationIngestService.looksLikePhishGuardNotification(
            List.of(
                "Unsafe message from WhatsApp",
                "Hackathon: Malicious or spoofed link behavior was detected in the message. URL status: 1 malicious. Do not trust or open it yet.")));
  }

  @Test
  void doesNotIgnoreNormalIncomingWhatsappMessages() {
    assertFalse(
        WindowsNotificationIngestService.looksLikePhishGuardNotification(
            List.of("Fisherman", "Tell me your opt")));
  }
}
