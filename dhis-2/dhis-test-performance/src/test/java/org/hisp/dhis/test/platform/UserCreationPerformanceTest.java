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
package org.hisp.dhis.test.platform;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Focused performance test for user creation via POST /api/users.
 *
 * <p>Run with {@code -DuserGroupUid=<uid>} pointing at a group with large membership to expose the
 * N+1 problem. Omit to run without group assignment as a baseline.
 *
 * <p>Required system properties:
 *
 * <ul>
 *   <li>{@code -DuserRoleUid} — UID of an existing user role
 *   <li>{@code -DorgUnitUid} — UID of an existing organisation unit
 * </ul>
 *
 * <p>Optional system properties:
 *
 * <ul>
 *   <li>{@code -DbaseUrl} (default: http://localhost:8080)
 *   <li>{@code -Dusername} (default: admin)
 *   <li>{@code -Dpassword} (default: district)
 *   <li>{@code -DuserGroupUid} (default: empty — no group assignment)
 *   <li>{@code -Diterations} (default: 3)
 * </ul>
 */
public class UserCreationPerformanceTest extends Simulation {

  private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");
  private static final String USERNAME = System.getProperty("username", "admin");
  private static final String PASSWORD = System.getProperty("password", "district");
  private static final String USER_ROLE_UID = System.getProperty("userRoleUid", "");
  private static final String ORG_UNIT_UID = System.getProperty("orgUnitUid", "");
  private static final String USER_GROUP_UID = System.getProperty("userGroupUid", "");
  private static final int ITERATIONS = Integer.parseInt(System.getProperty("iterations", "3"));

  private static final AtomicInteger COUNTER =
      new AtomicInteger((int) (System.currentTimeMillis() % 10_000_000));

  public UserCreationPerformanceTest() {
    HttpProtocolBuilder httpProtocol =
        http.baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .disableCaching()
            .basicAuth(USERNAME, PASSWORD);

    ScenarioBuilder scenario =
        scenario("POST User - create")
            .exec(flushCookieJar())
            .repeat(ITERATIONS)
            .on(
                exec(
                    session -> {
                      int num = COUNTER.getAndIncrement();
                      String groups =
                          USER_GROUP_UID.isBlank()
                              ? ""
                              : ",\"userGroups\":[{\"id\":\"" + USER_GROUP_UID + "\"}]";
                      String body =
                          "{"
                              + "\"username\":\"perftest_"
                              + String.format("%07d", num)
                              + "\","
                              + "\"firstName\":\"Perf\","
                              + "\"surname\":\"Test\","
                              + "\"password\":\"Test123!\","
                              + "\"userRoles\":[{\"id\":\""
                              + USER_ROLE_UID
                              + "\"}],"
                              + "\"organisationUnits\":[{\"id\":\""
                              + ORG_UNIT_UID
                              + "\"}],"
                              + "\"dataViewOrganisationUnits\":[{\"id\":\""
                              + ORG_UNIT_UID
                              + "\"}]"
                              + groups
                              + "}";
                      return session.set("body", body);
                    })
                    .exec(
                        http("POST User")
                            .post("/api/users")
                            .header("Content-Type", "application/json")
                            .body(StringBody("#{body}"))
                            .check(status().in(200, 201))));

    setUp(scenario.injectOpen(atOnceUsers(1))).protocols(httpProtocol);
  }
}
