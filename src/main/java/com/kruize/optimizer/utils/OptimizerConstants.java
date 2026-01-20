package com.kruize.optimizer.utils;

public class OptimizerConstants {

    // contains constants related to Kruize API Endpoints
    public class KruizeClientConstants {

        // query params and other general constants
        public static final String VERBOSE = "verbose";
        public static final String NAME = "name";

        // list APIs
        public static final String LIST_DATASOURCE_ENDPOINT = "/datasources";
        public static final String LIST_METADATA_PROFILE_ENDPOINT = "/listMetadataProfiles";
        public static final String LIST_METRIC_PROFILE_ENDPOINT = "/listMetricProfiles";
        public static final String LIST_LAYERS_ENDPOINT = "/listLayers";
        public static final String LIST_EXPERIMENTS_ENDPOINT = "/listExperiments";

        // create APIs
        public static final String CREATE_METADATA_PROFILE_ENDPOINT = "/createMetadataProfile";
        public static final String CREATE_METRIC_PROFILE_ENDPOINT = "/createMetricProfile";
        public static final String CREATE_LAYERS_ENDPOINT = "/createLayer";

        // update APIs
        public static final String UPDATE_METADATA_PROFILE_ENDPOINT = "/updateMetadataProfile";
        public static final String UPDATE_METRIC_PROFILE_ENDPOINT = "/updateMetricProfile";

        // bulk APIs
        public static final String BULK_ENDPOINT = "/bulk";
        public static final String JOB_ID = "job_id";
    }
}
