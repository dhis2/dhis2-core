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
package org.hisp.dhis.monitoring.metrics.jdbc;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import javax.sql.DataSource;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * @author Jon Schneider
 */
public class DataSourcePoolMetrics implements MeterBinder {
  private final DataSource dataSource;

  private final CachingDataSourcePoolMetadataProvider metadataProvider;

  private final Iterable<Tag> tags;

  public DataSourcePoolMetrics(
      DataSource dataSource,
      Collection<DataSourcePoolMetadataProvider> metadataProviders,
      String dataSourceName,
      Iterable<Tag> tags) {
    this(
        dataSource,
        new CompositeDataSourcePoolMetadataProvider(metadataProviders),
        dataSourceName,
        tags);
  }

  public DataSourcePoolMetrics(
      DataSource dataSource,
      DataSourcePoolMetadataProvider metadataProvider,
      String name,
      Iterable<Tag> tags) {
    Assert.notNull(dataSource, "DataSource must not be null");
    Assert.notNull(metadataProvider, "MetadataProvider must not be null");
    this.dataSource = dataSource;
    this.metadataProvider = new CachingDataSourcePoolMetadataProvider(metadataProvider);
    this.tags = Tags.concat(tags, "name", name);
  }

  @Override
  public void bindTo(MeterRegistry registry) {
    if (this.metadataProvider.getDataSourcePoolMetadata(this.dataSource) != null) {
      bindPoolMetadata(registry, "active", DataSourcePoolMetadata::getActive);
      bindPoolMetadata(registry, "idle", DataSourcePoolMetadata::getIdle);
      bindPoolMetadata(registry, "max", DataSourcePoolMetadata::getMax);
      bindPoolMetadata(registry, "min", DataSourcePoolMetadata::getMin);
    }
  }

  private <N extends Number> void bindPoolMetadata(
      MeterRegistry registry, String metricName, Function<DataSourcePoolMetadata, N> function) {
    bindDataSource(registry, metricName, this.metadataProvider.getValueFunction(function));
  }

  private <N extends Number> void bindDataSource(
      MeterRegistry registry, String metricName, Function<DataSource, N> function) {
    if (function.apply(this.dataSource) != null) {
      registry.gauge(
          "jdbc.connections." + metricName,
          this.tags,
          this.dataSource,
          (m) -> function.apply(m).doubleValue());
    }
  }

  private static class CachingDataSourcePoolMetadataProvider
      implements DataSourcePoolMetadataProvider {

    private static final Map<DataSource, DataSourcePoolMetadata> cache =
        new ConcurrentReferenceHashMap<>();

    private final DataSourcePoolMetadataProvider metadataProvider;

    CachingDataSourcePoolMetadataProvider(DataSourcePoolMetadataProvider metadataProvider) {
      this.metadataProvider = metadataProvider;
    }

    <N extends Number> Function<DataSource, N> getValueFunction(
        Function<DataSourcePoolMetadata, N> function) {
      return (dataSource) -> function.apply(getDataSourcePoolMetadata(dataSource));
    }

    @Override
    public DataSourcePoolMetadata getDataSourcePoolMetadata(DataSource dataSource) {
      DataSourcePoolMetadata metadata = cache.get(dataSource);
      if (metadata == null) {
        metadata = this.metadataProvider.getDataSourcePoolMetadata(dataSource);
        cache.put(dataSource, metadata);
      }
      return metadata;
    }
  }
}
