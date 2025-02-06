/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.system.leader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import org.hisp.dhis.encryption.EncryptionStatus;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.external.conf.model.GoogleAccessToken;
import org.hisp.dhis.leader.election.NoOpLeaderManager;
import org.junit.jupiter.api.Test;

/**
 * @author Ameen Mohamed
 */
class LeaderManagerTest {
  static class TestDhisConfigurationProvider implements DhisConfigurationProvider {

    protected Properties properties;

    public TestDhisConfigurationProvider(Properties properties) {
      this.properties = properties;
    }

    @Override
    public Properties getProperties() {
      return this.properties;
    }

    @Override
    public String getProperty(ConfigurationKey key) {
      return getPropertyOrDefault(key, key.getDefaultValue());
    }

    @Override
    public String getPropertyOrDefault(ConfigurationKey key, String defaultValue) {
      for (String alias : key.getAliases()) {
        if (properties.contains(alias)) {
          return properties.getProperty(alias);
        }
      }

      return properties.getProperty(key.getKey(), defaultValue);
    }

    @Override
    public boolean hasProperty(ConfigurationKey key) {
      return false;
    }

    @Override
    public boolean isEnabled(ConfigurationKey key) {
      return DhisConfigurationProvider.isOn(getProperty(key));
    }

    @Override
    public boolean isDisabled(ConfigurationKey key) {
      return DhisConfigurationProvider.isOff(getProperty(key));
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
    public List<String> getRemoteServersAllowed() {
      return List.of();
    }

    @Override
    public boolean remoteServerIsInAllowedList(String url) {
      return false;
    }

    @Override
    public boolean isLdapConfigured() {
      return false;
    }

    @Override
    public boolean isAnalyticsDatabaseConfigured() {
      return false;
    }

    @Override
    public EncryptionStatus getEncryptionStatus() {
      return EncryptionStatus.OK;
    }

    @Override
    public Map<String, Serializable> getConfigurationsAsMap() {
      return Map.of();
    }
  }

  @Test
  void testNodeInfo() {
    Properties properties = new Properties();
    properties.put(ConfigurationKey.NODE_ID, "1");
    TestDhisConfigurationProvider dhisConfigurationProvider =
        new TestDhisConfigurationProvider(properties);
    NoOpLeaderManager leaderManager = new NoOpLeaderManager(dhisConfigurationProvider);

    assertNotNull(leaderManager.getCurrentNodeUuid());
    assertNotNull(leaderManager.getLeaderNodeUuid());
    assertEquals(leaderManager.getCurrentNodeUuid(), leaderManager.getLeaderNodeUuid());
    assertTrue(leaderManager.isLeader());
  }
}
