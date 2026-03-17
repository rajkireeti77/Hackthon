package com.phishguardai.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PhishGuardAiBackendApplication {
  public static void main(String[] args) {
    SpringApplication.run(PhishGuardAiBackendApplication.class, args);
  }
}

