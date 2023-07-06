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
package org.hisp.dhis.monitoring.prometheus.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration Properties for configuring metrics export to Prometheus.
 *
 * @author Jon Schneider
 */
public class PrometheusProperties {

  /**
   * Whether to enable publishing descriptions as part of the scrape payload to Prometheus. Turn
   * this off to minimize the amount of data sent on each scrape.
   */
  private boolean descriptions = true;

  /**
   * Configuration options for using Prometheus Pushgateway, allowing metrics to be pushed when they
   * cannot be scraped.
   */
  private Pushgateway pushgateway = new Pushgateway();

  /** Step size (i.e. reporting frequency) to use. */
  private Duration step = Duration.ofMinutes(1);

  public boolean isDescriptions() {
    return this.descriptions;
  }

  public void setDescriptions(boolean descriptions) {
    this.descriptions = descriptions;
  }

  public Duration getStep() {
    return this.step;
  }

  public void setStep(Duration step) {
    this.step = step;
  }

  public Pushgateway getPushgateway() {
    return this.pushgateway;
  }

  public void setPushgateway(Pushgateway pushgateway) {
    this.pushgateway = pushgateway;
  }

  /** Configuration options for push-based interaction with Prometheus. */
  public static class Pushgateway {

    /** Enable publishing via a Prometheus Pushgateway. */
    private Boolean enabled = false;

    /** Base URL for the Pushgateway. */
    private String baseUrl = "http://localhost:9091";

    /** Frequency with which to push metrics. */
    private Duration pushRate = Duration.ofMinutes(1);

    /** Job identifier for this application instance. */
    private String job;

    /** Grouping key for the pushed metrics. */
    private Map<String, String> groupingKey = new HashMap<>();

    public Boolean getEnabled() {
      return this.enabled;
    }

    public void setEnabled(Boolean enabled) {
      this.enabled = enabled;
    }

    public String getBaseUrl() {
      return this.baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
    }

    public Duration getPushRate() {
      return this.pushRate;
    }

    public void setPushRate(Duration pushRate) {
      this.pushRate = pushRate;
    }

    public String getJob() {
      return this.job;
    }

    public void setJob(String job) {
      this.job = job;
    }

    public Map<String, String> getGroupingKey() {
      return this.groupingKey;
    }

    public void setGroupingKey(Map<String, String> groupingKey) {
      this.groupingKey = groupingKey;
    }
  }
}
