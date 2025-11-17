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
package org.hisp.dhis.db.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
import org.hisp.dhis.test.config.TrackerReadOnlyDataSourceTestConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TrackerReadOnlyDataSourceTestConfig.class, DataSourceConfig.class})
public class ReadOnlyDatasourceIntegrationTest {

  @Autowired
  @Qualifier("trackerReadOnlyDataSource")
  private DataSource readOnlyDataSource;

  @Autowired
  @Qualifier("actualDataSource")
  private DataSource actualDataSource;

  @Autowired
  @Qualifier("readOnlyNamedParameterJdbcTemplate")
  private NamedParameterJdbcTemplate readOnlyNamedParameterJdbcTemplate;

  @Autowired
  @Qualifier("primaryReadOnlyJdbcTemplate")
  private JdbcTemplate jdbcTemplate;

  @Test
  void shouldReturnReadOnlyDataSourceWhenReadOnlyUrlIsConfigured() throws SQLException {
    assertNotSame(
        readOnlyDataSource,
        actualDataSource,
        "Read-only DataSource should be distinct from the main DataSource when READ_ONLY_CONNECTION_URL is provided");

    assertReadOnlyConnection(readOnlyNamedParameterJdbcTemplate.getJdbcTemplate().getDataSource());
    assertReadOnlyConnection(jdbcTemplate.getDataSource());
  }

  @Test
  void shouldReturnDefaultDataSourceWhenReadOnlyUrlIsNotConfigured() throws SQLException {
    DhisConfigurationProvider readWriteConfig =
        new DhisConfigurationProvider() {
          @Override
          public String getProperty(ConfigurationKey key) {
            return key == ConfigurationKey.READ_ONLY_CONNECTION_URL ? "" : null;
          }

          @Override
          public Properties getProperties() {
            Properties props = new Properties();
            props.setProperty(ConfigurationKey.READ_ONLY_CONNECTION_URL.name(), "");
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
            return "";
          }

          @Override
          public boolean isReadOnlyMode() {
            return false;
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

    DataSource readWriteDataSource =
        new DataSourceConfig().readOnlyDataSource(readWriteConfig, actualDataSource);

    try (Connection conn = readWriteDataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT 1");
        ResultSet rs = ps.executeQuery()) {
      assertFalse(conn.isReadOnly(), "Connection should not be read-only");
      assertTrue(rs.next(), "ResultSet should have a row");
      assertEquals(1, rs.getInt(1), "Query should return 1");
    }
  }

  private void assertReadOnlyConnection(DataSource dataSource) throws SQLException {
    Assertions.assertNotNull(dataSource, "DataSource should not be null");
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT 1");
        ResultSet rs = ps.executeQuery()) {
      assertTrue(conn.isReadOnly(), "Connection should be read-only");
      assertTrue(rs.next(), "ResultSet should have a row");
      assertEquals(1, rs.getInt(1), "Query should return 1");
    }
  }
}
