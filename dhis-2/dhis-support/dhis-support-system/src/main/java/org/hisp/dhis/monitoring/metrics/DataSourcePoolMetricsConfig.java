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
package org.hisp.dhis.monitoring.metrics;

import static org.hisp.dhis.external.conf.ConfigurationKey.MONITORING_DBPOOL_ENABLED;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.monitoring.metrics.jdbc.C3p0MetadataProvider;
import org.hisp.dhis.monitoring.metrics.jdbc.HikariMetadataProvider;
import org.hisp.dhis.monitoring.metrics.jdbc.PoolMetadataProvider;
import org.hisp.dhis.monitoring.metrics.jdbc.PoolMetrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * @author Luciano Fiandesio
 */
@Configuration
public class DataSourcePoolMetricsConfig {
  @Configuration
  @Conditional(DataSourcePoolMetricsEnabledCondition.class)
  static class DataSourcePoolMetadataMetricsConfiguration {

    private static final String DATASOURCE_SUFFIX = "dataSource";

    private final MeterRegistry registry;

    private final Collection<PoolMetadataProvider> metadataProviders;

    DataSourcePoolMetadataMetricsConfiguration(
        MeterRegistry registry, Collection<PoolMetadataProvider> metadataProviders) {
      this.registry = registry;
      this.metadataProviders = metadataProviders;
    }

    @Autowired
    public void bindDataSourcesToRegistry(Map<String, DataSource> dataSources) {
      dataSources.forEach(this::bindDataSourceToRegistry);
    }

    private void bindDataSourceToRegistry(String beanName, DataSource dataSource) {
      String dataSourceName = getDataSourceName(beanName);
      new PoolMetrics(dataSource, this.metadataProviders, dataSourceName, Collections.emptyList())
          .bindTo(this.registry);
    }

    /**
     * Get the name of a DataSource based on its {@code beanName}.
     *
     * @param beanName the name of the data source bean
     * @return a name for the given data source
     */
    private String getDataSourceName(String beanName) {
      if (beanName.length() > DATASOURCE_SUFFIX.length()
          && StringUtils.endsWithIgnoreCase(beanName, DATASOURCE_SUFFIX)) {
        return beanName.substring(0, beanName.length() - DATASOURCE_SUFFIX.length());
      }
      return beanName;
    }
  }

  @Bean
  public Collection<PoolMetadataProvider> dataSourceMetadataProvider() {
    return List.of(
        dataSource -> {
          if (dataSource instanceof ComboPooledDataSource comboPooledDataSource) {
            return new C3p0MetadataProvider(comboPooledDataSource);
          } else if (dataSource instanceof HikariDataSource hikariDataSource) {
            return new HikariMetadataProvider(hikariDataSource);
          } else {
            throw new IllegalArgumentException(
                "Unsupported DataSource type: " + dataSource.getClass().getName());
          }
        });
  }

  static class DataSourcePoolMetricsEnabledCondition extends MetricsEnabler {
    @Override
    protected ConfigurationKey getConfigKey() {
      return MONITORING_DBPOOL_ENABLED;
    }
  }
}
