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

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Objects;
import javax.annotation.CheckForNull;
import org.hisp.dhis.system.SystemInfo;
import org.hisp.dhis.system.SystemService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Registers a {@code dhis2_version_info} Prometheus metric on the {@code /api/metrics} scrape
 * endpoint, exposing the running instance's build version, revision and build time as labels.
 *
 * <p>Version, revision and build time are fixed for the lifetime of the JVM (baked in at build
 * time, read once from {@code build.properties}), so unlike {@link
 * org.hisp.dhis.webapi.security.session.SessionMetrics} this reads {@link
 * SystemService#getSystemInfo()} once at registration time rather than wiring a live function.
 *
 * <p>Follows the standard Prometheus "info" pattern (e.g. {@code prometheus_build_info}): the gauge
 * value is always {@code 1} and the actual data is carried entirely in labels, since
 * version/revision/build-time are categorical, not numeric.
 *
 * <p>No-ops when no {@link MeterRegistry} is available in the application context, or when {@link
 * SystemService#getSystemInfo()} returns {@code null} (queried before {@code SystemService} has
 * finished its own startup initialization).
 *
 * <p><b>Naming note:</b> the meter is registered with the dotted name {@code dhis2.version.info},
 * not the underscored {@code dhis2_version_info} it ends up exposed as. The Prometheus client
 * library treats {@code _info} (like {@code _total}/{@code _created}) as a reserved metric-name
 * suffix and strips it from a plain gauge, which would silently rename this metric to {@code
 * dhis2_version}. Micrometer's {@code PrometheusMeterRegistry} only takes the dedicated Info code
 * path — which is exempt from that stripping — when the pre-conversion meter name ends in the
 * literal {@code .info}, so the dotted form is required here, not cosmetic.
 *
 * @author Jason P. Pickering <jason@dhis2.org>
 */
@Component
public class VersionMetrics {

  static final String VERSION_INFO = "dhis2.version.info";

  @Autowired
  VersionMetrics(SystemService systemService, ObjectProvider<MeterRegistry> registryProvider) {
    this(systemService, registryProvider.getIfAvailable());
  }

  VersionMetrics(SystemService systemService, @CheckForNull MeterRegistry registry) {
    if (registry == null) {
      return;
    }

    SystemInfo systemInfo = systemService.getSystemInfo();
    if (systemInfo == null) {
      return;
    }

    String buildTime =
        systemInfo.getBuildTime() == null ? "" : systemInfo.getBuildTime().toInstant().toString();

    Gauge.builder(VERSION_INFO, () -> 1)
        .description(
            "DHIS2 build version, revision and build time, exposed as labels; value is always 1")
        .tag("version", Objects.requireNonNullElse(systemInfo.getVersion(), ""))
        .tag("revision", Objects.requireNonNullElse(systemInfo.getRevision(), ""))
        .tag("build_time", buildTime)
        .register(registry);
  }
}
