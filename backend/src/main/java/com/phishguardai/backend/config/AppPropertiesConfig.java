package com.phishguardai.backend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
  CorsProperties.class,
  FirebaseProperties.class,
  GeminiProperties.class,
  IncomingMailProperties.class,
  WindowsNotificationsProperties.class,
  LocalLlmProperties.class,
  SandboxScannerProperties.class,
  SafeBrowsingProperties.class,
  UrlModelProperties.class,
  TextModelProperties.class,
  FileModelProperties.class
})
public class AppPropertiesConfig {}

