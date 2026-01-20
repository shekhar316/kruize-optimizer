package com.kruize.optimizer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WebhookPayload {

    @JsonProperty("summary")
    private Summary summary;

    @JsonProperty("webhook")
    private WebhookStatus webhook;

    // Getters and Setters
    public Summary getSummary() {
        return summary;
    }

    public void setSummary(Summary summary) {
        this.summary = summary;
    }

    public WebhookStatus getWebhook() {
        return webhook;
    }

    public void setWebhook(WebhookStatus webhook) {
        this.webhook = webhook;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Summary {
        @JsonProperty("jobID")
        private String jobId;

        @JsonProperty("status")
        private String status;

        @JsonProperty("total_experiments")
        private int totalExperiments;

        @JsonProperty("processed_experiments")
        private int processedExperiments;

        @JsonProperty("existing_experiments")
        private int existingExperiments;

        // Getters and Setters
        public String getJobId() {
            return jobId;
        }

        public void setJobId(String jobId) {
            this.jobId = jobId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public int getTotalExperiments() {
            return totalExperiments;
        }

        public void setTotalExperiments(int totalExperiments) {
            this.totalExperiments = totalExperiments;
        }

        public int getProcessedExperiments() {
            return processedExperiments;
        }

        public void setProcessedExperiments(int processedExperiments) {
            this.processedExperiments = processedExperiments;
        }

        public int getExistingExperiments() {
            return existingExperiments;
        }

        public void setExistingExperiments(int existingExperiments) {
            this.existingExperiments = existingExperiments;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WebhookStatus {
        @JsonProperty("status")
        private String status;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
