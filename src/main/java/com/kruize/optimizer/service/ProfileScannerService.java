package com.kruize.optimizer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kruize.optimizer.client.KruizeClient;
import com.kruize.optimizer.model.KruizeProfile;
import com.kruize.optimizer.model.ProfileScanResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@ApplicationScoped
public class ProfileScannerService {

    private static final Logger LOG = Logger.getLogger(ProfileScannerService.class);

    @ConfigProperty(name = "kruize.profile.git.reference-url")
    String referenceUrl;

    @Inject
    @RestClient
    KruizeClient kruizeClient;

    public ProfileScanResult scanKruizeProfiles() {
        ProfileScanResult result = new ProfileScanResult();
        List<String> alerts = new ArrayList<>();

        // 1. Fetch Reference Profile Index
        ProfileScanResult referenceIndex = fetchReferenceIndex();

        // 2. Fetch Installed Profiles from Kruize
        List<KruizeProfile> metadataProfiles = safeGetProfiles(() -> kruizeClient.getMetadataProfiles(true));
        List<KruizeProfile> metricProfiles = safeGetProfiles(() -> kruizeClient.getMetricProfiles(true));
        List<KruizeProfile> layers = safeGetProfiles(() -> kruizeClient.getLayers());

        // 3. Map to Result
        result.setMetadataProfiles(mapProfiles(metadataProfiles));
        result.setMetricProfiles(mapProfiles(metricProfiles));
        result.setLayers(mapProfiles(layers));

        // 4. Validate against Reference
        if (referenceIndex != null) {
            validateProfiles(metadataProfiles, referenceIndex.getMetadataProfiles(), "Metadata Profile", alerts);
            validateProfiles(metricProfiles, referenceIndex.getMetricProfiles(), "Metric Profile", alerts);
            validateSimpleItems(layers, referenceIndex.getLayers(), "Layer", alerts);
        } else {
            alerts.add("Skipping validation: Reference index could not be loaded.");
        }

        result.setAlerts(alerts);
        return result;
    }

    private List<KruizeProfile> safeGetProfiles(Supplier<List<KruizeProfile>> supplier) {
        try {
            List<KruizeProfile> profiles = supplier.get();
            return profiles != null ? profiles : new ArrayList<>();
        } catch (Exception e) {
            LOG.error("Failed to fetch profiles: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<ProfileScanResult.ParsedProfile> mapProfiles(List<KruizeProfile> rawList) {
        if (rawList == null)
            return new ArrayList<>();
        return rawList.stream()
                .map(p -> new ProfileScanResult.ParsedProfile(
                        p.getMetadata() != null ? p.getMetadata().getName() : p.getName(),
                        p.getProfileVersion()))
                .collect(Collectors.toList());
    }

    private ProfileScanResult fetchReferenceIndex() {
        if (isLocal()) {
            try {
                String resourcePath = "configs/config-master-index.json";
                LOG.infof("Reading local reference index from classpath: %s", resourcePath);
                try (var inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
                    if (inputStream != null) {
                        ObjectMapper mapper = new ObjectMapper();
                        return mapper.readValue(inputStream, ProfileScanResult.class);
                    } else {
                        LOG.errorf("Local reference index resource not found: %s", resourcePath);
                        return null;
                    }
                }
            } catch (Exception e) {
                LOG.error("Local Index Read Error: " + e.getMessage());
                return null;
            }
        } else {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(referenceUrl))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    ObjectMapper mapper = new ObjectMapper();
                    return mapper.readValue(response.body(), ProfileScanResult.class);
                } else {
                    LOG.error("GitHub returned status: " + response.statusCode());
                    return null;
                }
            } catch (Exception e) {
                LOG.error("Failed to fetch reference JSON: " + e.getMessage());
                return null;
            }
        }
    }

    private boolean isLocal() {
        // Simple check if referenceUrl is set to "local".
        // Note: ProfileInstallerService also checks base-url, but referencing logic
        // usually relies on referenceUrl
        return referenceUrl != null && referenceUrl.equalsIgnoreCase("local");
    }

    // Validate Profiles (Name + Version)
    private void validateProfiles(List<KruizeProfile> installed,
            List<ProfileScanResult.ParsedProfile> required,
            String type,
            List<String> alerts) {
        if (required == null)
            return;
        if (installed == null)
            installed = new ArrayList<>();

        Map<String, String> installedMap = installed.stream()
                .collect(Collectors.toMap(
                        p -> p.getMetadata() != null ? p.getMetadata().getName()
                                : (p.getName() != null ? p.getName() : "unknown"),
                        p -> p.getProfileVersion() != null ? p.getProfileVersion() : "",
                        (v1, v2) -> v1));

        for (ProfileScanResult.ParsedProfile req : required) {
            if (!installedMap.containsKey(req.getName())) {
                alerts.add(String.format("%s profile '%s' is not installed (required version: %s).",
                        type, req.getName(), req.getProfileVersion()));
            } else {
                // Default req version to v1.0 if null
                String reqVersion = normalizeVersion(req.getProfileVersion());
                String installedVersion = normalizeVersion(installedMap.get(req.getName()));

                if (!installedVersion.equals(reqVersion)) {
                    alerts.add(String.format("%s profile '%s' version mismatch. Installed: %s, Required: %s.",
                            type, req.getName(), installedVersion, reqVersion));
                }
            }
        }
    }

    private String normalizeVersion(String v) {
        if (v == null || v.isEmpty())
            return "v1.0";
        if (!v.startsWith("v"))
            return "v" + v;
        return v;
    }

    // Validate Layers (Name only)
    private void validateSimpleItems(List<KruizeProfile> installed,
            List<ProfileScanResult.ParsedProfile> required,
            String type,
            List<String> alerts) {
        if (required == null)
            return;
        if (installed == null)
            installed = new ArrayList<>();

        List<String> installedNames = installed.stream()
                .map(p -> p.getName() != null ? p.getName()
                        : (p.getMetadata() != null ? p.getMetadata().getName() : "unknown"))
                .collect(Collectors.toList());

        for (ProfileScanResult.ParsedProfile req : required) {
            if (!installedNames.contains(req.getName())) {
                alerts.add(String.format("%s '%s' is not installed.", type, req.getName()));
            }
        }
    }
}
