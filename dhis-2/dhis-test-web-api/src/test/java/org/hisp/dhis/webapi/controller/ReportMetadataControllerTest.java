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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** Metadata import, export and schema of reports. */
@Transactional
class ReportMetadataControllerTest extends PostgresControllerIntegrationTestBase {
  private static final String REPORT_UID = "RepUid00001";

  private static final String VISUALIZATION_UID = "RepVis00001";

  /** The identifiable object property set, which the dashboard search serializes reports with. */
  private static final Set<String> IDENTIFIABLE_KEYS =
      Set.of(
          "access",
          "attributeValues",
          "code",
          "created",
          "createdBy",
          "displayName",
          "href",
          "id",
          "lastUpdated",
          "lastUpdatedBy",
          "name",
          "sharing",
          "translations",
          "user");

  @Autowired private EntityManagerFactory entityManagerFactory;

  /** The report is imported in the same payload as the visualization it references. */
  @Test
  void shouldCreateAndUpdateReportViaMetadataImport() {
    importMetadata(report("REP_A", "Imported report", true, "CACHE_6AM_TOMORROW"));

    JsonObject created = getReport(REPORT_UID);
    assertEquals("REP_A", created.getString("code").string());
    assertEquals("Imported report", created.getString("name").string());
    assertEquals("HTML", created.getString("type").string());
    assertEquals("<html>A</html>", created.getString("designContent").string());
    assertEquals(VISUALIZATION_UID, created.getString("visualization.id").string());
    assertTrue(created.getBoolean("reportParams.reportingPeriod").booleanValue());
    assertFalse(created.getBoolean("reportParams.organisationUnit").booleanValue());
    assertTrue(created.getBoolean("relativePeriods.thisMonth").booleanValue());
    assertTrue(created.getBoolean("relativePeriods.last12Months").booleanValue());
    assertEquals("CACHE_6AM_TOMORROW", created.getString("cacheStrategy").string());
    assertEquals("rw------", created.getString("sharing.public").string());
    assertEquals(
        "Rapport importé",
        created.getArray("translations").getObject(0).getString("value").string());

    importMetadata(report("REP_B", "Re-imported report", false, "NO_CACHE"));

    JsonObject updated = getReport(REPORT_UID);
    assertEquals("REP_B", updated.getString("code").string());
    assertEquals("Re-imported report", updated.getString("name").string());
    assertFalse(updated.getBoolean("reportParams.reportingPeriod").booleanValue());
    assertTrue(updated.getBoolean("reportParams.organisationUnit").booleanValue());
    assertEquals("NO_CACHE", updated.getString("cacheStrategy").string());
  }

  @Test
  void shouldUpdateReportWhenExportedReportWithChangesIsPut() {
    importMetadata(report("REP_A", "Imported report", true, "CACHE_6AM_TOMORROW"));
    String exported = getReport(REPORT_UID).toJson();
    String changed =
        replace(exported, "\"name\":\"Imported report\"", "\"name\":\"Changed report\"");
    changed =
        replace(
            changed,
            "\"designContent\":\"<html>A</html>\"",
            "\"designContent\":\"<html>B</html>\"");
    changed = replace(changed, "\"organisationUnit\":false", "\"organisationUnit\":true");
    changed = replace(changed, "\"thisMonth\":true", "\"thisMonth\":false");
    changed =
        replace(
            changed, "\"cacheStrategy\":\"CACHE_6AM_TOMORROW\"", "\"cacheStrategy\":\"NO_CACHE\"");
    changed = replace(changed, "\"value\":\"Rapport importé\"", "\"value\":\"Rapport changé\"");

    assertStatus(HttpStatus.OK, PUT("/reports/" + REPORT_UID, changed));

    JsonObject updated = getReport(REPORT_UID);
    assertEquals("Changed report", updated.getString("name").string());
    assertEquals("<html>B</html>", updated.getString("designContent").string());
    assertTrue(updated.getBoolean("reportParams.reportingPeriod").booleanValue());
    assertTrue(updated.getBoolean("reportParams.organisationUnit").booleanValue());
    assertFalse(updated.getBoolean("relativePeriods.thisMonth").booleanValue());
    assertTrue(updated.getBoolean("relativePeriods.last12Months").booleanValue());
    assertEquals("NO_CACHE", updated.getString("cacheStrategy").string());
    assertEquals(VISUALIZATION_UID, updated.getString("visualization.id").string());
    assertEquals(
        "Rapport changé",
        updated.getArray("translations").getObject(0).getString("value").string());
  }

  @Test
  void shouldExportReportProperties() {
    importMetadata(report("REP_A", "Imported report", true, "CACHE_6AM_TOMORROW"));

    JsonObject report = getReport(REPORT_UID);

    Set<String> expected = new TreeSet<>(IDENTIFIABLE_KEYS);
    expected.addAll(
        Set.of(
            "cacheStrategy",
            "designContent",
            "relativePeriods",
            "reportParams",
            "type",
            "visualization"));
    assertEquals(expected, new TreeSet<>(report.names()));
    assertEquals(
        Set.of(
            "reportingPeriod",
            "grandParentOrganisationUnit",
            "parentOrganisationUnit",
            "organisationUnit"),
        Set.copyOf(report.getObject("reportParams").names()));
    assertTrue(report.getString("href").string().endsWith("/reports/" + REPORT_UID));
    assertEquals("Imported report", report.getString("displayName").string());
  }

  /**
   * The dashboard search serializes reports with the identifiable object properties only. Access
   * and href are not set on a report read in a new request, so they are left out.
   */
  @Test
  void shouldServeReportsInDashboardSearchWithIdentifiableProperties() {
    importMetadata(report("REP_A", "Imported report", true, "CACHE_6AM_TOMORROW"));
    startNewRequestSession();

    JsonArray reports =
        GET("/dashboards/q/Imported?max=REPORTS").content(HttpStatus.OK).getArray("reports");

    assertEquals(1, reports.size());
    JsonObject report = reports.getObject(0);
    Set<String> expected = new TreeSet<>(IDENTIFIABLE_KEYS);
    expected.removeAll(Set.of("access", "href"));
    assertEquals(expected, new TreeSet<>(report.names()));
    assertEquals(REPORT_UID, report.getString("id").string());
    assertEquals("Imported report", report.getString("displayName").string());
  }

  /** Field filtering serves every report property of a dashboard item's reports. */
  @Test
  void shouldServeReportPropertiesOfDashboardItemReports() {
    importMetadata(report("REP_A", "Imported report", true, "CACHE_6AM_TOMORROW"));
    importMetadata(
        """
        {'dashboards':[{'id':'DashUid0001','name':'Reports',
          'dashboardItems':[{'id':'DashItem001','type':'REPORTS',
            'reports':[{'id':'%s'}]}]}]}
        """
            .formatted(REPORT_UID));
    startNewRequestSession();

    JsonObject report =
        GET("/dashboards/DashUid0001?fields=dashboardItems[reports[id,displayName,type,cacheStrategy]]")
            .content(HttpStatus.OK)
            .getArray("dashboardItems")
            .getObject(0)
            .getArray("reports")
            .getObject(0);

    assertEquals(Set.of("id", "displayName", "type", "cacheStrategy"), Set.copyOf(report.names()));
    assertEquals("Imported report", report.getString("displayName").string());
    assertEquals("HTML", report.getString("type").string());
    assertEquals("CACHE_6AM_TOMORROW", report.getString("cacheStrategy").string());
  }

  /** The OpenAPI document describes dashboard reports as identifiable objects. */
  @Test
  void shouldDescribeDashboardReportsAsIdentifiableObjectsInOpenApi() {
    JsonObject schemas =
        GET("/openapi/openapi.json").content(HttpStatus.OK).getObject("components.schemas");

    for (String owner : List.of("DashboardItem", "DashboardSearchResult")) {
      assertEquals(
          "#/components/schemas/BaseIdentifiableObject",
          schemas.getObject(owner).getObject("properties.reports.items").getString("$ref").string(),
          owner);
    }
  }

  /** Reports are imported after the visualizations they reference. */
  @Test
  void shouldImportReportsAfterVisualizations() {
    int report = GET("/schemas/report").content(HttpStatus.OK).getNumber("order").intValue();
    int visualization =
        GET("/schemas/visualization").content(HttpStatus.OK).getNumber("order").intValue();

    assertTrue(report > visualization, () -> report + " <= " + visualization);
  }

  /** The schema properties of a report with their persisted and required flags and lengths. */
  @Test
  void shouldDescribeReportSchemaProperties() {
    JsonObject schema = GET("/schemas/report").content(HttpStatus.OK);
    JsonArray properties = schema.getArray("properties");
    Set<String> names = new TreeSet<>();
    Set<String> persisted = new TreeSet<>();
    Set<String> required = new TreeSet<>();
    Map<String, JsonObject> byName = new HashMap<>();
    for (int i = 0; i < properties.size(); i++) {
      JsonObject property = properties.getObject(i);
      String name = property.getString("name").string();
      names.add(name);
      byName.put(name, property);
      if (property.getBoolean("persisted").booleanValue()) {
        persisted.add(name);
      }
      if (property.getBoolean("required").booleanValue()) {
        required.add(name);
      }
    }

    Set<String> expectedNames = new TreeSet<>(IDENTIFIABLE_KEYS);
    expectedNames.remove("translations");
    expectedNames.addAll(
        Set.of(
            "cacheStrategy",
            "designContent",
            "relativePeriods",
            "reportParams",
            "translation",
            "type",
            "visualization"));
    assertEquals(expectedNames, names);
    assertEquals(
        Set.of(
            "cacheStrategy",
            "code",
            "created",
            "createdBy",
            "designContent",
            "id",
            "lastUpdated",
            "lastUpdatedBy",
            "name",
            "reportParams",
            "sharing",
            "translation",
            "type",
            "visualization"),
        persisted);
    assertEquals(Set.of("name"), required);
    assertEquals(230, byName.get("name").getNumber("length").intValue());
    assertEquals(Integer.MAX_VALUE, byName.get("designContent").getNumber("length").intValue());
    assertEquals(50, byName.get("type").getNumber("length").intValue());
    assertEquals(40, byName.get("cacheStrategy").getNumber("length").intValue());
    assertEquals(50, byName.get("code").getNumber("length").intValue());
    assertEquals("URL", byName.get("href").getString("propertyType").string());
    assertEquals("report", schema.getString("name").string());
    assertTrue(schema.getBoolean("shareable").booleanValue());
    assertTrue(schema.getBoolean("translatable").booleanValue());
    assertTrue(schema.getBoolean("metadata").booleanValue());
  }

  private static String report(
      String code, String name, boolean reportingPeriod, String cacheStrategy) {
    return """
        {'visualizations':[{'id':'%s','name':'Report visualization','type':'PIVOT_TABLE'}],
         'reports':[{'id':'%s','code':'%s','name':'%s','type':'HTML',
          'designContent':'<html>A</html>',
          'visualization':{'id':'%s'},
          'reportParams':{'reportingPeriod':%s,'organisationUnit':%s},
          'relativePeriods':{'thisMonth':true,'last12Months':true},
          'cacheStrategy':'%s',
          'translations':[{'locale':'fr','property':'NAME','value':'Rapport importé'}],
          'sharing':{'public':'rw------','external':false}}]}
        """
        .formatted(
            VISUALIZATION_UID,
            REPORT_UID,
            code,
            name,
            VISUALIZATION_UID,
            reportingPeriod,
            !reportingPeriod,
            cacheStrategy);
  }

  /** Replaces the single occurrence of {@code from}, failing if it does not occur exactly once. */
  private static String replace(String json, String from, String to) {
    assertEquals(1, json.split(Pattern.quote(from), -1).length - 1, () -> from + " in " + json);
    return json.replace(from, to);
  }

  private void importMetadata(String metadata) {
    HttpResponse response = POST("/metadata", metadata);
    assertEquals(HttpStatus.OK, response.status(), () -> response.contentUnchecked().toJson());
  }

  /** Reads the report from the database, bypassing the session and the L2 cache. */
  private JsonObject getReport(String uid) {
    startNewRequestSession();
    entityManagerFactory.getCache().evictAll();
    return GET("/reports/" + uid).content(HttpStatus.OK);
  }
}
