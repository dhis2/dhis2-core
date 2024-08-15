/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.config;

import static org.hisp.dhis.external.conf.ConfigurationKey.USE_QUERY_CACHE;
import static org.hisp.dhis.external.conf.ConfigurationKey.USE_SECOND_LEVEL_CACHE;

import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.PersistenceUnitTransactionType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.hibernate.cache.jcache.ConfigSettings;
import org.hibernate.cache.jcache.MissingCacheStrategy;
import org.hibernate.cache.jcache.internal.JCacheRegionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitManager;

@Slf4j
public class DhisPersistenceUnitManager implements PersistenceUnitManager {
  private final DhisConfigurationProvider dhisConfig;
  private final DataSource dataSource;

  public DhisPersistenceUnitManager(DhisConfigurationProvider dhisConfig, DataSource dataSource) {
    this.dhisConfig = dhisConfig;
    this.dataSource = dataSource;
  }

  @Override
  public PersistenceUnitInfo obtainDefaultPersistenceUnitInfo() throws IllegalStateException {
    return DhisPersistenceUnitInfo.builder()
        .nonJtaDataSource(dataSource)
        .jtaDataSource(dataSource)
        .persistenceProviderClassName(HibernatePersistenceProvider.class.getName())
        .persistenceUnitName("dhis")
        .properties(getAdditionalProperties(dhisConfig))
        .validationMode(ValidationMode.AUTO)
        .transactionType(PersistenceUnitTransactionType.RESOURCE_LOCAL)
        .sharedCacheMode(SharedCacheMode.ENABLE_SELECTIVE)
        .mappingFileNames(Arrays.stream(loadResources()).toList())
        .build();
  }

  @Override
  public PersistenceUnitInfo obtainPersistenceUnitInfo(String persistenceUnitName)
      throws IllegalArgumentException, IllegalStateException {
    return obtainDefaultPersistenceUnitInfo();
  }

  /**
   * Loads all the hibernate mapping files from the classpath
   *
   * @return Array of Strings representing the mapping files
   */
  private String[] loadResources() {
    try {
      PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
      Resource[] resources = resolver.getResources("classpath*:org/hisp/dhis/**/*.hbm.xml");

      List<String> list = new ArrayList<>();
      for (Resource resource : resources) {
        String url = resource.getURL().toString();
        list.add(url);
      }
      return list.toArray(new String[0]);
    } catch (IOException e) {
      log.error(e.getMessage(), e);
    }
    return ArrayUtils.EMPTY_STRING_ARRAY;
  }

  /**
   * Returns additional properties to be used by the {@link LocalContainerEntityManagerFactoryBean}
   */
  private Properties getAdditionalProperties(DhisConfigurationProvider dhisConfig) {
    Properties properties = new Properties();
    properties.put(
        "hibernate.current_session_context_class",
        "org.springframework.orm.hibernate5.SpringSessionContext");

    if (dhisConfig.getProperty(USE_SECOND_LEVEL_CACHE).equals("true")) {
      properties.put(AvailableSettings.USE_SECOND_LEVEL_CACHE, "true");
      properties.put(AvailableSettings.CACHE_REGION_FACTORY, JCacheRegionFactory.class.getName());
      properties.put(AvailableSettings.USE_QUERY_CACHE, dhisConfig.getProperty(USE_QUERY_CACHE));
      properties.put(
          ConfigSettings.MISSING_CACHE_STRATEGY,
          MissingCacheStrategy.CREATE.getExternalRepresentation());
    }

    properties.setProperty(
        AvailableSettings.DIALECT, dhisConfig.getProperty(ConfigurationKey.CONNECTION_DIALECT));
    properties.setProperty(
        AvailableSettings.HBM2DDL_AUTO, dhisConfig.getProperty(ConfigurationKey.CONNECTION_SCHEMA));
    properties.setProperty(
        AvailableSettings.URL, dhisConfig.getProperty(ConfigurationKey.CONNECTION_URL));

    // TODO: this is anti-pattern and should be turn off
    properties.put("hibernate.allow_update_outside_transaction", "true");

    return properties;
  }
}
