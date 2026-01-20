package com.kruize.optimizer.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class HealthCheckResult {
    private String status;
    private String lastCheckedAt;
    private List<String> datasources;

    @JsonProperty("metadata_profiles")
    private List<ProfileScanResult.ParsedProfile> metadataProfiles;

    @JsonProperty("metric_profiles")
    private List<ProfileScanResult.ParsedProfile> metricProfiles;

    private List<ProfileScanResult.ParsedProfile> layers;

    private List<ProfileScanResult.ParsedProfile> rulesets;

    private List<String> issues;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLastCheckedAt() {
        return lastCheckedAt;
    }

    public void setLastCheckedAt(String lastCheckedAt) {
        this.lastCheckedAt = lastCheckedAt;
    }

    public List<String> getDatasources() {
        return datasources;
    }

    public void setDatasources(List<String> datasources) {
        this.datasources = datasources;
    }

    public List<ProfileScanResult.ParsedProfile> getMetadataProfiles() {
        return metadataProfiles;
    }

    public void setMetadataProfiles(List<ProfileScanResult.ParsedProfile> metadataProfiles) {
        this.metadataProfiles = metadataProfiles;
    }

    public List<ProfileScanResult.ParsedProfile> getMetricProfiles() {
        return metricProfiles;
    }

    public void setMetricProfiles(List<ProfileScanResult.ParsedProfile> metricProfiles) {
        this.metricProfiles = metricProfiles;
    }

    public List<ProfileScanResult.ParsedProfile> getLayers() {
        return layers;
    }

    public void setLayers(List<ProfileScanResult.ParsedProfile> layers) {
        this.layers = layers;
    }

    public List<ProfileScanResult.ParsedProfile> getRulesets() {
        return rulesets;
    }

    public void setRulesets(List<ProfileScanResult.ParsedProfile> rulesets) {
        this.rulesets = rulesets;
    }

    private Map<String, Double> stats;

    public Map<String, Double> getStats() {
        return stats;
    }

    public void setStats(Map<String, Double> stats) {
        this.stats = stats;
    }

    public List<String> getIssues() {
        return issues;
    }

    public void setIssues(List<String> issues) {
        this.issues = issues;
    }

    @JsonProperty("pending_updates")
    private List<ProfileScanResult.PendingUpdate> pendingUpdates;

    public List<ProfileScanResult.PendingUpdate> getPendingUpdates() {
        return pendingUpdates;
    }

    public void setPendingUpdates(List<ProfileScanResult.PendingUpdate> pendingUpdates) {
        this.pendingUpdates = pendingUpdates;
    }
}
