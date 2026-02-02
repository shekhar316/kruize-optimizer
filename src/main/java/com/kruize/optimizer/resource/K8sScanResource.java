package com.kruize.optimizer.resource;

import com.kruize.optimizer.model.K8sScanResult;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/scan")
public class K8sScanResource {

        @Inject
        KubernetesClient client;

        @Inject
        com.kruize.optimizer.config.TargetLabelConfig targetLabelConfig;

        @Inject
        com.kruize.optimizer.config.KruizeClusterConfig clusterConfig;

        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public K8sScanResult scan(@QueryParam("all") boolean all) {
                K8sScanResult finalResult = new K8sScanResult();
                finalResult.setNamespaces(new ArrayList<>());
                finalResult.setWorkloads(new ArrayList<>());

                // Check if multiple clusters are configured
                if (clusterConfig.clusters().isPresent() && !clusterConfig.clusters().get().isEmpty()) {
                        for (com.kruize.optimizer.config.KruizeClusterConfig.Cluster cluster : clusterConfig.clusters()
                                        .get()) {
                                try (KubernetesClient kClient = createClient(cluster)) {
                                        K8sScanResult clusterResult = scanCluster(kClient, cluster.name(), all);
                                        finalResult.getNamespaces().addAll(clusterResult.getNamespaces());
                                        finalResult.getWorkloads().addAll(clusterResult.getWorkloads());
                                } catch (Exception e) {
                                        e.printStackTrace();
                                        // Log error but continue with other clusters
                                }
                        }
                } else {
                        // Fallback to default local client
                        K8sScanResult clusterResult = scanCluster(client, "local", all);
                        finalResult.setNamespaces(clusterResult.getNamespaces());
                        finalResult.setWorkloads(clusterResult.getWorkloads());
                }

                return finalResult;
        }

        private K8sScanResult scanCluster(KubernetesClient kClient, String clusterName, boolean all) {
                K8sScanResult result = new K8sScanResult();
                List<K8sScanResult.NamespaceInfo> nsResult = new ArrayList<>();
                List<K8sScanResult.WorkloadInfo> allWorkloads = new ArrayList<>();

                // 1. Scan Namespaces
                List<Namespace> namespaces = kClient.namespaces().list().getItems();

                // Create a map for quick lookup of optimized namespaces
                Map<String, Boolean> namespaceOptimizedMap = new HashMap<>();

                namespaces.forEach(ns -> {
                        boolean isOptimized = checkLabel(ns.getMetadata().getLabels());
                        namespaceOptimizedMap.put(ns.getMetadata().getName(), isOptimized);
                        if (all || isOptimized) {
                                nsResult.add(new K8sScanResult.NamespaceInfo(ns.getMetadata().getName(), clusterName,
                                                isOptimized));
                        }
                });

                // 2. Scan Deployments
                List<Deployment> deployments = kClient.apps().deployments().inAnyNamespace().list().getItems();
                for (Deployment d : deployments) {
                        boolean isNamespaceOptimized = namespaceOptimizedMap
                                        .getOrDefault(d.getMetadata().getNamespace(), false);
                        boolean isWorkloadOptimized = checkLabel(d.getMetadata().getLabels());
                        boolean isOptimized = isNamespaceOptimized || isWorkloadOptimized;

                        if (all || isOptimized) {
                                List<K8sScanResult.ContainerInfo> containers = d.getSpec().getTemplate().getSpec()
                                                .getContainers()
                                                .stream()
                                                .map(c -> new K8sScanResult.ContainerInfo(c.getName(), c.getImage()))
                                                .collect(Collectors.toList());

                                allWorkloads.add(new K8sScanResult.WorkloadInfo(
                                                d.getMetadata().getName(),
                                                d.getMetadata().getNamespace(),
                                                clusterName,
                                                "Deployment",
                                                isOptimized,
                                                containers,
                                                d.getMetadata().getLabels()));
                        }
                }

                // 3. Scan StatefulSets
                List<StatefulSet> statefulSets = kClient.apps().statefulSets().inAnyNamespace().list().getItems();
                for (StatefulSet s : statefulSets) {
                        boolean isNamespaceOptimized = namespaceOptimizedMap
                                        .getOrDefault(s.getMetadata().getNamespace(), false);
                        boolean isWorkloadOptimized = checkLabel(s.getMetadata().getLabels());
                        boolean isOptimized = isNamespaceOptimized || isWorkloadOptimized;

                        if (all || isOptimized) {
                                List<K8sScanResult.ContainerInfo> containers = s.getSpec().getTemplate().getSpec()
                                                .getContainers()
                                                .stream()
                                                .map(c -> new K8sScanResult.ContainerInfo(c.getName(), c.getImage()))
                                                .collect(Collectors.toList());

                                allWorkloads.add(new K8sScanResult.WorkloadInfo(
                                                s.getMetadata().getName(),
                                                s.getMetadata().getNamespace(),
                                                clusterName,
                                                "StatefulSet",
                                                isOptimized,
                                                containers,
                                                s.getMetadata().getLabels()));
                        }
                }

                // 4. Scan ReplicaSets
                List<ReplicaSet> replicaSets = kClient.apps().replicaSets().inAnyNamespace().list().getItems();
                for (ReplicaSet r : replicaSets) {
                        boolean isNamespaceOptimized = namespaceOptimizedMap
                                        .getOrDefault(r.getMetadata().getNamespace(), false);
                        boolean isWorkloadOptimized = checkLabel(r.getMetadata().getLabels());
                        boolean isOptimized = isNamespaceOptimized || isWorkloadOptimized;

                        if (all || isOptimized) {
                                List<K8sScanResult.ContainerInfo> containers = r.getSpec().getTemplate().getSpec()
                                                .getContainers()
                                                .stream()
                                                .map(c -> new K8sScanResult.ContainerInfo(c.getName(), c.getImage()))
                                                .collect(Collectors.toList());

                                allWorkloads.add(new K8sScanResult.WorkloadInfo(
                                                r.getMetadata().getName(),
                                                r.getMetadata().getNamespace(),
                                                clusterName,
                                                "ReplicaSet",
                                                isOptimized,
                                                containers,
                                                r.getMetadata().getLabels()));
                        }
                }

                result.setNamespaces(nsResult);
                result.setWorkloads(allWorkloads);
                return result;
        }

        private KubernetesClient createClient(com.kruize.optimizer.config.KruizeClusterConfig.Cluster cluster) {
                io.fabric8.kubernetes.client.ConfigBuilder configBuilder = new io.fabric8.kubernetes.client.ConfigBuilder();

                cluster.url().ifPresent(configBuilder::withMasterUrl);

                if (cluster.authType().isPresent() && "TOKEN".equalsIgnoreCase(cluster.authType().get())) {
                        cluster.token().ifPresent(configBuilder::withOauthToken);
                }

                cluster.caCert().ifPresent(configBuilder::withCaCertData);

                return new io.fabric8.kubernetes.client.KubernetesClientBuilder().withConfig(configBuilder.build())
                                .build();
        }

        @POST
        @Path("/enable-autotune")
        @Produces(MediaType.APPLICATION_JSON)
        public Response enableAutotune(@QueryParam("namespace") String namespace,
                        @QueryParam("deployment") String deploymentName) {
                if (namespace == null || namespace.isEmpty()) {
                        return Response.status(Response.Status.BAD_REQUEST)
                                        .entity("{\"error\": \"Namespace is required\"}").build();
                }

                try {
                        if (deploymentName != null && !deploymentName.isEmpty()) {
                                // Label Deployment
                                Deployment deployment = client.apps().deployments().inNamespace(namespace)
                                                .withName(deploymentName).get();
                                if (deployment == null) {
                                        return Response.status(Response.Status.NOT_FOUND).entity(
                                                        "{\"error\": \"Deployment not found: " + deploymentName + "\"}")
                                                        .build();
                                }

                                client.apps().deployments().inNamespace(namespace).withName(deploymentName).edit(d -> {
                                        if (d.getMetadata().getLabels() == null) {
                                                d.getMetadata().setLabels(new HashMap<>());
                                        }
                                        d.getMetadata().getLabels().put("kruize/autotune", "enabled");
                                        return d;
                                });
                                return Response.ok("{\"message\": \"Deployment " + deploymentName
                                                + " labeled successfully\"}").build();
                        } else {
                                // Label Namespace
                                Namespace ns = client.namespaces().withName(namespace).get();
                                if (ns == null) {
                                        return Response.status(Response.Status.NOT_FOUND).entity(
                                                        "{\"error\": \"Namespace not found: " + namespace + "\"}")
                                                        .build();
                                }

                                client.namespaces().withName(namespace).edit(n -> {
                                        if (n.getMetadata().getLabels() == null) {
                                                n.getMetadata().setLabels(new HashMap<>());
                                        }
                                        n.getMetadata().getLabels().put("kruize/autotune", "enabled");
                                        return n;
                                });
                                return Response.ok(
                                                "{\"message\": \"Namespace " + namespace + " labeled successfully\"}")
                                                .build();
                        }
                } catch (Exception e) {
                        e.printStackTrace();
                        return Response.serverError()
                                        .entity("{\"error\": \"Error labeling resource: " + e.getMessage() + "\"}")
                                        .build();
                }
        }

        private boolean checkLabel(Map<String, String> resourceLabels) {
                if (resourceLabels == null || resourceLabels.isEmpty()) {
                        return false;
                }
                // Check if any configured target label matches the resource labels
                for (Map.Entry<String, String> target : targetLabelConfig.getTargetLabels().entrySet()) {
                        if (target.getValue().equals(resourceLabels.get(target.getKey()))) {
                                return true;
                        }
                }
                return false;
        }
}
