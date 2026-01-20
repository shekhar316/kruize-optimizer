package com.kruize.optimizer.service;

import com.kruize.optimizer.model.HealthCheckResult;
import com.kruize.optimizer.model.ProfileScanResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class KruizeHealthService {

    private static final Logger LOG = Logger.getLogger(KruizeHealthService.class);

    @Inject
    ProfileScannerService profileScannerService;

    @Inject
    DatasourceService datasourceService;

    @Inject
    ProfileInstallerService profileInstallerService;

    @Inject
    MeterRegistry registry;

    // Global variable to store the last checked health
    private HealthCheckResult lastResult;

    /**
     * Performs a health check, optionally attempting to fix missing profiles.
     * Updates the global lastResult variable.
     *
     * @param attemptFix If true, will try to install missing profiles.
     * @return The updated HealthCheckResult
     */
    public HealthCheckResult performHealthCheck(boolean attemptFix) {
        HealthCheckResult result = new HealthCheckResult();
        List<String> issues = new ArrayList<>();
        result.setLastCheckedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        // Populate Stats
        Map<String, Double> stats = new HashMap<>();
        stats.put("total_jobs_created", registry.counter("total_jobs_created").count());
        stats.put("total_experiments_created", registry.counter("total_experiments_created").count());
        stats.put("total_experiments_processed", registry.counter("total_experiments_processed").count());
        result.setStats(stats);

        // 1. Check Profiles
        ProfileScanResult profileResult;

        // Scan first
        ProfileScanResult initialScan = profileScannerService.scanKruizeProfiles();
        boolean profilesMissing = initialScan.getAlerts() != null && !initialScan.getAlerts().isEmpty();

        if (profilesMissing && attemptFix) {
            LOG.info("Health Check: Profiles missing. Attempting to install...");
            // Use installer service which installs and returns the NEW state
            profileResult = profileInstallerService.checkAndInstallProfiles();
        } else {
            profileResult = initialScan;
        }

        // Pass through profile data
        result.setMetadataProfiles(profileResult.getMetadataProfiles());
        result.setMetricProfiles(profileResult.getMetricProfiles());
        result.setLayers(profileResult.getLayers());
        result.setRulesets(profileResult.getRulesets());
        result.setPendingUpdates(profileResult.getPendingUpdates());

        // Any remaining alerts from profile scan are issues
        if (profileResult.getAlerts() != null && !profileResult.getAlerts().isEmpty()) {
            issues.addAll(profileResult.getAlerts());
        }

        // 2. Check Datasources
        List<String> datasources = datasourceService.getConnectedDatasources();
        result.setDatasources(datasources);

        if (datasources.isEmpty()) {
            issues.add("No datasources connected to Kruize.");
        }

        // 3. Determine Overall Status
        if (issues.isEmpty()) {
            result.setStatus("HEALTHY");
        } else {
            result.setStatus("UNHEALTHY");
        }

        result.setIssues(issues);

        // Update Global Variable
        this.lastResult = result;
        return result;
    }

    /**
     * Returns the last checked health result.
     * If no check has been performed yet, triggers one without fixing.
     * 
     * @return HealthCheckResult
     */
    public HealthCheckResult getLastHealth() {
        if (lastResult == null) {
            return performHealthCheck(false);
        }
        return lastResult;
    }
}
