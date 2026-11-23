/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.webapi.monitoring;

import static org.hisp.dhis.external.conf.ConfigurationKey.MONITORING_JMX_ENABLED;

import io.micrometer.core.instrument.MeterRegistry;
import org.hisp.dhis.datastatistics.DataStatisticsService;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.monitoring.metrics.MetricsEnabler;
import org.hisp.dhis.webapi.security.session.SessionStatisticsProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.jmx.export.annotation.AnnotationMBeanExporter;
import org.springframework.jmx.support.RegistrationPolicy;

/**
 * Registers selected statistics as JMX MBeans when {@code monitoring.jmx.enabled} is on in
 * dhis.conf. Intended for monitoring tools that consume JMX rather than Prometheus, such as
 * Glowroot.
 *
 * <p>The same setting also registers the HikariCP connection pool MBeans, see {@code
 * org.hisp.dhis.datasource.DatabasePoolUtils}.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Configuration
@Conditional(JmxMonitoringConfig.JmxMonitoringEnabledCondition.class)
public class JmxMonitoringConfig {

  @Bean
  public UserStatisticsMBean userStatisticsMBean(
      DataStatisticsService dataStatisticsService, MeterRegistry meterRegistry) {
    return new UserStatisticsMBean(dataStatisticsService, meterRegistry);
  }

  @Bean
  public SessionStatisticsMBean sessionStatisticsMBean(
      SessionStatisticsProvider sessionStatisticsProvider) {
    return new SessionStatisticsMBean(sessionStatisticsProvider);
  }

  @Bean
  public AnnotationMBeanExporter mBeanExporter() {
    AnnotationMBeanExporter exporter = new AnnotationMBeanExporter();
    exporter.setRegistrationPolicy(RegistrationPolicy.REPLACE_EXISTING);
    return exporter;
  }

  static class JmxMonitoringEnabledCondition extends MetricsEnabler {
    @Override
    protected ConfigurationKey getConfigKey() {
      return MONITORING_JMX_ENABLED;
    }
  }
}
