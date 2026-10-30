/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.test.raw;

import static io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.rampUsers;
import static io.gatling.javaapi.core.CoreDsl.rampUsersPerSec;
import static io.gatling.javaapi.http.HttpDsl.http;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hisp.dhis.test.raw.ConfigLoader.CONFIG;
import static org.hisp.dhis.test.raw.Helper.asIntVersion;

import io.gatling.javaapi.core.ClosedInjectionStep;
import io.gatling.javaapi.core.OpenInjectionStep;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides the common definitions and settings used across all tests. It's the class where all
 * common settings should be defined.
 */
public class TestDefinitions {
  private static final Logger logger = LoggerFactory.getLogger(TestDefinitions.class);
  public static final String DHIS2_INSTANCE = CONFIG.getString("instance");
  public static final int DHIS2_VERSION = asIntVersion(CONFIG.getString("version", EMPTY));
  public static final int BASELINE = asIntVersion(CONFIG.getString("baseline", EMPTY));
  public static final String USERNAME = CONFIG.getString("username");
  public static final String PASSWORD = CONFIG.getString("password");
  public static final String SCENARIO = CONFIG.getString("scenario");
  public static final String QUERY = stream(CONFIG.getStringArray("query")).collect(joining(","));

  /**
   * Creates a {@link OpenInjectionStep} object with a single constant user during the given period
   * of time.
   *
   * @param during the rump duration.
   * @return the configured {@link OpenInjectionStep}.
   */
  public static ClosedInjectionStep constantSingleUser(int during) {
    return constantConcurrentUsers(Integer.parseInt((String) CONFIG.getProperty("concurrentUsers")))
        .during(during);
  }

  /**
   * Creates a {@link OpenInjectionStep} object based on the given amount of users and rump
   * duration.
   *
   * @param users the number of users to be created during the given period.
   * @param during the rump duration.
   * @return the configured {@link OpenInjectionStep}.
   */
  public static OpenInjectionStep simpleUsersRumpUp(int users, int during) {
    return rampUsers(users).during(during);
  }

  /**
   * Creates a default {@link OpenInjectionStep} that defines default values for the users rump-up
   * simulation.
   *
   * @return the {@link OpenInjectionStep} object.
   */
  public static OpenInjectionStep defaultComplexUsersRumpUp() {
    int totalDesiredUserCount = 15;
    double userRampUpPerInterval = 1;
    double rampUpIntervalSeconds = 3;

    int totalRampUptimeSeconds = 3;
    int steadyStateDurationSeconds = 20;

    return rampUsersPerSec(userRampUpPerInterval / (rampUpIntervalSeconds / 60))
        .to(totalDesiredUserCount)
        .during(Duration.ofSeconds(totalRampUptimeSeconds + steadyStateDurationSeconds));
  }

  /**
   * Configures a default HTTP simulation protocol to be used by the tests.
   *
   * @return the configured {@link HttpProtocolBuilder} object.
   */
  public static HttpProtocolBuilder defaultHttpProtocol() {
    logger.info("# Pointing to instance: " + DHIS2_INSTANCE);

    return http.baseUrl(DHIS2_INSTANCE)
        .acceptHeader("application/json")
        .maxConnectionsPerHost(100)
        .basicAuth(USERNAME, PASSWORD)
        .header("Content-Type", "application/json")
        .userAgentHeader("Gatling/Performance Test: " + DHIS2_INSTANCE);
  }
}
