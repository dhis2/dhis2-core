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
package org.hisp.dhis.monitoring.metrics;

import static org.hisp.dhis.external.conf.ConfigurationKey.MONITORING_HIBERNATE_ENABLED;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jpa.HibernateMetrics;
import java.util.Collections;
import java.util.Map;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * @author Luciano Fiandesio
 */
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
      new HibernateMetrics(
              entityManagerFactory.unwrap(SessionFactory.class),
              entityManagerFactoryName,
              Collections.emptyList())
          .bindTo(registry);
    } catch (PersistenceException ex) {
      // Continue
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
        && StringUtils.endsWithIgnoreCase(beanName, ENTITY_MANAGER_FACTORY_SUFFIX)) {
      return beanName.substring(0, beanName.length() - ENTITY_MANAGER_FACTORY_SUFFIX.length());
    }
    return beanName;
  }

  static class HibernateMetricsEnabledCondition extends MetricsEnabler {
    @Override
    protected ConfigurationKey getConfigKey() {
      return MONITORING_HIBERNATE_ENABLED;
    }
  }
}
