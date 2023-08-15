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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.hisp.dhis.jsontree.JsonBuilder;
import org.hisp.dhis.jsontree.JsonBuilder.JsonObjectBuilder;
import org.hisp.dhis.jsontree.JsonNode;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.scheduling.JobStatus;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.SchedulingType;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonJobConfiguration;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link org.hisp.dhis.webapi.controller.scheduling.JobConfigurationController}. Since
 * test setup does not start the {@link org.hisp.dhis.scheduling.JobScheduler} the actual scheduling
 * cannot be tested. This tests focuses on creation including the serialization of job parameters.
 *
 * @author Jan Bernitt
 */
class JobConfigurationControllerTest extends DhisControllerConvenienceTest {

  private static final String UID1 = "asdflksadfjlkj";

  private static final String UID2 = "kajshdfkjahsdkfhj";

  @Test
  void testCONTINUOUS_ANALYTICS_TABLE() {
    String jobId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/jobConfigurations",
                "{'name':'test','jobType':'CONTINUOUS_ANALYTICS_TABLE','delay':'1',"
                    + "'jobParameters':{'fullUpdateHourOfDay':'1','lastYears':'2',"
                    + "'skipTableTypes':['DATA_VALUE','COMPLETENESS','COMPLETENESS_TARGET','ORG_UNIT_TARGET','EVENT','ENROLLMENT','VALIDATION_RESULT']}}"));
    JsonObject parameters = assertJobConfigurationExists(jobId, "CONTINUOUS_ANALYTICS_TABLE");
    assertEquals(1, parameters.getNumber("fullUpdateHourOfDay").intValue());
    assertEquals(2, parameters.getNumber("lastYears").intValue());
    assertContainsOnly(
        List.of(
            "ENROLLMENT",
            "VALIDATION_RESULT",
            "DATA_VALUE",
            "COMPLETENESS",
            "EVENT",
            "ORG_UNIT_TARGET",
            "COMPLETENESS_TARGET"),
        parameters.getArray("skipTableTypes").stringValues());
  }

  @Test
  void testDISABLE_INACTIVE_USERS() {
    String jobId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/jobConfigurations",
                "{'name':'test','jobType':'DISABLE_INACTIVE_USERS','cronExpression':'0 0 1 ? * *','jobParameters':{'inactiveMonths':'3'}}"));
    JsonObject parameters = assertJobConfigurationExists(jobId, "DISABLE_INACTIVE_USERS");
    assertEquals(3, parameters.getNumber("inactiveMonths").intValue());
  }

  @Test
  void testDATA_INTEGRITY() {
    String jobId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/jobConfigurations",
                "{'name':'test','jobType':'DATA_INTEGRITY','cronExpression':'0 0 12 ? * MON-FRI'}"));
    JsonObject parameters = assertJobConfigurationExists(jobId, "DATA_INTEGRITY");
    assertTrue(parameters.exists());
  }

  @Test
  void testRESOURCE_TABLE() {
    String jobId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/jobConfigurations",
                "{'name':'test','jobType':'RESOURCE_TABLE','cronExpression':'0 0 3 ? * MON'}"));
    JsonObject parameters = assertJobConfigurationExists(jobId, "RESOURCE_TABLE");
    assertFalse(parameters.exists());
  }

  @Test
  void testANALYTICS_TABLE() {
    String jobId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/jobConfigurations",
                "{'name':'test','jobType':'ANALYTICS_TABLE','cronExpression':'0 0 3 ? * *',"
                    + "'jobParameters':{'lastYears':'1',"
                    + "'skipTableTypes':['DATA_VALUE','COMPLETENESS','ENROLLMENT'],"
                    + "'skipPrograms':['"
                    + UID1
                    + "','"
                    + UID2
                    + "'],"
                    + "'skipResourceTables':true}}"));
    JsonObject parameters = assertJobConfigurationExists(jobId, "ANALYTICS_TABLE");
    assertEquals(1, parameters.getNumber("lastYears").intValue());
    assertTrue(parameters.getBoolean("skipResourceTables").booleanValue());
    assertContainsOnly(
        List.of("DATA_VALUE", "COMPLETENESS", "ENROLLMENT"),
        parameters.getArray("skipTableTypes").stringValues());
    assertContainsOnly(List.of(UID1, UID2), parameters.getArray("skipPrograms").stringValues());
  }

  @Test
  void testPROGRAM_NOTIFICATIONS() {
    String jobId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/jobConfigurations",
                "{'name':'test','jobType':'PROGRAM_NOTIFICATIONS','cronExpression':'0 0 1 ? * *'}"));
    JsonObject parameters = assertJobConfigurationExists(jobId, "PROGRAM_NOTIFICATIONS");
    assertFalse(parameters.exists());
  }

  @Test
  void testMONITORING() {
    String jobId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/jobConfigurations",
                "{'name':'test','jobType':'MONITORING','cronExpression':'0 0 12 ? * MON-FRI',"
                    + "'jobParameters':{'relativeStart':'1','relativeEnd':'2','validationRuleGroups':[],'sendNotifications':true,'persistResults':true}}"));
    JsonObject parameters = assertJobConfigurationExists(jobId, "MONITORING");
    assertEquals(1, parameters.getNumber("relativeStart").intValue());
    assertEquals(2, parameters.getNumber("relativeEnd").intValue());
    assertTrue(parameters.getBoolean("sendNotifications").booleanValue());
    assertTrue(parameters.getBoolean("persistResults").booleanValue());
    assertTrue(parameters.getArray("validationRuleGroups").isEmpty());
  }

  @Test
  void testLOCK_EXCEPTION_CLEANUP() {
    String jobId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/jobConfigurations",
                "{'name':'test','jobType':'LOCK_EXCEPTION_CLEANUP','cronExpression':'0 0 12 ? * MON-FRI',"
                    + "'jobParameters':{'expiresAfterMonths':'3'}}"));
    JsonObject parameters = assertJobConfigurationExists(jobId, "LOCK_EXCEPTION_CLEANUP");
    assertEquals(3, parameters.getNumber("expiresAfterMonths").intValue());
  }

  @Test
  void testGetJobTypeInfo() {
    for (JsonObject e :
        GET("/jobConfigurations/jobTypes").content().getList("jobTypes", JsonObject.class)) {
      if (e.getString("jobType").string().equals("ANALYTICS_TABLE")) {
        for (JsonObject param : e.getList("jobParameters", JsonObject.class)) {
          if (param.getString("name").string().equals("skipTableTypes")) {
            assertEquals(
                List.of(
                    "DATA_VALUE",
                    "COMPLETENESS",
                    "COMPLETENESS_TARGET",
                    "ORG_UNIT_TARGET",
                    "EVENT",
                    "ENROLLMENT",
                    "OWNERSHIP",
                    "VALIDATION_RESULT",
                    "TRACKED_ENTITY_INSTANCE_EVENTS",
                    "TRACKED_ENTITY_INSTANCE_ENROLLMENTS",
                    "TRACKED_ENTITY_INSTANCE"),
                param.getArray("constants").stringValues());
          }
        }
      }
    }
  }

  @Test
  void testGetJobTypesExtended() {
    JsonObject types = GET("/jobConfigurations/jobTypesExtended").content();
    JsonObject param = types.getObject("ANALYTICS_TABLE").getObject("skipTableTypes");
    assertEquals(
        List.of(
            "DATA_VALUE",
            "COMPLETENESS",
            "COMPLETENESS_TARGET",
            "ORG_UNIT_TARGET",
            "EVENT",
            "ENROLLMENT",
            "OWNERSHIP",
            "VALIDATION_RESULT",
            "TRACKED_ENTITY_INSTANCE_EVENTS",
            "TRACKED_ENTITY_INSTANCE_ENROLLMENTS",
            "TRACKED_ENTITY_INSTANCE"),
        param.getArray("constants").stringValues());
  }

  private static final Map<String, Object> MINIMAL_CRON_CONFIG =
      Map.of(
          "name",
          "test",
          "jobType",
          JobType.DATA_INTEGRITY,
          "cronExpression",
          "0 0 12 ? * MON-FRI");

  @Test
  void testLastExecutedIsIgnored_Create() {
    assertNull(createExpectSuccess(Map.of("lastExecuted", new Date())).getLastExecuted());
  }

  @Test
  void testLastExecutedIsIgnored_Update() {
    assertNull(
        updateExpectSuccess(obj -> obj.addMember("lastExecuted", toJson(new Date())))
            .getLastExecuted());
  }

  @Test
  void testLastAliveIsIgnored_Create() {
    assertNull(createExpectSuccess(Map.of("lastAlive", new Date())).getLastAlive());
  }

  @Test
  void testLastAliveIsIgnored_Update() {
    assertNull(
        updateExpectSuccess(obj -> obj.addMember("lastAlive", toJson(new Date()))).getLastAlive());
  }

  @Test
  void testLastFinishedIsIgnored_Create() {
    assertNull(createExpectSuccess(Map.of("lastFinished", new Date())).getLastFinished());
  }

  @Test
  void testLastFinishedIsIgnored_Update() {
    assertNull(
        updateExpectSuccess(obj -> obj.addMember("lastFinished", toJson(new Date())))
            .getLastFinished());
  }

  @Test
  void testLastExecutedStatusIsIgnored_Create() {
    assertEquals(
        JobStatus.NOT_STARTED,
        createExpectSuccess(Map.of("lastExecutedStatus", JobStatus.DISABLED))
            .getLastExecutedStatus());
  }

  @Test
  void testLastExecutedStatusIsReadOnly_Update() {
    assertEquals(
        JobStatus.NOT_STARTED,
        updateExpectSuccess(obj -> obj.addMember("lastExecutedStatus", toJson(JobStatus.DISABLED)))
            .getLastExecutedStatus());
  }

  @Test
  void testJobStatusIsIgnored_Create() {
    assertEquals(
        JobStatus.SCHEDULED,
        createExpectSuccess(Map.of("jobStatus", JobStatus.DISABLED)).getJobStatus());
  }

  @Test
  void testJobStatusIsIgnored_Update() {
    assertEquals(
        JobStatus.SCHEDULED,
        updateExpectSuccess(obj -> obj.addMember("jobStatus", toJson(JobStatus.DISABLED)))
            .getJobStatus());
  }

  @Test
  void testSchedulingTypeCron_Create() {
    JsonJobConfiguration config = createExpectSuccess(Map.of());
    assertEquals(SchedulingType.CRON, config.getSchedulingType());
    assertNotNull(config.getCronExpression());
    assertNull(config.getDelay());
  }

  @Test
  void testSchedulingTypeCron_CreateWithDelay() {
    JsonJobConfiguration config = createExpectSuccess(Map.of("delay", 100));
    assertEquals(SchedulingType.CRON, config.getSchedulingType());
    assertNotNull(config.getCronExpression());
    assertEquals(100, config.getDelay());
  }

  @Test
  void testSchedulingTypeFixedDelay_Create() {
    JsonJobConfiguration config =
        createExpectSuccess(
            Map.of("name", "d-test", "jobType", "CONTINUOUS_ANALYTICS_TABLE", "delay", 100),
            Map.of());
    assertEquals(SchedulingType.FIXED_DELAY, config.getSchedulingType());
    assertNull(config.getCronExpression());
    assertEquals(100, config.getDelay());
  }

  @Test
  void testSchedulingTypeFixedDelay_CreateWithCronExpression() {
    JsonJobConfiguration config =
        createExpectSuccess(
            Map.of(
                "name",
                "d-test",
                "jobType",
                "CONTINUOUS_ANALYTICS_TABLE",
                "delay",
                100,
                "schedulingType",
                "FIXED_DELAY"),
            Map.of("cronExpression", "0 0 12 ? * MON-FRI"));
    assertEquals(SchedulingType.FIXED_DELAY, config.getSchedulingType());
    assertNotNull(config.getCronExpression());
    assertEquals(100, config.getDelay());
  }

  @Test
  void testSchedulingTypeIsReadWrite_Update() {
    assertEquals(
        SchedulingType.FIXED_DELAY, // created like that by default in this test
        updateExpectSuccess(
                obj ->
                    obj.addMember("schedulingType", toJson(SchedulingType.FIXED_DELAY))
                        .addMember("delay", toJson(420)))
            .getSchedulingType());
  }

  @Test
  void testJobTypeIsReadOnly_Update() {
    assertEquals(
        JobType.DATA_INTEGRITY, // created like that by default in this test
        updateExpectSuccess(obj -> obj.addMember("jobType", toJson(JobType.ANALYTICS_TABLE)))
            .getJobType());
  }

  @Test
  void testQueueNameIsReadOnly_Create() {
    assertNull(createExpectSuccess(Map.of("queueName", "test-q")).getQueueName());
  }

  @Test
  void testQueueNameIsReadOnly_Update() {
    assertNull(
        updateExpectSuccess(obj -> obj.addMember("queueName", toJson("test-q"))).getQueueName());
  }

  @Test
  void testQueuePositionIsReadOnly_Create() {
    assertNull(createExpectSuccess(Map.of("queuePosition", 42)).getQueuePosition());
  }

  @Test
  void testQueuePositionIsReadOnly_Update() {
    assertNull(
        updateExpectSuccess(obj -> obj.addMember("queuePosition", toJson(42))).getQueuePosition());
  }

  private JsonJobConfiguration createExpectSuccess(Map<String, Object> extra) {
    return createExpectSuccess(MINIMAL_CRON_CONFIG, extra);
  }

  private JsonJobConfiguration createExpectSuccess(
      Map<String, Object> minimal, Map<String, Object> extra) {
    JsonNode json =
        JsonBuilder.createObject(
            obj -> {
              minimal.forEach((name, value) -> obj.addMember(name, toJson(value)));
              extra.forEach((name, value) -> obj.addMember(name, toJson(value)));
            });

    String jobId =
        assertStatus(HttpStatus.CREATED, POST("/jobConfigurations", json.getDeclaration()));
    return getJsonJobConfiguration(jobId);
  }

  private JsonJobConfiguration updateExpectSuccess(Consumer<JsonObjectBuilder> addMembers) {
    JsonJobConfiguration config = createExpectSuccess(Map.of());
    JsonNode withUpdate = config.node().addMembers(addMembers);
    String jobId = config.getId();
    assertStatus(HttpStatus.OK, PUT("/jobConfigurations/" + jobId, withUpdate.getDeclaration()));

    return getJsonJobConfiguration(jobId);
  }

  private JsonJobConfiguration getJsonJobConfiguration(String jobId) {
    return GET("/jobConfigurations/{id}", jobId).content().as(JsonJobConfiguration.class);
  }

  private JsonObject assertJobConfigurationExists(String jobId, String expectedJobType) {
    JsonObject jobConfiguration = GET("/jobConfigurations/{id}", jobId).content();
    assertEquals(jobId, jobConfiguration.getString("id").string());
    assertEquals("test", jobConfiguration.getString("name").string());
    assertEquals("SCHEDULED", jobConfiguration.getString("jobStatus").string());
    assertTrue(jobConfiguration.getBoolean("enabled").booleanValue());
    assertEquals(expectedJobType, jobConfiguration.getString("jobType").string());
    return jobConfiguration.getObject("jobParameters");
  }

  private static JsonNode toJson(Object value) {
    try {
      return JsonNode.of(new ObjectMapper().writeValueAsString(value));
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
