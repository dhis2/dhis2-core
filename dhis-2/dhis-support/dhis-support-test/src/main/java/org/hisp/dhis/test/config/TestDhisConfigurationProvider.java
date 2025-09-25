/*
 * Copyright (c) 2004-2022, University of Oslo
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
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.encryption.EncryptionStatus;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.external.conf.model.GoogleAccessToken;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

@Slf4j
public class TestDhisConfigurationProvider implements DhisConfigurationProvider {

  protected Properties properties;

  public TestDhisConfigurationProvider() {}

  public TestDhisConfigurationProvider(Properties properties) {
    this.properties = properties;
  }

  public TestDhisConfigurationProvider(String filename) {
    this.properties = getPropertiesFromFile(filename);
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
    String value = properties.getProperty(key.getKey());

    for (String alias : key.getAliases()) {
      if (properties.contains(alias)) {
        value = alias;
      }
    }

    return StringUtils.isNotEmpty(value);
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
  public String getConnectionUrl() {
    return null;
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
    return this.properties.getProperty(
        ConfigurationKey.SERVER_BASE_URL.getKey(),
        ConfigurationKey.SERVER_BASE_URL.getDefaultValue());
  }

  @Override
  public List<String> getMetaDataSyncRemoteServersAllowed() {
    return Stream.of(
            this.properties
                .getProperty(
                    ConfigurationKey.META_DATA_SYNC_SERVERS_ALLOWED.getKey(),
                    ConfigurationKey.META_DATA_SYNC_SERVERS_ALLOWED.getDefaultValue())
                .split(","))
        .filter(StringUtils::isNotEmpty)
        .toList();
  }

  @Override
  public boolean isMetaDataSyncRemoteServerAllowed(String url) {
    List<String> remoteServersAllowed = getMetaDataSyncRemoteServersAllowed();
    return !getMetaDataSyncRemoteServersAllowed().isEmpty()
        && remoteServersAllowed.stream().anyMatch(url::startsWith);
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
    return Stream.of(ConfigurationKey.values())
        .collect(
            Collectors.toMap(
                ConfigurationKey::getKey,
                v ->
                    v.isConfidential()
                        ? ""
                        : getPropertyOrDefault(
                            v, ObjectUtils.defaultIfNull(v.getDefaultValue(), ""))));
  }

  protected Properties getPropertiesFromFile(String fileName) {
    try {
      return PropertiesLoaderUtils.loadProperties(new ClassPathResource(fileName));
    } catch (IOException ex) {
      log.warn(String.format("Could not load %s from classpath", fileName), ex);
      return new Properties();
    }
  }

  public void addProperties(Properties properties) {
    this.properties.putAll(properties);
  }
}
