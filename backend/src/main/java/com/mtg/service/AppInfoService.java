package com.mtg.service;

import com.mtg.dto.AppInfoResponseDTO;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

@ApplicationScoped
public class AppInfoService {
    private static final String VERSION_PROPERTIES = "version.properties";

    private final Properties buildProperties = loadBuildProperties();

    @ConfigProperty(name = "app.name", defaultValue = "MTG Deck Manager API")
    String defaultName;

    @ConfigProperty(name = "app.version", defaultValue = "0.0.0-local")
    String defaultVersion;

    @ConfigProperty(name = "app.commit", defaultValue = "local")
    String defaultCommit;

    @ConfigProperty(name = "app.branch", defaultValue = "local")
    String defaultBranch;

    @ConfigProperty(name = "app.build-time", defaultValue = "local")
    String defaultBuildTime;

    @ConfigProperty(name = "app.environment", defaultValue = "local")
    String defaultEnvironment;

    @ConfigProperty(name = "app.creator", defaultValue = "Danilo Catapan")
    String defaultCreator;

    @ConfigProperty(name = "app.objective", defaultValue = "Analise e otimizacao de decks Commander com recomendacoes explicaveis.")
    String defaultObjective;

    public AppInfoResponseDTO currentInfo() {
        return new AppInfoResponseDTO(
                value("app.name", defaultName),
                value("app.version", defaultVersion),
                value("app.commit", defaultCommit),
                value("app.branch", defaultBranch),
                value("app.buildTime", defaultBuildTime),
                value("app.environment", defaultEnvironment),
                value("app.creator", defaultCreator),
                value("app.objective", defaultObjective)
        );
    }

    private String value(String key, String fallback) {
        String propertyValue = Optional.ofNullable(buildProperties.getProperty(key))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .orElse(null);
        if (propertyValue != null && !isLocalPlaceholder(propertyValue)) {
            return propertyValue;
        }
        return Optional.ofNullable(fallback)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .orElse(propertyValue);
    }

    private boolean isLocalPlaceholder(String value) {
        return "local".equals(value) || "0.0.0-local".equals(value);
    }

    private Properties loadBuildProperties() {
        Properties properties = new Properties();
        try (InputStream stream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(VERSION_PROPERTIES)) {
            if (stream != null) {
                properties.load(stream);
            }
        } catch (IOException ignored) {
            // Config defaults keep local and test runs resilient when metadata is absent.
        }
        return properties;
    }
}
