package com.phishguardai.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import java.io.FileInputStream;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FirebaseConfig {
  private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

  @Bean
  @ConditionalOnProperty(name = "app.security.disable-auth", havingValue = "false")
  public FirebaseApp firebaseApp(FirebaseProperties props) throws Exception {
    if (!FirebaseApp.getApps().isEmpty()) {
      return FirebaseApp.getInstance();
    }

    var serviceAccountPath = props.getServiceAccountPath();
    if (serviceAccountPath == null || serviceAccountPath.isBlank()) {
      throw new IllegalStateException(
          "Missing Firebase service account. Set FIREBASE_SERVICE_ACCOUNT_PATH to a JSON key file path.");
    }

    var credentials = GoogleCredentials.fromStream(new FileInputStream(serviceAccountPath));
    var builder = FirebaseOptions.builder().setCredentials(credentials);
    if (props.getProjectId() != null && !props.getProjectId().isBlank()) {
      builder.setProjectId(props.getProjectId());
    }
    var options = builder.build();

    var app = FirebaseApp.initializeApp(options);
    log.info("Initialized Firebase Admin SDK. projectId={}", Objects.toString(props.getProjectId(), "(auto)"));
    return app;
  }
}

