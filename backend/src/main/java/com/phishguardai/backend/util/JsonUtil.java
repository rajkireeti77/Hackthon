package com.phishguardai.backend.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonUtil {
  private final ObjectMapper mapper;

  public JsonUtil(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  public String toJson(Object value) {
    try {
      return mapper.writeValueAsString(value);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to serialize JSON", e);
    }
  }

  public <T> T fromJson(String json, Class<T> clazz) {
    try {
      return mapper.readValue(json, clazz);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to parse JSON", e);
    }
  }

  public <T> T fromJson(String json, TypeReference<T> type) {
    try {
      return mapper.readValue(json, type);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to parse JSON", e);
    }
  }
}

