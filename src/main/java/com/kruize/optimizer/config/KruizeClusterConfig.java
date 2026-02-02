package com.kruize.optimizer.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;
import java.util.List;
import java.util.Optional;

@ConfigMapping(prefix = "kruize")
public interface KruizeClusterConfig {

    Optional<List<Cluster>> clusters();

    Scan scan();

    Job job();

    Profile profile();

    Target target();

    Optional<Webhook> webhook();

    interface Cluster {
        String name();

        Optional<String> url();

        Optional<String> authType();

        Optional<String> token();

        Optional<String> caCert();

        String datasource();
    }

    interface Scan {
        String interval();
    }

    interface Job {
        Polling polling();

        interface Polling {
            String interval();
        }
    }

    interface Profile {
        Git git();

        interface Git {
            @WithName("reference-url")
            String referenceUrl();

            @WithName("base-url")
            String baseUrl();
        }
    }

    interface Target {
        Labels labels();

        interface Labels {
            int limit();

            String json();
        }
    }

    interface Webhook {
        String url();
    }
}
