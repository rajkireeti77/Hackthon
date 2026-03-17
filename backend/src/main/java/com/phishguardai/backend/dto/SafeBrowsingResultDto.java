package com.phishguardai.backend.dto;

import java.util.ArrayList;
import java.util.List;

public class SafeBrowsingResultDto {
  private boolean flagged;
  private List<String> threatTypes = new ArrayList<>();

  public SafeBrowsingResultDto() {}

  public SafeBrowsingResultDto(boolean flagged, List<String> threatTypes) {
    this.flagged = flagged;
    this.threatTypes = threatTypes == null ? new ArrayList<>() : threatTypes;
  }

  public boolean isFlagged() {
    return flagged;
  }

  public void setFlagged(boolean flagged) {
    this.flagged = flagged;
  }

  public List<String> getThreatTypes() {
    return threatTypes;
  }

  public void setThreatTypes(List<String> threatTypes) {
    this.threatTypes = threatTypes == null ? new ArrayList<>() : threatTypes;
  }
}

