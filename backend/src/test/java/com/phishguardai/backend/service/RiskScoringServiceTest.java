package com.phishguardai.backend.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.phishguardai.backend.dto.SafeBrowsingResultDto;
import com.phishguardai.backend.dto.SandboxScanResultDto;
import java.util.List;
import org.junit.jupiter.api.Test;

class RiskScoringServiceTest {

  private final RiskScoringService service = new RiskScoringService();

  @Test
  void otpAndBankRequestPushesScoreIntoSuspiciousRange() {
    int score =
        service.combineScore(
            10,
            "Urgent, tell me your OTP now or your bank account will be blocked. Verify your bank login immediately.",
            new SafeBrowsingResultDto(false, List.of()),
            SandboxScanResultDto.disabled(),
            List.of(),
            0,
            0,
            0);

    assertTrue(score >= 40, "OTP and bank-theft language should not remain safe/low-risk.");
  }

  @Test
  void prizeClaimLanguageGetsStrongScamPenalty() {
    int score =
        service.combineScore(
            10,
            "Congratulations, you won a prize reward. Click here to claim and send the processing fee now.",
            new SafeBrowsingResultDto(false, List.of()),
            SandboxScanResultDto.disabled(),
            List.of(),
            0,
            0,
            0);

    assertTrue(score >= 40, "Prize-claim scams should be scored as suspicious or worse.");
  }
}
