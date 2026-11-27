/*
 * Copyright and related rights waived via CC0
 *
 * You should have received a copy of the CC0 legalcode along with this
 * work. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package com.zaxxer.hikaricp.test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.SQLExceptionOverride;
import com.zaxxer.hikaricp.test.driver.CustomDriver;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;

public class HikariConfigTest {
    @Test
    public void settersLoadConfiguredClassesFromHikariConfigClassLoaderWhenContextClassLoaderIsUnavailable() {
        HikariConfig config = new HikariConfig();
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            Thread.currentThread().setContextClassLoader(null);
            config.setDriverClassName(CustomDriver.class.getName());
            config.setExceptionOverrideClassName(TestExceptionOverride.class.getName());
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }

        assertThat(config.getDriverClassName()).isEqualTo(CustomDriver.class.getName());
        assertThat(config.getExceptionOverrideClassName()).isEqualTo(TestExceptionOverride.class.getName());
        assertThat(config.getExceptionOverride()).isInstanceOf(TestExceptionOverride.class);
    }

    @Test
    public void constructorLoadsConfigurationFromClasspathResource() {
        HikariConfig config = new HikariConfig("/hikari-config-test.properties");

        assertThatConfigurationLoaded(config);
    }

    @Test
    public void constructorLoadsConfigurationFromClasspathResourceThroughClassLoaderFallback() {
        HikariConfig config = new HikariConfig("hikari-config-test.properties");

        assertThatConfigurationLoaded(config);
    }

    @Test
    void settersCanBeInvokedThroughDynamicReflection() throws Exception {
        Properties props = new Properties();
        try (var is = getClass().getClassLoader().getResourceAsStream("hikari-config-test.properties")) {
            if (is == null) {
                throw new IllegalStateException("Cannot find property file: hikari-config-test.properties");
            }
            props.load(is);
        }

        String className = "com.zaxxer.hikari.HikariDataSource";
        HikariDataSource config = (HikariDataSource) Class.forName(className)
                .getDeclaredConstructor()
                .newInstance();

        invokeDynamicSetter(config, "jdbcUrl", String.class, props.getProperty("jdbcUrl"));
        invokeDynamicSetter(config, "maximumPoolSize", int.class, Integer.parseInt(props.getProperty("maximumPoolSize")));
        invokeDynamicSetter(config, "minimumIdle", int.class, Integer.parseInt(props.getProperty("minimumIdle")));
        invokeDynamicSetter(config, "username", String.class, props.getProperty("username"));
        invokeDynamicSetter(config, "password", String.class, props.getProperty("password"));

        invokeDynamicSetter(config, "driverClassName", String.class, props.getProperty("driverClassName"));
        invokeDynamicSetter(config, "catalog", String.class, props.getProperty("catalog"));
        invokeDynamicSetter(config, "connectionTimeout", long.class, Long.parseLong(props.getProperty("connectionTimeout")));
        invokeDynamicSetter(config, "idleTimeout", long.class, Long.parseLong(props.getProperty("idleTimeout")));
        invokeDynamicSetter(config, "leakDetectionThreshold", long.class, Long.parseLong(props.getProperty("leakDetectionThreshold")));
        invokeDynamicSetter(config, "maxLifetime", long.class, Long.parseLong(props.getProperty("maxLifetime")));
        invokeDynamicSetter(config, "validationTimeout", long.class, Long.parseLong(props.getProperty("validationTimeout")));

        assertThatConfigurationLoaded(config);
        config.close();
    }

    private void invokeDynamicSetter(Object target, String propertyName, Class<?> paramType, Object value) throws Exception {
        String setterName = "set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
        target.getClass().getMethod(setterName, paramType).invoke(target, value);
    }

    private static void assertThatConfigurationLoaded(HikariConfig config) {
        assertThat(config.getJdbcUrl()).isEqualTo("jdbc:test:from-resource");
        assertThat(config.getMaximumPoolSize()).isEqualTo(4);
        assertThat(config.getMinimumIdle()).isEqualTo(1);
        assertThat(config.getUsername()).isEqualTo("resource-user");
        assertThat(config.getPassword()).isEqualTo("resource-password");
        assertThat(config.getDriverClassName()).isEqualTo("org.h2.Driver");
        assertThat(config.getCatalog()).isEqualTo("resource-catalog");
        assertThat(config.getConnectionTimeout()).isEqualTo(500);
        assertThat(config.getIdleTimeout()).isEqualTo(100);
        assertThat(config.getLeakDetectionThreshold()).isEqualTo(11000);
        assertThat(config.getMaxLifetime()).isEqualTo(2800000);
        assertThat(config.getValidationTimeout()).isEqualTo(500);
    }

    public static final class TestExceptionOverride implements SQLExceptionOverride {
        public TestExceptionOverride() {
        }
    }
}
