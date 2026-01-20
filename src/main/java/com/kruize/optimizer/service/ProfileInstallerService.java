package com.kruize.optimizer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kruize.optimizer.client.KruizeClient;
import com.kruize.optimizer.model.ProfileScanResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class ProfileInstallerService {

    private static final Logger LOG = Logger.getLogger(ProfileInstallerService.class);

    @ConfigProperty(name = "kruize.profile.git.reference-url")
    String referenceUrl;

    @ConfigProperty(name = "kruize.profile.git.base-url")
    String githubRawBase;

    @Inject
    @RestClient
    KruizeClient kruizeClient;

    @Inject
    ProfileScannerService scannerService;

    // Use ObjectMapper for JSON conversions
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProfileScanResult checkAndInstallProfiles() {
        // 1. Get Reference Index
        ProfileScanResult referenceIndex = fetchReferenceIndex();
        if (referenceIndex == null) {
            ProfileScanResult error = new ProfileScanResult();
            if (error.getAlerts() == null)
                error.setAlerts(new ArrayList<>());
            error.addAlert("Failed to fetch reference index for installation.");
            return error;
        }

        // 2. Get Installed Profiles (Current State)
        ProfileScanResult installedState = scannerService.scanKruizeProfiles();
        if (installedState.getAlerts() == null) {
            installedState.setAlerts(new ArrayList<>());
        }

        // 3. Check and Install Metadata Profiles
        processProfiles(referenceIndex.getMetadataProfiles(), installedState.getMetadataProfiles(), "metadata",
                "metadata-profiles", installedState, false);

        // 4. Check and Install Metric Profiles
        processProfiles(referenceIndex.getMetricProfiles(), installedState.getMetricProfiles(), "metric",
                "metric-profiles", installedState, false);

        // 5. Check and Install Layers
        processProfiles(referenceIndex.getLayers(), installedState.getLayers(), "layer", "layers", installedState,
                false);

        // 6. Return final state (with alerts populated during processing)
        return installedState;
    }

    public void updateProfiles(List<String> targetStrings) {
        if (targetStrings == null || targetStrings.isEmpty())
            return;

        // No need to fetch Reference Index if we trust the user input string for
        // location
        for (String target : targetStrings) {
            try {
                // Expected format: folderName/profileName OR folderName/profileName/version
                // Example: metadata-profiles/cluster-metadata/v1.0
                String[] parts = target.split("/");
                if (parts.length < 2) {
                    LOG.errorf("Invalid target format: %s. Expected folderName/profileName[/version]", target);
                    continue;
                }

                String folderName = parts[0];
                String profileName = parts[1];
                String version = (parts.length >= 3) ? parts[2] : "v1.0"; // Default to v1.0

                // Map folder to type
                String type = getTypeFromFolder(folderName);
                if (type == null) {
                    LOG.errorf("Unknown folder type: %s", folderName);
                    continue;
                }

                // Install with force update
                installItem(profileName, version, String.format("%s.json", profileName), type, folderName, true);

            } catch (Exception e) {
                LOG.errorf("Error processing target %s: %s", target, e.getMessage());
            }
        }
    }

    private String getTypeFromFolder(String folder) {
        switch (folder) {
            case "metadata-profiles":
                return "metadata";
            case "metric-profiles":
                return "metric";
            case "layers":
                return "layer";
            default:
                return null;
        }
    }

    private String normalizeVersion(String v) {
        if (v == null || v.isEmpty())
            return "v1.0";
        if (!v.startsWith("v"))
            return "v" + v;
        return v;
    }

    private void processProfiles(List<ProfileScanResult.ParsedProfile> required,
            List<ProfileScanResult.ParsedProfile> installed,
            String type,
            String folderName,
            ProfileScanResult result,
            boolean forceUpdate) {
        if (required == null)
            return;
        if (installed == null)
            installed = List.of();

        Map<String, String> installedMap = installed.stream()
                .collect(Collectors.toMap(ProfileScanResult.ParsedProfile::getName,
                        p -> p.getProfileVersion() != null ? p.getProfileVersion() : "",
                        (v1, v2) -> v1));

        for (ProfileScanResult.ParsedProfile req : required) {
            boolean needsInstall = false;
            boolean isUpdate = false;
            // Default version to v1.0 if missing (especially for layers)
            String reqVersion = normalizeVersion(req.getProfileVersion());

            if (!installedMap.containsKey(req.getName())) {
                LOG.infof("Missing %s profile: %s", type, req.getName());
                needsInstall = true;
            } else {
                String installedVersion = normalizeVersion(installedMap.get(req.getName()));
                if (!installedVersion.equals(reqVersion)) {
                    String updateMsg = String.format(
                            "Version mismatch for %s profile: %s. Installed: '%s', Required: '%s'",
                            type, req.getName(), installedVersion, reqVersion);
                    LOG.info(updateMsg);

                    // Populate Pending Update
                    if (result.getPendingUpdates() == null)
                        result.setPendingUpdates(new ArrayList<>());

                    // Location: folderName/profileName/version
                    String location = String.format("%s/%s/%s", folderName, req.getName(), reqVersion);

                    result.addPendingUpdate(new ProfileScanResult.PendingUpdate(
                            req.getName(), type, installedVersion, reqVersion, location));

                    // Do NOT auto-update on version mismatch, but alert
                    if (result.getAlerts() == null)
                        result.setAlerts(new ArrayList<>());
                    result.addAlert(updateMsg);

                    needsInstall = false;
                    isUpdate = true;
                }
            }

            if (needsInstall) {
                // Format: v1.0/folderName/profileName.json
                // Note: The filename no longer includes the version as per requirement.
                String fileName = String.format("%s.json", req.getName());
                installItem(req.getName(), reqVersion, fileName, type, folderName, isUpdate);
            }
        }
    }

    private void installItem(String name, String version, String fileName, String type, String folderName,
            boolean isUpdate) {
        try {
            String content = null;

            if (isLocal()) {
                // Local structure: classpath:configs/version/folderName/fileName
                String resourcePath = String.format("configs/%s/%s/%s", version, folderName, fileName);
                LOG.infof("Reading local %s definition from classpath: %s", type, resourcePath);

                try (var inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
                    if (inputStream != null) {
                        content = new String(inputStream.readAllBytes());
                    } else {
                        LOG.errorf("Local resource not found: %s", resourcePath);
                    }
                }
            } else {
                // Remote structure: base/version/folderName/fileName
                String url = String.format("%s/%s/%s/%s", githubRawBase, version, folderName, fileName);
                LOG.infof("Fetching %s definition from: %s", type, url);
                content = fetchUrlContent(url);
            }

            if (content == null) {
                LOG.errorf("Failed to retrieve definition for %s", name);
                return;
            }

            // Convert String JSON to Object
            Object jsonObject = objectMapper.readValue(content, Object.class);

            // POST/PUT to Kruize
            switch (type) {
                case "metadata":
                    if (isUpdate) {
                        kruizeClient.updateMetadataProfile(name, jsonObject);
                        LOG.infof("Successfully updated metadata profile: %s", name);
                    } else {
                        kruizeClient.createMetadataProfile(jsonObject);
                        LOG.infof("Successfully created metadata profile: %s", name);
                    }
                    break;
                case "metric":
                    if (isUpdate) {
                        kruizeClient.updateMetricProfile(name, jsonObject);
                        LOG.infof("Successfully updated metric profile: %s", name);
                    } else {
                        kruizeClient.createMetricProfile(jsonObject);
                        LOG.infof("Successfully created metric profile: %s", name);
                    }
                    break;
                case "layer":
                    kruizeClient.createLayer(jsonObject);
                    LOG.infof("Successfully installed %s: %s", type, name);
                    break;
                default:
                    LOG.warn("Unknown type for installation: " + type);
                    return;
            }

        } catch (Exception e) {
            LOG.errorf("Failed to install/update %s %s: %s", type, name, e.getMessage());
        }
    }

    private boolean isLocal() {
        return (referenceUrl != null && referenceUrl.equalsIgnoreCase("local")) ||
                (githubRawBase != null && githubRawBase.equalsIgnoreCase("local"));
    }

    // Helper to fetch URL content
    private String fetchUrlContent(String url) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return response.body();
            } else {
                LOG.errorf("HTTP %d for URL: %s", response.statusCode(), url);
            }
        } catch (Exception e) {
            LOG.error("HTTP Fetch Error: " + e.getMessage());
        }
        return null;
    }

    // Helper to fetch reference
    private ProfileScanResult fetchReferenceIndex() {
        if (isLocal()) {
            try {
                // Local index: classpath:configs/config-master-index.json
                String resourcePath = "configs/config-master-index.json";
                LOG.infof("Reading local reference index from classpath: %s", resourcePath);

                try (var inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
                    if (inputStream != null) {
                        return objectMapper.readValue(inputStream, ProfileScanResult.class);
                    } else {
                        LOG.errorf("Local reference index resource not found: %s", resourcePath);
                    }
                }
            } catch (IOException e) {
                LOG.error("Local Index Read Error: " + e.getMessage());
            }
            return null;
        } else {
            String content = fetchUrlContent(referenceUrl);
            if (content != null) {
                try {
                    return objectMapper.readValue(content, ProfileScanResult.class);
                } catch (Exception e) {
                    LOG.error("JSON Parse Error: " + e.getMessage());
                }
            }
            return null;
        }
    }
}
