package com.kruize.optimizer.service;

import com.kruize.optimizer.client.KruizeClient;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.scheduler.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.*;
import com.kruize.optimizer.config.KruizeClusterConfig;

@ApplicationScoped
public class WorkloadAutomatorService {

    private static final Logger LOG = Logger.getLogger(WorkloadAutomatorService.class);

    @Inject
    com.kruize.optimizer.config.TargetLabelConfig targetLabelConfig;

    @Inject
    KubernetesClient client;

    @Inject
    @RestClient
    KruizeClient kruizeClient;

    @Inject
    ProfileInstallerService profileInstallerService;

    @Inject
    MeterRegistry registry;

    private Counter totalJobsCreated;
    private Counter totalExperimentsCreated;
    private Counter totalExperimentsProcessed;
    private Counter totalExperimentsUnique;

    @PostConstruct
    void init() {
        totalJobsCreated = registry.counter("total_jobs_created");
        totalExperimentsCreated = registry.counter("total_experiments_created");
        totalExperimentsProcessed = registry.counter("total_experiments_processed");
        totalExperimentsUnique = registry.counter("total_experiments_unique");
    }

    private final java.util.Set<String> completedJobs = java.util.concurrent.ConcurrentHashMap.newKeySet();

    @org.eclipse.microprofile.config.inject.ConfigProperty(name = "kruize.webhook.url", defaultValue = "http://localhost:8080/webhook")
    String webhookUrl;

    @Inject
    KruizeClusterConfig clusterConfig;

    @Scheduled(every = "${kruize.scan.interval:5m}")
    public void scanAndRegisterWorkloads() {
        LOG.info("Starting scheduled workload scan...");

        // Ensure profiles are installed (locally)
        try {
            profileInstallerService.checkAndInstallProfiles();
        } catch (Exception e) {
            LOG.error("Failed to check/install profiles: " + e.getMessage());
        }

        // Check if multiple clusters are configured
        if (clusterConfig.clusters().isPresent() && !clusterConfig.clusters().get().isEmpty()) {
            for (KruizeClusterConfig.Cluster cluster : clusterConfig.clusters().get()) {
                LOG.infof("Processing cluster: %s", cluster.name());
                try (KubernetesClient kClient = createClient(cluster)) {
                    scanAndRegister(kClient, cluster.name(), cluster.datasource());
                } catch (Exception e) {
                    LOG.errorf("Failed to process cluster %s: %s", cluster.name(), e.getMessage());
                }
            }
        } else {
            // Fallback to default local client
            LOG.info("No external clusters configured. Using default local client.");
            scanAndRegister(client, "local", "prometheus-1");
        }
    }

    private KubernetesClient createClient(KruizeClusterConfig.Cluster cluster) {
        io.fabric8.kubernetes.client.ConfigBuilder configBuilder = new io.fabric8.kubernetes.client.ConfigBuilder();

        cluster.url().ifPresent(configBuilder::withMasterUrl);

        if (cluster.authType().isPresent() && "TOKEN".equalsIgnoreCase(cluster.authType().get())) {
            cluster.token().ifPresent(configBuilder::withOauthToken);
        }

        cluster.caCert().ifPresent(configBuilder::withCaCertData);

        return new io.fabric8.kubernetes.client.KubernetesClientBuilder().withConfig(configBuilder.build()).build();
    }

    private void scanAndRegister(KubernetesClient kClient, String clusterName, String datasource) {
        Set<String> namespaces = new HashSet<>();
        Set<String> workloadNames = new HashSet<>();

        try {
            // Iterate over each label configuration
            for (Map.Entry<String, String> entry : targetLabelConfig.getTargetLabels().entrySet()) {
                String labelKey = entry.getKey();
                String labelValue = entry.getValue();

                LOG.debugf("[%s] Scanning for label: %s=%s", clusterName, labelKey, labelValue);

                // 1. Scan Namespaces with the label
                List<Namespace> labeledNamespaces = kClient.namespaces().withLabel(labelKey, labelValue).list()
                        .getItems();
                for (Namespace ns : labeledNamespaces) {
                    String nsName = ns.getMetadata().getName();
                    LOG.debugf("[%s] Found labeled namespace: %s", clusterName, nsName);

                    // Add all Deployments in this namespace
                    kClient.apps().deployments().inNamespace(nsName).list().getItems().forEach(d -> {
                        namespaces.add(d.getMetadata().getNamespace());
                        workloadNames.add(d.getMetadata().getName());
                    });

                    // Add all StatefulSets in this namespace
                    kClient.apps().statefulSets().inNamespace(nsName).list().getItems().forEach(s -> {
                        namespaces.add(s.getMetadata().getNamespace());
                        workloadNames.add(s.getMetadata().getName());
                    });

                    // Add all ReplicaSets in this namespace
                    kClient.apps().replicaSets().inNamespace(nsName).list().getItems().forEach(r -> {
                        namespaces.add(r.getMetadata().getNamespace());
                        workloadNames.add(r.getMetadata().getName());
                    });
                }

                // 2. Scan Workloads explicitly labeled
                // Deployments
                List<Deployment> deploymentList = kClient.apps().deployments().inAnyNamespace()
                        .withLabel(labelKey, labelValue).list().getItems();
                deploymentList.forEach(d -> {
                    namespaces.add(d.getMetadata().getNamespace());
                    workloadNames.add(d.getMetadata().getName());
                });

                // StatefulSets
                List<StatefulSet> statefulSetList = kClient.apps().statefulSets().inAnyNamespace()
                        .withLabel(labelKey, labelValue).list().getItems();
                statefulSetList.forEach(s -> {
                    namespaces.add(s.getMetadata().getNamespace());
                    workloadNames.add(s.getMetadata().getName());
                });

                // ReplicaSets
                List<ReplicaSet> replicaSetList = kClient.apps().replicaSets().inAnyNamespace()
                        .withLabel(labelKey, labelValue).list().getItems();
                replicaSetList.forEach(r -> {
                    namespaces.add(r.getMetadata().getNamespace());
                    workloadNames.add(r.getMetadata().getName());
                });
            }
        } catch (Exception e) {
            LOG.errorf("[%s] Error scanning Kubernetes resources: %s", clusterName, e.getMessage());
            return;
        }

        if (workloadNames.isEmpty()) {
            LOG.infof("[%s] No optimized workloads found.", clusterName);
            return;
        }

        LOG.infof("[%s] Found %d optimized workloads in %d namespaces. Initiating bulk creation...", clusterName,
                workloadNames.size(),
                namespaces.size());

        try {
            // Construct Payload
            Map<String, Object> payload = new HashMap<>();

            Map<String, Object> filter = new HashMap<>();
            Map<String, Object> include = new HashMap<>();
            include.put("namespace", new ArrayList<>(namespaces));
            include.put("workload", new ArrayList<>(workloadNames));
            include.put("containers", List.of("")); // Empty string as per requirement
            filter.put("include", include);

            payload.put("filter", filter);
            payload.put("datasource", datasource);
            payload.put("cluster_name", clusterName);
            payload.put("metadata_profile", "cluster-metadata-local-monitoring");
            payload.put("measurement_duration", "15min");

            if (webhookUrl != null && !webhookUrl.isEmpty()) {
                Map<String, String> webhook = new HashMap<>();
                webhook.put("url", webhookUrl);
                payload.put("webhook", webhook);
            }

            String response = kruizeClient.bulkCreateExperiments(payload);
            LOG.infof("[%s] Bulk API Response: %s", clusterName, response);

            totalJobsCreated.increment();
        } catch (Exception e) {
            LOG.errorf("[%s] Failed to call Bulk API", clusterName, e);
        }
    }

    public void handleWebhook(java.util.List<com.kruize.optimizer.model.WebhookPayload> payloads) {
        for (com.kruize.optimizer.model.WebhookPayload payload : payloads) {
            if (payload.getSummary() != null) {
                com.kruize.optimizer.model.WebhookPayload.Summary summary = payload.getSummary();
                String jobId = summary.getJobId();
                String status = summary.getStatus();
                LOG.infof("Received webhook for Job %s with status %s", jobId, status);

                if ("COMPLETED".equalsIgnoreCase(status)) {
                    if (!completedJobs.add(jobId)) {
                        LOG.infof("Job %s already processed. Skipping metrics update.", jobId);
                        continue;
                    }

                    int total = summary.getTotalExperiments();
                    int processed = summary.getProcessedExperiments();
                    int existing = summary.getExistingExperiments();
                    int unique = total - existing;

                    // Update metrics
                    // Note: We might want to set these values instead of incrementing if they are
                    // absolute for the job.
                    // But usually metrics are monotonically increasing counters for the application
                    // lifetime.
                    // The user said "maintain... total experiments". If new jobs come in, we add to
                    // total.

                    if (total > 0)
                        totalExperimentsCreated.increment(total);
                    if (processed > 0)
                        totalExperimentsProcessed.increment(processed);
                    if (unique > 0)
                        totalExperimentsUnique.increment(unique);

                    LOG.infof("Job %s processed. Total: %d, Processed: %d, Existing: %d, Unique: %d",
                            jobId, total, processed, existing, unique);
                } else {
                    LOG.infof("Job %s status is %s, skipping metric update.", jobId, status);
                }
            }
        }
    }
}
