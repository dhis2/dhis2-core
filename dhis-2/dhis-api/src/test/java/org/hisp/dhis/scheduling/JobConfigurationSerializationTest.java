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
package org.hisp.dhis.scheduling;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.hamcrest.Matchers;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.scheduling.parameters.AnalyticsJobParameters;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {JobConfiguration}.
 *
 * @author Volker Schmidt
 */
class JobConfigurationSerializationTest {

  private static final String UID1 = "ajsdglkjASG";

  private static final String UID2 = "aksjfhakHg2";

  @Test
  void json() throws IOException {
    final ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.configure(DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY, false);
    JobConfiguration jc =
        objectMapper.readValue(
            "{\n"
                + "      \"lastUpdated\": \"2018-05-18T09:40:19.561\",\n"
                + "      \"id\": \"sHMedQF7VYa\",\n"
                + "      \"created\": \"2017-12-08T12:45:18.351\",\n"
                + "      \"name\": \"Test Analytic\",\n"
                + "      \"jobStatus\": \"SCHEDULED\",\n"
                + "      \"displayName\": \"Test Analytic\",\n"
                + "      \"enabled\": true,\n"
                + "      \"leaderOnlyJob\": true,\n"
                + "      \"externalAccess\": false,\n"
                + "      \"jobType\": \"ANALYTICS_TABLE\",\n"
                + "      \"cronExpression\": \"0 0 12 ? * MON-FRI\",\n"
                + "      \"lastRuntimeExecution\": \"00:00:00.060\",\n"
                + "      \"lastExecutedStatus\": \"COMPLETED\",\n"
                + "      \"lastExecuted\": \"2018-02-22T09:31:21.906\",\n"
                + "      \"favorite\": false,\n"
                + "      \"configurable\": false,\n"
                + "      \"access\": {\n"
                + "        \"read\": true,\n"
                + "        \"update\": true,\n"
                + "        \"externalize\": false,\n"
                + "        \"delete\": true,\n"
                + "        \"write\": true,\n"
                + "        \"manage\": true\n"
                + "      },\n"
                + "      \"jobParameters\":{\"lastYears\":2,\"skipResourceTables\":true,"
                + "      \"skipTableTypes\":[\"ENROLLMENT\",\"ORG_UNIT_TARGET\",\"VALIDATION_RESULT\"],"
                + "      \"skipPrograms\":[\""
                + UID1
                + "\""
                + ",\""
                + UID2
                + "\"]"
                + "      },"
                + "      \"favorites\": [],\n"
                + "      \"translations\": [],\n"
                + "      \"userGroupAccesses\": [],\n"
                + "      \"attributeValues\": [],\n"
                + "      \"userAccesses\": []\n"
                + "    },",
            JobConfiguration.class);
    assertEquals(JobStatus.SCHEDULED, jc.getJobStatus());
    assertEquals("Test Analytic", jc.getName());
    assertEquals("Test Analytic", jc.getDisplayName());
    assertTrue(jc.isEnabled());
    assertTrue(jc.isLeaderOnlyJob());
    assertEquals(JobType.ANALYTICS_TABLE, jc.getJobType());
    assertEquals("0 0 12 ? * MON-FRI", jc.getCronExpression());
    assertNotNull(jc.getJobParameters());
    assertEquals((Integer) 2, ((AnalyticsJobParameters) jc.getJobParameters()).getLastYears());
    assertTrue(((AnalyticsJobParameters) jc.getJobParameters()).isSkipResourceTables());
    assertNotNull(((AnalyticsJobParameters) jc.getJobParameters()).getSkipTableTypes());
    assertEquals(3, ((AnalyticsJobParameters) jc.getJobParameters()).getSkipTableTypes().size());
    assertThat(
        ((AnalyticsJobParameters) jc.getJobParameters()).getSkipTableTypes(),
        Matchers.hasItems(
            AnalyticsTableType.ENROLLMENT,
            AnalyticsTableType.ORG_UNIT_TARGET,
            AnalyticsTableType.VALIDATION_RESULT));
    assertThat(
        ((AnalyticsJobParameters) jc.getJobParameters()).getSkipPrograms(),
        Matchers.hasItems(UID1, UID2));
  }

  @Test
  void disabled() throws IOException {
    final ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.configure(DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY, false);
    JobConfiguration jc =
        objectMapper.readValue(
            "{\n"
                + "      \"lastUpdated\": \"2018-05-18T09:40:19.561\",\n"
                + "      \"id\": \"sHMedQF7VYa\",\n"
                + "      \"created\": \"2017-12-08T12:45:18.351\",\n"
                + "      \"name\": \"Test Analytic\",\n"
                + "      \"jobStatus\": \"SCHEDULED\",\n"
                + "      \"displayName\": \"Test Analytic\",\n"
                + "      \"enabled\": false,\n"
                + "      \"leaderOnlyJob\": true,\n"
                + "      \"externalAccess\": false,\n"
                + "      \"jobType\": \"ANALYTICS_TABLE\",\n"
                + "      \"cronExpression\": \"0 0 12 ? * MON-FRI\",\n"
                + "      \"lastRuntimeExecution\": \"00:00:00.060\",\n"
                + "      \"lastExecutedStatus\": \"COMPLETED\",\n"
                + "      \"lastExecuted\": \"2018-02-22T09:31:21.906\",\n"
                + "      \"favorite\": false,\n"
                + "      \"configurable\": false,\n"
                + "      \"access\": {\n"
                + "        \"read\": true,\n"
                + "        \"update\": true,\n"
                + "        \"externalize\": false,\n"
                + "        \"delete\": true,\n"
                + "        \"write\": true,\n"
                + "        \"manage\": true\n"
                + "      },\n"
                + "      \"jobParameters\":{\"lastYears\":2,\"skipResourceTables\":true,"
                + "      \"skipTableTypes\":[\"ENROLLMENT\",\"ORG_UNIT_TARGET\",\"VALIDATION_RESULT\"],"
                + "      \"skipPrograms\":[\""
                + UID1
                + "\""
                + ",\""
                + UID2
                + "\"]"
                + "      },"
                + "      \"favorites\": [],\n"
                + "      \"translations\": [],\n"
                + "      \"userGroupAccesses\": [],\n"
                + "      \"attributeValues\": [],\n"
                + "      \"userAccesses\": []\n"
                + "    },",
            JobConfiguration.class);
    assertEquals(JobStatus.DISABLED, jc.getJobStatus());
    assertEquals("Test Analytic", jc.getName());
    assertEquals("Test Analytic", jc.getDisplayName());
    assertFalse(jc.isEnabled());
    assertTrue(jc.isLeaderOnlyJob());
    assertEquals(JobType.ANALYTICS_TABLE, jc.getJobType());
    assertEquals("0 0 12 ? * MON-FRI", jc.getCronExpression());
    assertNotNull(jc.getJobParameters());
    assertEquals((Integer) 2, ((AnalyticsJobParameters) jc.getJobParameters()).getLastYears());
    assertTrue(((AnalyticsJobParameters) jc.getJobParameters()).isSkipResourceTables());
    assertNotNull(((AnalyticsJobParameters) jc.getJobParameters()).getSkipTableTypes());
    assertEquals(3, ((AnalyticsJobParameters) jc.getJobParameters()).getSkipTableTypes().size());
    assertThat(
        ((AnalyticsJobParameters) jc.getJobParameters()).getSkipTableTypes(),
        Matchers.hasItems(
            AnalyticsTableType.ENROLLMENT,
            AnalyticsTableType.ORG_UNIT_TARGET,
            AnalyticsTableType.VALIDATION_RESULT));
    assertThat(
        ((AnalyticsJobParameters) jc.getJobParameters()).getSkipPrograms(),
        Matchers.hasItems(UID1, UID2));
  }
}
