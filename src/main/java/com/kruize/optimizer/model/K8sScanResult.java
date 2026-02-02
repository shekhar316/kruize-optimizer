package com.kruize.optimizer.model;

import java.util.ArrayList;
import java.util.List;

public class K8sScanResult {
    private List<NamespaceInfo> namespaces = new ArrayList<>();
    private List<WorkloadInfo> workloads = new ArrayList<>();

    public List<NamespaceInfo> getNamespaces() {
        return namespaces;
    }

    public void setNamespaces(List<NamespaceInfo> namespaces) {
        this.namespaces = namespaces;
    }

    public List<WorkloadInfo> getWorkloads() {
        return workloads;
    }

    public void setWorkloads(List<WorkloadInfo> workloads) {
        this.workloads = workloads;
    }

    public static class NamespaceInfo {
        private String name;
        private String clusterName;
        private boolean kruizeOptimized;

        public NamespaceInfo() {
        }

        public NamespaceInfo(String name, String clusterName, boolean kruizeOptimized) {
            this.name = name;
            this.clusterName = clusterName;
            this.kruizeOptimized = kruizeOptimized;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getClusterName() {
            return clusterName;
        }

        public void setClusterName(String clusterName) {
            this.clusterName = clusterName;
        }

        public boolean isKruizeOptimized() {
            return kruizeOptimized;
        }

        public void setKruizeOptimized(boolean kruizeOptimized) {
            this.kruizeOptimized = kruizeOptimized;
        }
    }

    public static class WorkloadInfo {
        private String name;
        private String namespace;
        private String clusterName;
        private String type; // Deployment, StatefulSet, ReplicaSet
        private boolean kruizeOptimized;
        private List<ContainerInfo> containers = new ArrayList<>();
        private java.util.Map<String, String> labels;

        public WorkloadInfo() {
        }

        public WorkloadInfo(String name, String namespace, String clusterName, String type, boolean kruizeOptimized,
                List<ContainerInfo> containers, java.util.Map<String, String> labels) {
            this.name = name;
            this.namespace = namespace;
            this.clusterName = clusterName;
            this.type = type;
            this.kruizeOptimized = kruizeOptimized;
            this.containers = containers;
            this.labels = labels;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getNamespace() {
            return namespace;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }

        public String getClusterName() {
            return clusterName;
        }

        public void setClusterName(String clusterName) {
            this.clusterName = clusterName;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public boolean isKruizeOptimized() {
            return kruizeOptimized;
        }

        public void setKruizeOptimized(boolean kruizeOptimized) {
            this.kruizeOptimized = kruizeOptimized;
        }

        public List<ContainerInfo> getContainers() {
            return containers;
        }

        public void setContainers(List<ContainerInfo> containers) {
            this.containers = containers;
        }

        public java.util.Map<String, String> getLabels() {
            return labels;
        }

        public void setLabels(java.util.Map<String, String> labels) {
            this.labels = labels;
        }
    }

    public static class ContainerInfo {
        private String name;
        private String image;

        public ContainerInfo() {
        }

        public ContainerInfo(String name, String image) {
            this.name = name;
            this.image = image;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getImage() {
            return image;
        }

        public void setImage(String image) {
            this.image = image;
        }
    }
}
