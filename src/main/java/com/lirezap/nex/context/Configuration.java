package com.lirezap.nex.context;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

/**
 * Configuration loading component.
 *
 * @author Alireza Pourtaghi
 */
public final class Configuration {
    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);

    private final Config config;

    Configuration() {
        // Loads the following (first-listed are higher priority)
        // system properties
        // application.conf (all resources on classpath with this name)
        // application.json (all resources on classpath with this name)
        // application.properties (all resources on classpath with this name)
        // reference.conf (all resources on classpath with this name)
        this.config = ConfigFactory.load();
    }

    public boolean loadBoolean(final String key) {
        var value = config.getBoolean(key);
        logger.trace("{}: {}", key, value);

        return value;
    }

    public int loadInt(final String key) {
        var value = config.getInt(key);
        logger.trace("{}: {}", key, value);

        return value;
    }

    public long loadLong(final String key) {
        var value = config.getLong(key);
        logger.trace("{}: {}", key, value);

        return value;
    }

    public double loadDouble(final String key) {
        var value = config.getDouble(key);
        logger.trace("{}: {}", key, value);

        return value;
    }

    public String loadString(final String key) {
        var value = config.getString(key);
        logger.trace("{}: {}", key, value);

        return value;
    }

    public List<String> loadStringList(final String key) {
        var value = config.getStringList(key);
        logger.trace("{}: {}", key, value);

        return value;
    }

    public Duration loadDuration(final String key) {
        var value = config.getDuration(key);
        logger.trace("{}: {}", key, value);

        return value;
    }

    public long loadMemoryBytes(final String key) {
        var value = config.getMemorySize(key).toBytes();
        logger.trace("{}: {}", key, value);

        return value;
    }

    public Config loadConfig(final String key) {
        return config.getConfig(key);
    }

    public ConfigObject loadObject(final String key) {
        return config.getObject(key);
    }
}
