/*
 * ISC License
 *
 * Copyright (c) 2025, Alireza Pourtaghi <lirezap@protonmail.com>
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package software.openex.oms.context;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.List;

import static com.typesafe.config.ConfigFactory.load;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Configuration loading component.
 *
 * @author Alireza Pourtaghi
 */
public final class Configuration {
    private static final Logger logger = getLogger(Configuration.class);

    private final Config config;

    Configuration() {
        // Loads the following (first-listed are higher priority)
        // system properties
        // application.conf (all resources on classpath with this name)
        // application.json (all resources on classpath with this name)
        // application.properties (all resources on classpath with this name)
        // reference.conf (all resources on classpath with this name)
        this.config = load();
    }

    public boolean loadBoolean(final String key) {
        final var value = config.getBoolean(key);
        logger.trace("{}: {}", key, value);

        return value;
    }

    public int loadInt(final String key) {
        final var value = config.getInt(key);
        logger.trace("{}: {}", key, value);

        return value;
    }

    public long loadLong(final String key) {
        final var value = config.getLong(key);
        logger.trace("{}: {}", key, value);

        return value;
    }

    public double loadDouble(final String key) {
        final var value = config.getDouble(key);
        logger.trace("{}: {}", key, value);

        return value;
    }

    public String loadString(final String key) {
        final var value = config.getString(key);
        logger.trace("{}: {}", key, value);

        return value;
    }

    public List<String> loadStringList(final String key) {
        final var value = config.getStringList(key);
        logger.trace("{}: {}", key, value);

        return value;
    }

    public Duration loadDuration(final String key) {
        final var value = config.getDuration(key);
        logger.trace("{}: {}", key, value);

        return value;
    }

    public long loadMemoryBytes(final String key) {
        final var value = config.getMemorySize(key).toBytes();
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
