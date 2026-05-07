package com.mtg.config;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@ApplicationScoped
public class AppLifecycleLogger {
    private static final Logger LOG = Logger.getLogger(AppLifecycleLogger.class);

    @ConfigProperty(name = "quarkus.application.name", defaultValue = "mtg-deck-manager-api")
    String applicationName;

    @ConfigProperty(name = "quarkus.application.version", defaultValue = "unknown")
    String applicationVersion;

    void onStart(@Observes StartupEvent event) {
        LOG.infov(
                "event=api.startup.completed timestamp={0} application={1} version={2} message=\"API pronta para receber requisicoes.\"",
                OffsetDateTime.now(ZoneOffset.UTC),
                applicationName,
                applicationVersion
        );
    }

    void onStop(@Observes ShutdownEvent event) {
        LOG.infov(
                "event=api.shutdown.completed timestamp={0} application={1} version={2} message=\"API finalizada.\"",
                OffsetDateTime.now(ZoneOffset.UTC),
                applicationName,
                applicationVersion
        );
    }
}
