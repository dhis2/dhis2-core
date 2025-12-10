/*
 * Copyright (c) 2004-2025, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.test.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import javax.sql.DataSource;
import org.hisp.dhis.config.DataSourceConfig;
import org.hisp.dhis.encryption.EncryptionStatus;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.external.conf.model.GoogleAccessToken;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Configuration
public class TrackerReadOnlyDataSourceTestConfig {

  @Bean(destroyMethod = "stop")
  @SuppressWarnings({"resource"})
  public PostgreSQLContainer<?> postgisContainer() {
    PostgreSQLContainer<?> container =
        new PostgreSQLContainer<>(
                DockerImageName.parse("postgis/postgis:16-3.5-alpine")
                    .asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("testdb")
            .withUsername("user")
            .withPassword("password")
            .withTmpFs(Map.of("/testtmpfs", "rw"));
    container.start();
    return container;
  }

  @Bean
  public DhisConfigurationProvider dhisConfigurationProvider(PostgreSQLContainer<?> container) {
    return new DhisConfigurationProvider() {
      @Override
      public String getProperty(ConfigurationKey key) {
        return switch (key) {
          case READ_REPLICA_CONNECTION_URL, CONNECTION_URL -> container.getJdbcUrl();
          case CONNECTION_USERNAME -> container.getUsername();
          case CONNECTION_PASSWORD -> container.getPassword();
          case DB_POOL_TYPE -> "HIKARI";
          case CONNECTION_DRIVER_CLASS -> "org.postgresql.Driver";
          case CONNECTION_POOL_TIMEOUT,
              CONNECTION_POOL_VALIDATION_TIMEOUT,
              CONNECTION_POOL_MAX_IDLE_TIME,
              CONNECTION_POOL_KEEP_ALIVE_TIME_SECONDS,
              CONNECTION_POOL_MAX_LIFETIME_SECONDS ->
              "60000";
          case CONNECTION_POOL_MAX_SIZE, CONNECTION_POOL_MIN_IDLE -> "5";
          case CONNECTION_POOL_TEST_QUERY -> "SELECT 1";
          default -> null;
        };
      }

      @Override
      public Properties getProperties() {
        Properties props = new Properties();
        props.setProperty(
            ConfigurationKey.READ_REPLICA_CONNECTION_URL.name(), container.getJdbcUrl());
        props.setProperty(ConfigurationKey.CONNECTION_USERNAME.name(), container.getUsername());
        props.setProperty(ConfigurationKey.CONNECTION_PASSWORD.name(), container.getPassword());
        props.setProperty(ConfigurationKey.DB_POOL_TYPE.name(), "HIKARI");
        props.setProperty(ConfigurationKey.CONNECTION_DRIVER_CLASS.name(), "org.postgresql.Driver");
        return props;
      }

      @Override
      public String getPropertyOrDefault(ConfigurationKey key, String defaultValue) {
        return defaultValue;
      }

      @Override
      public boolean hasProperty(ConfigurationKey key) {
        return false;
      }

      @Override
      public Optional<GoogleCredential> getGoogleCredential() {
        return Optional.empty();
      }

      @Override
      public Optional<GoogleAccessToken> getGoogleAccessToken() {
        return Optional.empty();
      }

      @Override
      public String getConnectionUrl() {
        return container.getJdbcUrl();
      }

      @Override
      public boolean isReadOnlyMode() {
        return true;
      }

      @Override
      public boolean isClusterEnabled() {
        return false;
      }

      @Override
      public String getServerBaseUrl() {
        return "";
      }

      @Override
      public List<String> getMetaDataSyncRemoteServersAllowed() {
        return List.of();
      }

      @Override
      public boolean isMetaDataSyncRemoteServerAllowed(String url) {
        return false;
      }

      @Override
      public boolean isLdapConfigured() {
        return false;
      }

      @Override
      public EncryptionStatus getEncryptionStatus() {
        return null;
      }

      @Override
      public boolean isAnalyticsDatabaseConfigured() {
        return false;
      }

      @Override
      public Map<String, Serializable> getConfigurationsAsMap() {
        return Map.of();
      }
    };
  }

  @Bean
  public DataSource trackerReadOnlyDataSource(DhisConfigurationProvider config) {
    return new DataSourceConfig(null).readOnlyDataSource(config, null);
  }
}
