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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;
import org.hisp.dhis.http.HttpClientAdapter;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.Test;

class DataSummaryControllerTest extends PostgresControllerIntegrationTestBase {

  @Test
  void canGetPrometheusMetrics() {

    HttpResponse response = GET("/api/dataSummary/metrics", HttpClientAdapter.Accept("text/plain"));
    assertEquals(HttpStatus.OK, response.status());
    String content = response.content("text/plain");
    assertFalse(content.isEmpty(), "Response content should not be empty");
    assertTrue(
        content.contains("# HELP data_summary_active_users"), "Active users help text is missing");
    assertTrue(
        content
            .lines()
            .anyMatch(line -> line.matches("^data_summary_active_users\\{days=\"\\d+\"} \\d+")),
        "Active users metric should have days label and integer value");

    assertTrue(
        content.contains("# HELP data_summary_object_counts"),
        "Object counts help text is missing");
    assertTrue(
        content.lines().anyMatch(line -> line.startsWith("data_summary_object_counts")),
        "Object counts metric is missing");
    assertTrue(
        content.contains("# HELP data_summary_data_value_count"),
        "Data value count help text is missing");
    assertTrue(
        content.lines().anyMatch(line -> line.startsWith("data_summary_data_value_count")),
        "Data value count metric is missing");
    assertTrue(
        content.contains("# HELP data_summary_event_count"), "Event count help text is missing");
    assertTrue(
        content.lines().anyMatch(line -> line.startsWith("data_summary_event_count")),
        "Event count metric is missing");
    // Single event count
    assertTrue(
        content.contains("# HELP data_summary_single_event_count"),
        "Single event count help text is missing");
    assertTrue(
        content.lines().anyMatch(line -> line.startsWith("data_summary_single_event_count")),
        "Single event count metric is missing");
    // Tracker event count
    assertTrue(
        content.contains("# HELP data_summary_tracker_event_count"),
        "Tracker event count help text is missing");
    assertTrue(
        content.lines().anyMatch(line -> line.startsWith("data_summary_tracker_event_count")),
        "Tracker event count metric is missing");
    assertTrue(
        content.lines().anyMatch(line -> line.startsWith("data_summary_enrollment_count")),
        "Enrollment count metric is missing");
    // Logins
    assertTrue(content.contains("# HELP data_summary_logins"), "Logins help text is missing");
    assertTrue(
        content.lines().anyMatch(line -> line.startsWith("data_summary_logins")),
        "Logins metric is missing");
    assertTrue(
        content
            .lines()
            .anyMatch(line -> line.matches("^data_summary_logins\\{days=\"\\d+\"} \\d+")),
        "Logins metric should have days label and integer value");
    assertTrue(
        content.contains("# HELP data_summary_build_info"), "Build info help text is missing");
    // data_summary_build_info should end with an integer representing the build time in seconds
    // since epoch
    assertTrue(
        content.lines().anyMatch(line -> line.matches("data_summary_build_info\\{.*\\} \\d+")),
        "Build info metric should end with an integer");
    assertTrue(content.contains("# HELP data_summary_system_id"), "System ID help text is missing");
    // data_summary_system_id metric should be a static value of 1
    assertTrue(
        content.lines().anyMatch(line -> line.matches("data_summary_system_id\\{.*\\} 1")),
        "System ID metric should be a static value of 1");
  }

  @Test
  void canGetSummaryStatistics() {

    HttpResponse response = GET("/api/dataSummary");
    assertEquals(HttpStatus.OK, response.status());
    JsonMixed content = response.content();
    assertFalse(content.isEmpty(), "Response content should not be empty");
    assertTrue(content.has("system"), "System information is missing");
    assertTrue(content.has("objectCounts"), "Object counts are missing");
    content
        .get("objectCounts")
        .asMap(JsonValue.class)
        .values()
        .forEach(value -> assertTrue(value.isInteger(), "Object count values should be integers"));
    assertTrue(content.has("activeUsers"), "Active users are missing");
    content
        .get("activeUsers")
        .asMap(JsonValue.class)
        .values()
        .forEach(value -> assertTrue(value.isInteger(), "Active user values should be integers"));
    content
        .get("activeUsers")
        .asMap(JsonValue.class)
        .keys()
        .forEach(key -> assertTrue(key.matches("\\d{1,2}"), "Active user keys should be integers"));
    content
        .get("logins")
        .asMap(JsonValue.class)
        .values()
        .forEach(value -> assertTrue(value.isInteger(), "Login values should be integers"));
    content
        .get("logins")
        .asMap(JsonValue.class)
        .keys()
        .forEach(key -> assertTrue(key.matches("\\d{1,2}"), "Login keys should be integers"));
    assertTrue(content.has("userInvitations"), "User invitations are missing");
    content
        .get("activeUsers")
        .asMap(JsonValue.class)
        .values()
        .forEach(
            value -> assertTrue(value.isInteger(), "User invitation values should be integers"));
    assertTrue(content.has("dataValueCount"), "Data value counts are missing");
    content
        .get("dataValueCount")
        .asMap(JsonValue.class)
        .values()
        .forEach(
            value -> assertTrue(value.isInteger(), "Data value count values should be integers"));
    content
        .get("dataValueCount")
        .asMap(JsonValue.class)
        .keys()
        .forEach(
            key -> assertTrue(key.matches("\\d{1,2}"), "Data value count keys should be integers"));
    assertTrue(content.has("eventCount"), "Event counts are missing");
    content
        .get("eventCount")
        .asMap(JsonValue.class)
        .values()
        .forEach(value -> assertTrue(value.isInteger(), "Event count values should be integers"));
    content
        .get("eventCount")
        .asMap(JsonValue.class)
        .keys()
        .forEach(key -> assertTrue(key.matches("\\d{1,2}"), "Event count keys should be integers"));
    content
        .get("trackerEventCount")
        .asMap(JsonValue.class)
        .values()
        .forEach(
            value ->
                assertTrue(value.isInteger(), "Tracker event count values should be integers"));
    content
        .get("trackerEventCount")
        .asMap(JsonValue.class)
        .keys()
        .forEach(
            key ->
                assertTrue(key.matches("\\d{1,2}"), "Tracker event count keys should be integers"));
    content
        .get("singleEventCount")
        .asMap(JsonValue.class)
        .values()
        .forEach(
            value -> assertTrue(value.isInteger(), "Single event count values should be integers"));
    content
        .get("singleEventCount")
        .asMap(JsonValue.class)
        .keys()
        .forEach(
            key ->
                assertTrue(key.matches("\\d{1,2}"), "Single event count keys should be integers"));
  }

  @Test
  void canVerifyDataElementObjectCount() {
    String dataElementId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataElements",
                "{ 'name': 'DataSummaryDE', 'shortName': 'DataSummaryDE', 'valueType' : 'NUMBER',"
                    + "'domainType' : 'AGGREGATE', 'aggregationType' : 'SUM'  }"));
    // Get object counts before deleting the data element
    HttpResponse responseBeforeDelete = GET("/api/dataSummary");
    JsonMixed contentBeforeDelete = responseBeforeDelete.content();
    int dataElementCountBeforeDelete;
    try {
      dataElementCountBeforeDelete =
          Integer.parseInt(
              contentBeforeDelete
                  .get("objectCounts")
                  .asMap(JsonValue.class)
                  .get("dataElement")
                  .toString());
    } catch (NumberFormatException e) {
      fail(
          "Could not parse data element count as integer: "
              + contentBeforeDelete
                  .get("objectCounts")
                  .asMap(JsonValue.class)
                  .get("dataElement")
                  .toString());
      return;
    }
    // Confirm greater than zero
    assertTrue(dataElementCountBeforeDelete > 0, "Data element count should be greater than zero");
    // Delete the data element
    assertStatus(HttpStatus.OK, DELETE("/dataElements/" + dataElementId));
    // Get object counts after deleting the data element
    HttpResponse responseAfterDelete = GET("/api/dataSummary");
    JsonMixed contentAfterDelete = responseAfterDelete.content();
    int dataElementCountAfterDelete;
    try {
      dataElementCountAfterDelete =
          Integer.parseInt(
              contentAfterDelete
                  .get("objectCounts")
                  .asMap(JsonValue.class)
                  .get("dataElement")
                  .toString());
    } catch (NumberFormatException e) {
      fail(
          "Could not parse data element count AFTER as integer: "
              + contentAfterDelete
                  .get("objectCounts")
                  .asMap(JsonValue.class)
                  .get("dataElement")
                  .toString());
      return;
    }
    // Confirm the count has decreased by one
    assertEquals(
        dataElementCountBeforeDelete - 1,
        dataElementCountAfterDelete,
        "Data element count should have decreased by one after deletion");
  }

  @Test
  void canVerifyDashboardObjectCount() {
    String dashboardId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dashboards", "{ 'name': 'DataSummaryDashboard', 'type': 'STANDARD_DASHBOARD' }"));
    // Get object counts before deleting the dashboard
    HttpResponse responseBeforeDelete = GET("/api/dataSummary");
    JsonMixed contentBeforeDelete = responseBeforeDelete.content();
    int dashboardCountBeforeDelete;
    try {
      dashboardCountBeforeDelete =
          Integer.parseInt(
              contentBeforeDelete
                  .get("objectCounts")
                  .asMap(JsonValue.class)
                  .get("dashboard")
                  .toString());
    } catch (NumberFormatException e) {
      fail(
          "Could not parse dashboard count as integer: "
              + contentBeforeDelete
                  .get("objectCounts")
                  .asMap(JsonValue.class)
                  .get("dashboard")
                  .toString());
      return;
    }
    // Confirm greater than zero
    assertTrue(dashboardCountBeforeDelete > 0, "Dashboard count should be greater than zero");
    // Delete the dashboard
    assertStatus(HttpStatus.OK, DELETE("/dashboards/" + dashboardId));
    // Get object counts after deleting the dashboard
    HttpResponse responseAfterDelete = GET("/api/dataSummary");
    JsonMixed contentAfterDelete = responseAfterDelete.content();
    int dashboardCountAfterDelete;
    try {
      dashboardCountAfterDelete =
          Integer.parseInt(
              contentAfterDelete
                  .get("objectCounts")
                  .asMap(JsonValue.class)
                  .get("dashboard")
                  .toString());
    } catch (NumberFormatException e) {
      fail(
          "Could not parse dashboard count AFTER as integer: "
              + contentAfterDelete
                  .get("objectCounts")
                  .asMap(JsonValue.class)
                  .get("dashboard")
                  .toString());
      return;
    }
    // Confirm the count has decreased by one
    assertEquals(
        dashboardCountBeforeDelete - 1,
        dashboardCountAfterDelete,
        "Dashboard count should have decreased by one after deletion");
  }

  void canVerifyDataElementGroupObjectCount() {
    String dataElementGroupId =
        assertStatus(
            HttpStatus.CREATED, POST("/dataElementGroups", "{ 'name': 'DataSummaryDEGroup' }"));
    // Get object counts before deleting the data element group
    HttpResponse responseBeforeDelete = GET("/api/dataSummary");
    JsonMixed contentBeforeDelete = responseBeforeDelete.content();
    int dataElementGroupCountBeforeDelete;
    try {
      dataElementGroupCountBeforeDelete =
          Integer.parseInt(
              contentBeforeDelete
                  .get("objectCounts")
                  .asMap(JsonValue.class)
                  .get("dataElementGroup")
                  .toString());
    } catch (NumberFormatException e) {
      fail(
          "Could not parse data element group count as integer: "
              + contentBeforeDelete
                  .get("objectCounts")
                  .asMap(JsonValue.class)
                  .get("dataElementGroup")
                  .toString());
      return;
    }

    // Confirm greater than zero
    assertTrue(
        dataElementGroupCountBeforeDelete > 0,
        "Data element group count should be greater than zero");
    // Delete the data element group
    assertStatus(HttpStatus.OK, DELETE("/dataElementGroups/" + dataElementGroupId));
    // Get object counts after deleting the data element group
    HttpResponse responseAfterDelete = GET("/api/dataSummary");
    JsonMixed contentAfterDelete = responseAfterDelete.content();
    int dataElementGroupCountAfterDelete;
    try {
      dataElementGroupCountAfterDelete =
          Integer.parseInt(
              contentAfterDelete
                  .get("objectCounts")
                  .asMap(JsonValue.class)
                  .get("dataElementGroup")
                  .toString());
    } catch (NumberFormatException e) {
      fail(
          "Could not parse data element group count AFTER as integer: "
              + contentAfterDelete
                  .get("objectCounts")
                  .asMap(JsonValue.class)
                  .get("dataElementGroup")
                  .toString());
      return;
    }
    // Confirm the count has decreased by one
    assertEquals(
        dataElementGroupCountBeforeDelete - 1,
        dataElementGroupCountAfterDelete,
        "Data element group count should have decreased by one after deletion");
  }

  @Test
  void canVerifyLoginsOneHourAgo() {
    HttpResponse responseBefore = GET("/api/dataSummary");
    JsonMixed contentBefore = responseBefore.content();
    // Users over the last hour
    int loginsOneHourAgoCountBefore;
    try {
      String raw = contentBefore.get("logins").asMap(JsonValue.class).get("0").toString();
      loginsOneHourAgoCountBefore = Integer.parseInt(raw);
    } catch (NumberFormatException e) {
      fail(
          "Could not parse active users one hour ago count as integer: "
              + contentBefore.get("logins").asMap(JsonValue.class).get("0").toString());
      return;
    }
    int loginsOneWeekAgoCountBefore;
    try {
      String raw = contentBefore.get("logins").asMap(JsonValue.class).get("2").toString();
      loginsOneWeekAgoCountBefore = Integer.parseInt(raw);
    } catch (NumberFormatException e) {
      fail(
          "Could not parse active users one week ago count as integer: "
              + contentBefore.get("logins").asMap(JsonValue.class).get("2").toString());
      return;
    }

    // Confirm greater than or equal to zero
    assertTrue(
        loginsOneHourAgoCountBefore >= 0,
        "Active users count one hour ago should be greater than or equal to zero");
    assertTrue(
        loginsOneWeekAgoCountBefore >= 0,
        "Active users count one week ago should be greater than or equal to zero");

    // Create a new user with the service layer and be sure to set the last login to five minutes
    // ago
    User a = makeUser("a");
    // Five minutes ago
    Date fiveMinutesAgo = new Date(System.currentTimeMillis() - 5 * 60 * 1000);
    a.setLastLogin(fiveMinutesAgo);
    userService.addUser(a);
    // Add another user who was active 2 days ago
    User b = makeUser("b");
    Date twoDaysAgo = new Date(System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000);
    b.setLastLogin(twoDaysAgo);
    userService.addUser(b);

    // Get object counts after creating a user
    HttpResponse responseAfter = GET("/api/dataSummary");
    JsonMixed contentAfter = responseAfter.content();
    int loginsOneHourAgoCountAfter;
    try {
      String raw = contentAfter.get("logins").asMap(JsonValue.class).get("0").toString();
      loginsOneHourAgoCountAfter = Integer.parseInt(raw);
    } catch (NumberFormatException e) {
      fail(
          "Could not parse logins one hour ago count AFTER as integer: "
              + contentAfter.get("logins").asMap(JsonValue.class).get("0").toString());
      return;
    }
    // Confirm the count has increased by one
    assertEquals(
        loginsOneHourAgoCountBefore + 1,
        loginsOneHourAgoCountAfter,
        "Logins count one hour ago should have increased by one after user login");

    // Logins over the last week should have increased by two
    int loginsOneWeekAgoCountAfter;
    try {
      String raw = contentAfter.get("logins").asMap(JsonValue.class).get("7").toString();
      loginsOneWeekAgoCountAfter = Integer.parseInt(raw);
    } catch (NumberFormatException e) {
      fail(
          "Could not parse logins one week ago count AFTER as integer: "
              + contentAfter.get("logins").asMap(JsonValue.class).get("7").toString());
      return;
    }

    assertEquals(
        loginsOneWeekAgoCountBefore + 2,
        loginsOneWeekAgoCountAfter,
        "Logins count one week ago should have increased by two after user logins");

    // Clean up - delete the user
    userService.deleteUser(a);
    userService.deleteUser(b);
  }
}
