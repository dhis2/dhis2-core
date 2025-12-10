/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.datasource;

import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory;

/**
 * Provider for HikariCP metrics tracker factories. This interface enables dependency inversion
 * between datasource configuration (in dhis-support-hibernate) and metrics configuration (in
 * dhis-support-system).
 *
 * <p>Implementations should validate datasource name uniqueness and return null if HikariCP is not
 * the configured pool type.
 */
@FunctionalInterface
public interface HikariMetricsTrackerProvider {

  /**
   * Creates a metrics tracker factory for a HikariCP datasource.
   *
   * @param dataSourceName unique name identifying this datasource in metrics
   * @return a metrics tracker factory, or null if metrics are not applicable (e.g., not using
   *     HikariCP)
   * @throws IllegalStateException if a datasource with this name was already registered
   * @throws NullPointerException if dataSourceName is null
   */
  MicrometerMetricsTrackerFactory createMetricsTracker(String dataSourceName);
}
