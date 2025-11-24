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
package org.hisp.dhis.external.config;

import static org.hisp.dhis.external.conf.ConfigurationKey.META_DATA_SYNC_RETRY;
import static org.hisp.dhis.external.conf.ConfigurationKey.META_DATA_SYNC_RETRY_TIME_FREQUENCY_MILLISEC;

import java.util.concurrent.Executor;
import org.hisp.dhis.external.conf.ConfigurationPropertyFactoryBean;
import org.hisp.dhis.external.location.DefaultLocationManager;
import org.hisp.dhis.external.location.LocationManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author Luciano Fiandesio
 */
@Configuration("externalServiceConfig")
@EnableAsync
@EnableScheduling
public class ServiceConfig {
  @Bean
  public LocationManager locationManager() {
    return DefaultLocationManager.getDefault();
  }

  @Bean
  public ConfigurationPropertyFactoryBean maxAttempts() {
    return new ConfigurationPropertyFactoryBean(META_DATA_SYNC_RETRY);
  }

  @Bean
  public ConfigurationPropertyFactoryBean initialInterval() {
    return new ConfigurationPropertyFactoryBean(META_DATA_SYNC_RETRY_TIME_FREQUENCY_MILLISEC);
  }

  /**
   * Default async executor for @Async methods. Uses Spring Boot defaults with MDC propagation.
   *
   * <p>DHIS2-20484: Uses MdcTaskDecorator to propagate MDC context from parent thread to async
   * execution threads, enabling SQL query correlation in logs. Without this, @Async methods would
   * execute on SimpleAsyncTaskExecutor threads without request_id context.
   *
   * <p>Configuration matches Spring Boot auto-configuration defaults:
   *
   * <ul>
   *   <li>Core pool size: 8 (Spring Boot default)
   *   <li>Max pool size: Integer.MAX_VALUE (unbounded)
   *   <li>Queue capacity: Integer.MAX_VALUE (unbounded)
   *   <li>Keep alive: 60 seconds (default)
   * </ul>
   */
  @Bean(name = "taskExecutor")
  public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(8); // Spring Boot default
    executor.setMaxPoolSize(Integer.MAX_VALUE); // Spring Boot default (unbounded)
    executor.setQueueCapacity(Integer.MAX_VALUE); // Spring Boot default (unbounded)
    executor.setKeepAliveSeconds(60); // Spring Boot default
    executor.setThreadNamePrefix("Async-");
    executor.setTaskDecorator(new MdcTaskDecorator());
    executor.initialize();
    return executor;
  }
}
