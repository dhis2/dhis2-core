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
package org.hisp.dhis.test.analytics;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.rampUsers;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ClosedInjectionStep;
import io.gatling.javaapi.core.OpenInjectionStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides the common definitions and settings used across all tests. It's the class where all
 * common settings should be defined.
 */
public class TestDefinitions {
  private static final Logger logger = LoggerFactory.getLogger(TestDefinitions.class);
  public static final String BASE_URL = "http://localhost:8080";

  public static final String USERNAME = "admin";
  public static final String PASSWORD = "district";

  /**
   * Creates a {@link OpenInjectionStep} object with a single constant user during the given period
   * of time.
   *
   * @param during the rump duration.
   * @return the configured {@link OpenInjectionStep}.
   */
  public static ClosedInjectionStep constantSingleUser(int during, int concurrentUsers) {
    return constantConcurrentUsers(concurrentUsers).during(during);
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
   * Basic login process.
   *
   * @return the {@link ChainBuilder} with the required login information.
   */
  public static ChainBuilder loginChain() {
    return exec(
        http("Login")
            .post("/api/auth/login")
            .header("Content-Type", "application/json")
            .body(StringBody("{\"username\":\"admin\",\"password\":\"district\"}"))
            .check(status().is(200)));
  }
}
