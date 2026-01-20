package com.kruize.optimizer.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.*;

@ApplicationScoped
public class TargetLabelConfig {
    private static final Logger LOG = Logger.getLogger(TargetLabelConfig.class);

    @ConfigProperty(name = "kruize.target.labels.json")
    String targetLabelsJson;

    @ConfigProperty(name = "kruize.target.labels.limit", defaultValue = "5")
    int labelLimit;

    private Map<String, String> targetLabels = new HashMap<>();

    @PostConstruct
    void init() {
        if (targetLabelsJson == null || targetLabelsJson.isBlank()) {
            LOG.warn("No target labels configured. Defaulting to 'kruize/autotune=enabled'");
            targetLabels.put("kruize/autotune", "enabled");
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode rootNode = mapper.readTree(targetLabelsJson);
            if (rootNode.isObject()) {
                targetLabels = mapper.convertValue(rootNode, new TypeReference<Map<String, String>>() {
                });
            } else if (rootNode.isArray()) {
                // if array of labels is attached, then format the key value pairs
                List<String> labelList = mapper.convertValue(rootNode, new TypeReference<List<String>>() {
                });
                for (String label : labelList) {
                    String[] parts = label.split("=", 2);
                    if (parts.length == 2) {
                        targetLabels.put(parts[0].trim(), parts[1].trim());
                    } else {
                        LOG.warnf("Invalid label format: %s. Expected 'key=value'.", label);
                    }
                }
            } else {
                LOG.error("Invalid JSON format for kruize.target.labels.json. Must be an Object or Array.");
            }
        } catch (JsonProcessingException e) {
            LOG.error("Failed to parse kruize.target.labels.json", e);
            // Fallback
            if (targetLabels.isEmpty()) {
                targetLabels.put("kruize/autotune", "enabled");
            }
        }

        // Apply Limit
        if (targetLabels.size() > labelLimit) {
            LOG.warnf("Label count %d exceeds limit %d. Truncating to first %d.", targetLabels.size(), labelLimit,
                    labelLimit);
            Map<String, String> limited = new LinkedHashMap<>();
            int count = 0;
            for (Map.Entry<String, String> entry : targetLabels.entrySet()) {
                if (count >= labelLimit)
                    break;
                limited.put(entry.getKey(), entry.getValue());
                count++;
            }
            targetLabels = limited;
        }

        LOG.infof("Loaded Target Labels: %s", targetLabels);
    }

    public Map<String, String> getTargetLabels() {
        return Collections.unmodifiableMap(targetLabels);
    }
}
