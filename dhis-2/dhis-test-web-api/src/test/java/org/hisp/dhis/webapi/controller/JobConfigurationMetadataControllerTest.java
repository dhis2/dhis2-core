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
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** Tests the metadata import, export and schema of job configurations. */
@Transactional
class JobConfigurationMetadataControllerTest extends PostgresControllerIntegrationTestBase {
  private static final String JOB_UID = "JobConfA001";

  /**
   * The schema properties of a job configuration except translations, which the schema names
   * "translation" and the JSON names "translations".
   */
  private static final Set<String> PROPERTIES_WITHOUT_TRANSLATIONS =
      Set.of(
          "access",
          "attributeValues",
          "code",
          "configurable",
          "created",
          "createdBy",
          "cronExpression",
          "delay",
          "displayName",
          "enabled",
          "errorCodes",
          "executedBy",
          "href",
          "id",
          "jobParameters",
          "jobStatus",
          "jobType",
          "lastAlive",
          "lastExecuted",
          "lastExecutedStatus",
          "lastFinished",
          "lastRuntimeExecution",
          "lastUpdated",
          "lastUpdatedBy",
          "leaderOnlyJob",
          "maxDelayedExecutionTime",
          "name",
          "nextExecutionTime",
          "queueName",
          "queuePosition",
          "schedulingType",
          "sharing",
          "user",
          "userUid");

  @Autowired private EntityManagerFactory entityManagerFactory;

  @Test
  void shouldCreateAndUpdateJobViaMetadataImport() {
    importMetadata(job("Job A", "0 0 3 ? * *", 2));

    JsonObject created = getJob();
    assertEquals("JOB_A", created.getString("code").string());
    assertEquals("Job A", created.getString("name").string());
    assertEquals("ANALYTICS_TABLE", created.getString("jobType").string());
    assertEquals("CRON", created.getString("schedulingType").string());
    assertEquals("0 0 3 ? * *", created.getString("cronExpression").string());
    assertEquals("SCHEDULED", created.getString("jobStatus").string());
    assertEquals("NOT_STARTED", created.getString("lastExecutedStatus").string());
    assertTrue(created.getBoolean("enabled").booleanValue());
    assertTrue(created.getBoolean("configurable").booleanValue());
    assertEquals(2, created.getObject("jobParameters").getNumber("lastYears").intValue());
    assertTrue(created.getObject("jobParameters").getBoolean("skipResourceTables").booleanValue());

    importMetadata(job("Job B", "0 0 4 ? * *", 5));

    JsonObject updated = getJob();
    assertEquals("Job B", updated.getString("name").string());
    assertEquals("0 0 4 ? * *", updated.getString("cronExpression").string());
    assertEquals(5, updated.getObject("jobParameters").getNumber("lastYears").intValue());
  }

  @Test
  void shouldUpdateJobWhenExportedJobWithChangesIsPut() {
    importMetadata(job("Job A", "0 0 3 ? * *", 2));
    JsonObject exported = getJob();
    JsonObject changed = withMember(exported, "cronExpression", "\"0 30 5 ? * *\"");
    changed = withMember(changed, "name", "\"Job changed\"");

    assertStatus(HttpStatus.OK, PUT("/jobConfigurations/" + JOB_UID, changed.toJson()));

    JsonObject updated = getJob();
    assertEquals("Job changed", updated.getString("name").string());
    assertEquals("0 30 5 ? * *", updated.getString("cronExpression").string());
    assertEquals("ANALYTICS_TABLE", updated.getString("jobType").string());
    assertEquals("CRON", updated.getString("schedulingType").string());
    assertEquals("JOB_A", updated.getString("code").string());
    assertEquals(2, updated.getObject("jobParameters").getNumber("lastYears").intValue());
  }

  @Test
  void shouldDeleteJob() {
    importMetadata(job("Job A", "0 0 3 ? * *", 2));

    assertStatus(HttpStatus.OK, DELETE("/jobConfigurations/" + JOB_UID));

    assertEquals(HttpStatus.NOT_FOUND, getJobResponse().status());
  }

  @Test
  void shouldRejectTranslationsOfJobOnMetadataImport() {
    HttpResponse response =
        POST(
            "/metadata",
            """
            {'jobConfigurations':[{
              'id':'%s','name':'Job A','jobType':'DATA_INTEGRITY','cronExpression':'0 0 3 ? * *',
              'translations':[{'locale':'fr','property':'NAME','value':'Tache A'}]
            }]}
            """
                .formatted(JOB_UID));

    String report = response.contentUnchecked().toJson();
    assertEquals(HttpStatus.CONFLICT, response.status(), report);
    assertTrue(report.contains("E1107"), report);
    assertEquals(HttpStatus.NOT_FOUND, getJobResponse().status());
  }

  @Test
  void shouldExportJobProperties() {
    importMetadata(job("Job A", "0 0 3 ? * *", 2));

    JsonObject job = getJob();

    Set<String> names = new TreeSet<>(PROPERTIES_WITHOUT_TRANSLATIONS);
    names.removeAll(
        Set.of(
            "createdBy",
            "user",
            "lastAlive",
            "lastExecuted",
            "lastFinished",
            "lastRuntimeExecution",
            "errorCodes",
            "delay",
            "queueName",
            "queuePosition",
            "executedBy",
            "userUid"));
    names.add("translations");
    assertEquals(names, new TreeSet<>(job.names()));
    assertTrue(job.getString("href").string().endsWith("/jobConfigurations/" + JOB_UID));
    assertEquals("Job A", job.getString("displayName").string());
    assertEquals(Set.of("users", "userGroups"), Set.copyOf(job.getObject("sharing").names()));
  }

  @Test
  void shouldDescribeJobSchemaPropertyNames() {
    Set<String> names = new TreeSet<>(PROPERTIES_WITHOUT_TRANSLATIONS);
    names.add("translation");
    assertEquals(names, schemaPropertyNames(property -> true));
  }

  @Test
  void shouldDescribeJobSchemaPersistedProperties() {
    assertEquals(
        Set.of(
            "code",
            "created",
            "cronExpression",
            "delay",
            "enabled",
            "errorCodes",
            "executedBy",
            "id",
            "jobParameters",
            "jobStatus",
            "jobType",
            "lastAlive",
            "lastExecuted",
            "lastExecutedStatus",
            "lastFinished",
            "lastUpdated",
            "lastUpdatedBy",
            "name",
            "queueName",
            "queuePosition",
            "schedulingType"),
        schemaPropertyNames(property -> property.getBoolean("persisted").booleanValue()));
  }

  @Test
  void shouldDescribeJobSchemaTypesAndLengths() {
    Map<String, JsonObject> properties =
        schemaProperties().collect(Collectors.toMap(p -> p.getString("name").string(), p -> p));

    assertEquals(230, properties.get("name").getNumber("length").intValue());
    assertEquals(50, properties.get("queueName").getNumber("length").intValue());
    assertEquals(11, properties.get("executedBy").getNumber("length").intValue());
    assertEquals(120, properties.get("jobType").getNumber("length").intValue());
    assertEquals(12, properties.get("schedulingType").getNumber("length").intValue());
    assertEquals(Integer.MAX_VALUE, properties.get("errorCodes").getNumber("length").intValue());
    assertEquals("CONSTANT", properties.get("jobStatus").getString("propertyType").string());
    assertEquals("COMPLEX", properties.get("jobParameters").getString("propertyType").string());
    assertEquals("URL", properties.get("href").getString("propertyType").string());
  }

  @Test
  void shouldDescribeJobSchemaNameAndFlags() {
    JsonObject schema = GET("/schemas/jobConfiguration").content(HttpStatus.OK);

    assertEquals("identifiableObject", schema.getString("name").string());
    assertFalse(schema.getBoolean("shareable").booleanValue());
    assertFalse(schema.getBoolean("translatable").booleanValue());
    assertTrue(schema.getBoolean("secondaryMetadata").booleanValue());
  }

  private Set<String> schemaPropertyNames(Predicate<JsonObject> filter) {
    return schemaProperties()
        .filter(filter)
        .map(property -> property.getString("name").string())
        .collect(Collectors.toCollection(TreeSet::new));
  }

  private Stream<JsonObject> schemaProperties() {
    return GET("/schemas/jobConfiguration")
        .content(HttpStatus.OK)
        .getArray("properties")
        .asList(JsonObject.class)
        .stream();
  }

  private static String job(String name, String cronExpression, int lastYears) {
    return """
        {'jobConfigurations':[{
          'id':'%s','code':'JOB_A','name':'%s','jobType':'ANALYTICS_TABLE',
          'schedulingType':'CRON','cronExpression':'%s',
          'jobParameters':{'lastYears':%d,'skipResourceTables':true}
        }]}
        """
        .formatted(JOB_UID, name, cronExpression, lastYears);
  }

  /** Returns a copy of the object with the value of the given member replaced. */
  private static JsonObject withMember(JsonObject object, String member, String json) {
    return JsonMixed.of(object.get(member).node().replaceWith(json)).asObject();
  }

  private void importMetadata(String metadata) {
    HttpResponse response = POST("/metadata", metadata);
    assertEquals(HttpStatus.OK, response.status(), () -> response.contentUnchecked().toJson());
    assertEquals("OK", response.content().getString("status").string());
  }

  private JsonObject getJob() {
    return getJobResponse().content(HttpStatus.OK);
  }

  /** Requests the job in a new session with an empty L2 cache. */
  private HttpResponse getJobResponse() {
    startNewRequestSession();
    entityManagerFactory.getCache().evictAll();
    return GET("/jobConfigurations/" + JOB_UID);
  }
}
