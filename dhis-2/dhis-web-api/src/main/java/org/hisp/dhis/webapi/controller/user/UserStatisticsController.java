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

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.hisp.dhis.security.Authorities.F_PERFORM_MAINTENANCE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.user.DailyActiveUsers;
import org.hisp.dhis.user.DailyLoginStatistics;
import org.hisp.dhis.user.LoginEventService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserActivityService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * User activity statistics backed by the {@code loginevent} and {@code useractivity} tables: logins
 * per day, active users per day, and active-user counts per window split by source (byRequest
 * covers any authenticated request from any client; byLogin covers recorded logins).
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@OpenApi.Document(
    entity = User.class,
    classifiers = {"team:platform", "purpose:support"})
@Controller
@RequiredArgsConstructor
@RequestMapping("/api/userStatistics")
public class UserStatisticsController {

  private final LoginEventService loginEventService;

  private final UserActivityService userActivityService;

  /**
   * Active-user counts for the standard summary windows, split by source.
   *
   * @param byRequest distinct users with any authenticated request in the window (all clients)
   * @param byLogin distinct users with a recorded login in the window
   */
  public record ActiveUsersSummary(
      @JsonProperty Map<Integer, Integer> byRequest, @JsonProperty Map<Integer, Integer> byLogin) {}

  /**
   * Returns login statistics per calendar day for the inclusive date range.
   *
   * <p>Each entry has {@code date}, {@code logins} (total recorded logins that day) and {@code
   * uniqueUsers} (distinct usernames). Days with zero activity are omitted.
   *
   * @param startDate inclusive start day (required)
   * @param endDate inclusive end day (required)
   * @return daily statistics ordered by date ascending
   */
  @GetMapping(value = "/logins", produces = APPLICATION_JSON_VALUE)
  @RequiresAuthority(anyOf = F_PERFORM_MAINTENANCE)
  public @ResponseBody List<DailyLoginStatistics> getLogins(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate)
      throws WebMessageException {
    if (endDate.isBefore(startDate)) {
      throw new WebMessageException(conflict("Start date is after end date"));
    }
    return loginEventService.getDailyStatistics(startDate, endDate);
  }

  /**
   * Returns active-user counts for the standard windows: key 0 = last hour, 1 = since start of
   * today, 2/7/30 = last N days. {@code byRequest} counts users with any authenticated API request
   * (covers web, Android and integrations); {@code byLogin} counts users with a recorded login.
   *
   * @return the active user summary
   */
  @GetMapping(value = "/activeUsers", produces = APPLICATION_JSON_VALUE)
  @RequiresAuthority(anyOf = F_PERFORM_MAINTENANCE)
  public @ResponseBody ActiveUsersSummary getActiveUsers() {
    List<Integer> keys = List.of(0, 1, 2, 7, 30);
    ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
    List<Date> windows =
        List.of(
            Date.from(now.minusHours(1).toInstant()),
            Date.from(now.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant()),
            Date.from(now.minusDays(2).toInstant()),
            Date.from(now.minusDays(7).toInstant()),
            Date.from(now.minusDays(30).toInstant()));
    return new ActiveUsersSummary(
        toWindowMap(keys, userActivityService.getActiveUsersCounts(windows)),
        toWindowMap(keys, loginEventService.getDistinctActiveUsersCounts(windows)));
  }

  /**
   * Returns distinct active users per calendar day (any authenticated request, any client) for the
   * inclusive date range. Days with zero activity are omitted.
   *
   * @param startDate inclusive start day (required)
   * @param endDate inclusive end day (required)
   * @return daily active users ordered by date ascending
   */
  @GetMapping(value = "/activeUsersPerDay", produces = APPLICATION_JSON_VALUE)
  @RequiresAuthority(anyOf = F_PERFORM_MAINTENANCE)
  public @ResponseBody List<DailyActiveUsers> getActiveUsersPerDay(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate)
      throws WebMessageException {
    if (endDate.isBefore(startDate)) {
      throw new WebMessageException(conflict("Start date is after end date"));
    }
    return userActivityService.getDailyStatistics(startDate, endDate);
  }

  private static Map<Integer, Integer> toWindowMap(List<Integer> keys, List<Integer> counts) {
    Map<Integer, Integer> map = new LinkedHashMap<>();
    for (int i = 0; i < keys.size(); i++) {
      map.put(keys.get(i), counts.get(i));
    }
    return map;
  }
}
