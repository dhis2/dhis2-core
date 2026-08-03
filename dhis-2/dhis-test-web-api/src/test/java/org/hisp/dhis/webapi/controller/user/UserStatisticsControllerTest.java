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
package org.hisp.dhis.webapi.controller.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.ZoneId;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.user.DefaultLoginEventService;
import org.hisp.dhis.user.DefaultUserActivityService;
import org.hisp.dhis.user.LoginAuthType;
import org.hisp.dhis.user.LoginEventService;
import org.hisp.dhis.user.UserActivityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Transactional
class UserStatisticsControllerTest extends PostgresControllerIntegrationTestBase {

  @Autowired private LoginEventService loginEventService;

  @Autowired private UserActivityService userActivityService;

  @org.junit.jupiter.api.BeforeEach
  void clearDedup() {
    DefaultLoginEventService.clearDedupCache();
    DefaultUserActivityService.clearDedupCache();
  }

  @Test
  void getLoginsReturnsDailyStatistics() {
    loginEventService.recordLogin("alice", LoginAuthType.FORM);
    loginEventService.recordLogin("bob", LoginAuthType.BASIC);

    LocalDate today = LocalDate.now(ZoneId.systemDefault());
    JsonArray body =
        GET("/api/userStatistics/logins?startDate=" + today + "&endDate=" + today)
            .content(HttpStatus.OK)
            .as(JsonArray.class);
    assertEquals(1, body.size());
    JsonObject day = body.getObject(0);
    assertEquals(today.toString(), day.getString("date").string());
    assertEquals(2, day.getNumber("logins").intValue());
    assertEquals(2, day.getNumber("uniqueUsers").intValue());
  }

  @Test
  void getLoginsRejectsInvertedRange() {
    LocalDate today = LocalDate.now(ZoneId.systemDefault());
    LocalDate yesterday = today.minusDays(1);
    assertEquals(
        HttpStatus.CONFLICT,
        GET("/api/userStatistics/logins?startDate=" + today + "&endDate=" + yesterday).status());
  }

  @Test
  void getLoginsEmptyRangeReturnsEmptyArray() {
    LocalDate farPast = LocalDate.of(2000, 1, 1);
    JsonArray body =
        GET("/api/userStatistics/logins?startDate=" + farPast + "&endDate=" + farPast)
            .content(HttpStatus.OK)
            .as(JsonArray.class);
    assertTrue(body.isEmpty());
  }

  @Test
  void getActiveUsersReturnsByRequestAndByLogin() {
    userActivityService.recordActivity("alice");
    userActivityService.recordActivity("bob");
    loginEventService.recordLogin("alice", LoginAuthType.FORM);

    JsonObject body =
        GET("/api/userStatistics/activeUsers").content(HttpStatus.OK).as(JsonObject.class);

    JsonObject byRequest = body.getObject("byRequest");
    JsonObject byLogin = body.getObject("byLogin");
    // The GET itself is an authenticated request, so the admin user may add one more.
    assertTrue(byRequest.getNumber("0").intValue() >= 2, "byRequest last hour: " + byRequest);
    assertTrue(byRequest.getNumber("30").intValue() >= 2, "byRequest last 30 days: " + byRequest);
    assertEquals(1, byLogin.getNumber("0").intValue(), "byLogin last hour: " + byLogin);
    assertEquals(1, byLogin.getNumber("30").intValue(), "byLogin last 30 days: " + byLogin);
  }

  @Test
  void getActiveUsersPerDayReturnsDailySeries() {
    userActivityService.recordActivity("alice");
    userActivityService.recordActivity("bob");

    LocalDate today = LocalDate.now(ZoneId.systemDefault());
    JsonArray body =
        GET("/api/userStatistics/activeUsersPerDay?startDate=" + today + "&endDate=" + today)
            .content(HttpStatus.OK)
            .as(JsonArray.class);

    assertEquals(1, body.size());
    JsonObject day = body.getObject(0);
    assertEquals(today.toString(), day.getString("date").string());
    assertTrue(day.getNumber("activeUsers").intValue() >= 2, "activeUsers: " + day);
  }
}
