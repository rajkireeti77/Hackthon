package com.phishguardai.backend.repository;

import com.phishguardai.backend.entity.PhishingReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhishingReportRepository extends JpaRepository<PhishingReport, Long> {}
