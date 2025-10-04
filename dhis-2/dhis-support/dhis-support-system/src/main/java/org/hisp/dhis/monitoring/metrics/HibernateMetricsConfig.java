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

import static org.hisp.dhis.external.conf.ConfigurationKey.MONITORING_HIBERNATE_ENABLED;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.TimeGauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Strings;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * @author Luciano Fiandesio
 */
@Slf4j
@Configuration
@Conditional(HibernateMetricsConfig.HibernateMetricsEnabledCondition.class)
public class HibernateMetricsConfig {
  private static final String ENTITY_MANAGER_FACTORY_SUFFIX = "entityManagerFactory";

  @Autowired
  public void bindEntityManagerFactoriesToRegistry(
      Map<String, EntityManagerFactory> entityManagerFactories, MeterRegistry registry) {
    entityManagerFactories.forEach(
        (name, factory) -> bindEntityManagerFactoryToRegistry(name, factory, registry));
  }

  private void bindEntityManagerFactoryToRegistry(
      String beanName, EntityManagerFactory entityManagerFactory, MeterRegistry registry) {

    String entityManagerFactoryName = getEntityManagerFactoryName(beanName);
    try {
      SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
      sessionFactory.getStatistics().setStatisticsEnabled(true);

      // Create a custom meter binder for Hibernate statistics
      new HibernateMeterBinderCustom(sessionFactory, entityManagerFactoryName).bindTo(registry);
    } catch (PersistenceException ex) {
      log.debug(
          "Failed to bind Hibernate metrics for EntityManagerFactory '{}': {}",
          entityManagerFactoryName,
          ex.getMessage());
    }
  }

  /**
   * Get the name of an {@link EntityManagerFactory} based on its {@code beanName}.
   *
   * @param beanName the name of the {@link EntityManagerFactory} bean
   * @return a name for the given entity manager factory
   */
  private String getEntityManagerFactoryName(String beanName) {
    if (beanName.length() > ENTITY_MANAGER_FACTORY_SUFFIX.length()
        && Strings.CI.endsWith(beanName, ENTITY_MANAGER_FACTORY_SUFFIX)) {
      return beanName.substring(0, beanName.length() - ENTITY_MANAGER_FACTORY_SUFFIX.length());
    }
    return beanName;
  }

  /** Custom implementation of a meter binder for Hibernate statistics */
  private static class HibernateMeterBinderCustom implements MeterBinder {
    private final SessionFactory sessionFactory;
    private final Iterable<Tag> tags;

    public HibernateMeterBinderCustom(SessionFactory sessionFactory, String name) {
      this.sessionFactory = sessionFactory;
      this.tags = Tags.of("entityManagerFactory", name);
    }

    @Override
    public void bindTo(MeterRegistry registry) {
      Statistics statistics = this.sessionFactory.getStatistics();

      // Query execution metrics
      registry.gauge("hibernate.queries", tags, statistics, Statistics::getQueryExecutionCount);
      registry.gauge(
          "hibernate.queries.slow", tags, statistics, Statistics::getQueryExecutionMaxTime);

      // Session metrics
      registry.gauge("hibernate.sessions.open", tags, statistics, Statistics::getSessionOpenCount);
      registry.gauge(
          "hibernate.sessions.closed", tags, statistics, Statistics::getSessionCloseCount);

      // Transaction metrics
      registry.gauge("hibernate.transactions", tags, statistics, Statistics::getTransactionCount);
      registry.gauge(
          "hibernate.transactions.successful",
          tags,
          statistics,
          Statistics::getSuccessfulTransactionCount);

      registry.gauge(
          "hibernate.transactions.failed",
          tags,
          statistics,
          s -> s.getTransactionCount() - s.getSuccessfulTransactionCount());

      registry.gauge(
          "hibernate.optimistic.failures", tags, statistics, Statistics::getOptimisticFailureCount);

      registry.gauge("hibernate.flushes", tags, statistics, Statistics::getFlushCount);
      registry.gauge(
          "hibernate.connections.obtained", tags, statistics, Statistics::getConnectCount);

      // Entity metrics
      registry.gauge(
          "hibernate.entities.deletes", tags, statistics, Statistics::getEntityDeleteCount);
      registry.gauge(
          "hibernate.entities.fetches", tags, statistics, Statistics::getEntityFetchCount);
      registry.gauge(
          "hibernate.entities.inserts", tags, statistics, Statistics::getEntityInsertCount);
      registry.gauge("hibernate.entities.loads", tags, statistics, Statistics::getEntityLoadCount);
      registry.gauge(
          "hibernate.entities.updates", tags, statistics, Statistics::getEntityUpdateCount);

      // Collection metrics
      registry.gauge(
          "hibernate.collections.deletes", tags, statistics, Statistics::getCollectionRemoveCount);
      registry.gauge(
          "hibernate.collections.fetches", tags, statistics, Statistics::getCollectionFetchCount);
      registry.gauge(
          "hibernate.collections.loads", tags, statistics, Statistics::getCollectionLoadCount);
      registry.gauge(
          "hibernate.collections.recreates",
          tags,
          statistics,
          Statistics::getCollectionRecreateCount);
      registry.gauge(
          "hibernate.collections.updates", tags, statistics, Statistics::getCollectionUpdateCount);

      // Prepared statements
      registry.gauge(
          "hibernate.statements.prepared", tags, statistics, Statistics::getPrepareStatementCount);
      registry.gauge(
          "hibernate.statements.closed", tags, statistics, Statistics::getCloseStatementCount);

      // Natural Id cache
      registry.gauge(
          "hibernate.cache.natural.id.requests.hit",
          tags,
          statistics,
          Statistics::getNaturalIdCacheHitCount);
      registry.gauge(
          "hibernate.cache.natural.id.requests.miss",
          tags,
          statistics,
          Statistics::getNaturalIdCacheMissCount);
      registry.gauge(
          "hibernate.cache.natural.id.puts",
          tags,
          statistics,
          Statistics::getNaturalIdCachePutCount);

      registry.gauge(
          "hibernate.query.natural.id.executions",
          tags,
          statistics,
          Statistics::getNaturalIdQueryExecutionCount);

      TimeGauge.builder(
              "hibernate.query.natural.id.executions.max",
              statistics,
              TimeUnit.MILLISECONDS,
              Statistics::getNaturalIdQueryExecutionMaxTime)
          .description("The maximum query time for naturalId queries executed against the database")
          .tags(tags)
          .register(registry);

      // Query statistics
      registry.gauge(
          "hibernate.query.executions", tags, statistics, Statistics::getQueryExecutionCount);

      TimeGauge.builder(
              "hibernate.query.executions.max",
              statistics,
              TimeUnit.MILLISECONDS,
              Statistics::getQueryExecutionMaxTime)
          .description("The time of the slowest query")
          .tags(tags)
          .register(registry);

      // Cache update timestamp
      registry.gauge(
          "hibernate.cache.update.timestamps.requests.hit",
          tags,
          statistics,
          Statistics::getUpdateTimestampsCacheHitCount);
      registry.gauge(
          "hibernate.cache.update.timestamps.requests.miss",
          tags,
          statistics,
          Statistics::getUpdateTimestampsCacheMissCount);
      registry.gauge(
          "hibernate.cache.update.timestamps.puts",
          tags,
          statistics,
          Statistics::getUpdateTimestampsCachePutCount);

      // Second level cache_ metrics
      registry.gauge(
          "hibernate.cache.l2.puts", tags, statistics, Statistics::getSecondLevelCachePutCount);
      registry.gauge(
          "hibernate.cache.l2.hits", tags, statistics, Statistics::getSecondLevelCacheHitCount);
      registry.gauge(
          "hibernate.cache.l2.misses", tags, statistics, Statistics::getSecondLevelCacheMissCount);

      // Query Caching
      registry.gauge(
          "hibernate.cache.query.requests.hit",
          tags,
          statistics,
          Statistics::getQueryCacheHitCount);
      registry.gauge(
          "hibernate.cache.query.requests.miss",
          tags,
          statistics,
          Statistics::getQueryCacheMissCount);
      registry.gauge(
          "hibernate.cache.query.puts", tags, statistics, Statistics::getQueryCachePutCount);
      registry.gauge(
          "hibernate.cache.query.plan.hit",
          tags,
          statistics,
          Statistics::getQueryPlanCacheHitCount);
      registry.gauge(
          "hibernate.cache.query.plan.miss",
          tags,
          statistics,
          Statistics::getQueryPlanCacheMissCount);
    }
  }

  static class HibernateMetricsEnabledCondition extends MetricsEnabler {
    @Override
    protected ConfigurationKey getConfigKey() {
      return MONITORING_HIBERNATE_ENABLED;
    }
  }
}
