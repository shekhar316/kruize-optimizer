package com.kruize.optimizer.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class ProfileScanResult {
    @JsonProperty("metadata_profiles")
    private List<ParsedProfile> metadataProfiles = new ArrayList<>();

    @JsonProperty("metric_profiles")
    private List<ParsedProfile> metricProfiles = new ArrayList<>();

    private List<ParsedProfile> layers = new ArrayList<>();

    private List<ParsedProfile> rulesets = new ArrayList<>();

    private List<String> alerts = new ArrayList<>();

    public List<ParsedProfile> getMetadataProfiles() {
        return metadataProfiles;
    }

    public void setMetadataProfiles(List<ParsedProfile> metadataProfiles) {
        this.metadataProfiles = metadataProfiles;
    }

    public List<ParsedProfile> getMetricProfiles() {
        return metricProfiles;
    }

    public void setMetricProfiles(List<ParsedProfile> metricProfiles) {
        this.metricProfiles = metricProfiles;
    }

    public List<ParsedProfile> getLayers() {
        return layers;
    }

    public void setLayers(List<ParsedProfile> layers) {
        this.layers = layers;
    }

    public List<ParsedProfile> getRulesets() {
        return rulesets;
    }

    public void setRulesets(List<ParsedProfile> rulesets) {
        this.rulesets = rulesets;
    }

    public List<String> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<String> alerts) {
        this.alerts = alerts;
    }

    public void addAlert(String alert) {
        this.alerts.add(alert);
    }

    public List<PendingUpdate> getPendingUpdates() {
        return pendingUpdates;
    }

    public void setPendingUpdates(List<PendingUpdate> pendingUpdates) {
        this.pendingUpdates = pendingUpdates;
    }

    public void addPendingUpdate(PendingUpdate update) {
        this.pendingUpdates.add(update);
    }

    @JsonProperty("pending_updates")
    private List<PendingUpdate> pendingUpdates = new ArrayList<>();

    public static class PendingUpdate {
        private String name;
        private String type;
        private String currentVersion;
        private String targetVersion;
        private String location; // e.g., metadata-profiles/name/version

        public PendingUpdate() {
        }

        public PendingUpdate(String name, String type, String currentVersion, String targetVersion, String location) {
            this.name = name;
            this.type = type;
            this.currentVersion = currentVersion;
            this.targetVersion = targetVersion;
            this.location = location;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getCurrentVersion() {
            return currentVersion;
        }

        public void setCurrentVersion(String currentVersion) {
            this.currentVersion = currentVersion;
        }

        public String getTargetVersion() {
            return targetVersion;
        }

        public void setTargetVersion(String targetVersion) {
            this.targetVersion = targetVersion;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }
    }

    public static class ParsedProfile {
        private String name;
        @JsonProperty("profile_version")
        private String profileVersion;

        public ParsedProfile() {
        }

        public ParsedProfile(String name, String profileVersion) {
            this.name = name;
            this.profileVersion = profileVersion;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getProfileVersion() {
            return profileVersion;
        }

        public void setProfileVersion(String profileVersion) {
            this.profileVersion = profileVersion;
        }
    }
}
