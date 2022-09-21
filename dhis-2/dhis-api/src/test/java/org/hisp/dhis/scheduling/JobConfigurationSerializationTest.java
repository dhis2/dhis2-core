/*
<<<<<<< HEAD
 * Copyright (c) 2004-2020, University of Oslo
=======
 * Copyright (c) 2004-2021, University of Oslo
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
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

import java.io.IOException;

import org.hamcrest.Matchers;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.scheduling.parameters.AnalyticsJobParameters;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

/**
 * Unit tests for {JobConfiguration}.
 *
 * @author Volker Schmidt
 */
public class JobConfigurationSerializationTest
{
    @Test
    public void xmlWithArray()
        throws IOException
    {
        final XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false );
        xmlMapper.configure( DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY, false );

<<<<<<< HEAD
        JobConfiguration jc = xmlMapper.readValue( "<jobConfiguration lastUpdated=\"2019-03-26T22:57:44.649\" id=\"uB9oC4R2nTn\" created=\"2019-03-26T22:57:44.649\" name=\"Test Analytic\">\n" +
            "      <jobStatus>NONE</jobStatus>\n" +
            "      <displayName>Test Analytic</displayName>\n" +
            "      <enabled>true</enabled>\n" +
            "      <leaderOnlyJob>true</leaderOnlyJob>\n" +
            "      <externalAccess>false</externalAccess>\n" +
            "      <jobType>ANALYTICS_TABLE</jobType>\n" +
            "      <nextExecutionTime>2019-03-27T12:00:00.000</nextExecutionTime>\n" +
            "      <favorite>false</favorite>\n" +
            "      <configurable>true</configurable>\n" +
            "      <access>\n" +
            "        <read>true</read>\n" +
            "        <update>true</update>\n" +
            "        <externalize>false</externalize>\n" +
            "        <delete>true</delete>\n" +
            "        <write>true</write>\n" +
            "        <manage>true</manage>\n" +
            "      </access>\n" +
            "      <lastUpdatedBy id=\"xE7jOejl9FI\"/>\n" +
            "      <jobParameters>\n" +
            "        <lastYears>2</lastYears>\n" +
            "        <skipResourceTables>true</skipResourceTables>\n" +
            "        <skipTableTypes>\n" +
            "          <skipTableType>ENROLLMENT</skipTableType>\n" +
            "          <skipTableType>ORG_UNIT_TARGET</skipTableType>\n" +
            "          <skipTableType>VALIDATION_RESULT</skipTableType>\n" +
            "        </skipTableTypes>" +
            "      </jobParameters>\n" +
            "      <cronExpression>0 0 12 ? * MON-FRI</cronExpression>\n" +
            "    </jobConfiguration>", JobConfiguration.class );
=======
        JobConfiguration jc = xmlMapper.readValue(
            "<jobConfiguration lastUpdated=\"2019-03-26T22:57:44.649\" id=\"uB9oC4R2nTn\" created=\"2019-03-26T22:57:44.649\" name=\"Test Analytic\">\n"
                +
                "      <jobStatus>NONE</jobStatus>\n" +
                "      <displayName>Test Analytic</displayName>\n" +
                "      <enabled>true</enabled>\n" +
                "      <leaderOnlyJob>true</leaderOnlyJob>\n" +
                "      <externalAccess>false</externalAccess>\n" +
                "      <jobType>ANALYTICS_TABLE</jobType>\n" +
                "      <nextExecutionTime>2019-03-27T12:00:00.000</nextExecutionTime>\n" +
                "      <favorite>false</favorite>\n" +
                "      <configurable>true</configurable>\n" +
                "      <access>\n" +
                "        <read>true</read>\n" +
                "        <update>true</update>\n" +
                "        <externalize>false</externalize>\n" +
                "        <delete>true</delete>\n" +
                "        <write>true</write>\n" +
                "        <manage>true</manage>\n" +
                "      </access>\n" +
                "      <lastUpdatedBy id=\"xE7jOejl9FI\"/>\n" +
                "      <jobParameters>\n" +
                "        <lastYears>2</lastYears>\n" +
                "        <skipResourceTables>true</skipResourceTables>\n" +
                "        <skipTableTypes>\n" +
                "          <skipTableType>ENROLLMENT</skipTableType>\n" +
                "          <skipTableType>ORG_UNIT_TARGET</skipTableType>\n" +
                "          <skipTableType>VALIDATION_RESULT</skipTableType>\n" +
                "        </skipTableTypes>" +
                "      </jobParameters>\n" +
                "      <cronExpression>0 0 12 ? * MON-FRI</cronExpression>\n" +
                "    </jobConfiguration>",
            JobConfiguration.class );
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za

        Assert.assertEquals( JobStatus.SCHEDULED, jc.getJobStatus() );
        Assert.assertEquals( "Test Analytic", jc.getDisplayName() );
        Assert.assertTrue( jc.isEnabled() );
        Assert.assertTrue( jc.isLeaderOnlyJob() );
        Assert.assertEquals( JobType.ANALYTICS_TABLE, jc.getJobType() );
        Assert.assertNull( jc.getNextExecutionTime() );
        Assert.assertEquals( "0 0 12 ? * MON-FRI", jc.getCronExpression() );

        Assert.assertNotNull( jc.getJobParameters() );
        Assert.assertEquals( (Integer) 2, ((AnalyticsJobParameters) jc.getJobParameters()).getLastYears() );
        Assert.assertTrue( ((AnalyticsJobParameters) jc.getJobParameters()).isSkipResourceTables() );
        Assert.assertNotNull( ((AnalyticsJobParameters) jc.getJobParameters()).getSkipTableTypes() );
        Assert.assertEquals( 3, ((AnalyticsJobParameters) jc.getJobParameters()).getSkipTableTypes().size() );
        Assert.assertThat( ((AnalyticsJobParameters) jc.getJobParameters()).getSkipTableTypes(), Matchers.hasItems(
            AnalyticsTableType.ENROLLMENT, AnalyticsTableType.ORG_UNIT_TARGET, AnalyticsTableType.VALIDATION_RESULT ) );
    }

    @Test
    public void xmlWithEmptyArray()
        throws IOException
    {
        final XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false );
        xmlMapper.configure( DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY, false );

<<<<<<< HEAD
        JobConfiguration jc = xmlMapper.readValue( "<jobConfiguration lastUpdated=\"2019-03-26T22:57:44.649\" id=\"uB9oC4R2nTn\" created=\"2019-03-26T22:57:44.649\" name=\"Test Analytic\">\n" +
            "      <jobStatus>SCHEDULED</jobStatus>\n" +
            "      <displayName>Test Analytic</displayName>\n" +
            "      <enabled>true</enabled>\n" +
            "      <leaderOnlyJob>true</leaderOnlyJob>\n" +
            "      <externalAccess>false</externalAccess>\n" +
            "      <jobType>ANALYTICS_TABLE</jobType>\n" +
            "      <nextExecutionTime>2019-03-27T12:00:00.000</nextExecutionTime>\n" +
            "      <favorite>false</favorite>\n" +
            "      <configurable>true</configurable>\n" +
            "      <access>\n" +
            "        <read>true</read>\n" +
            "        <update>true</update>\n" +
            "        <externalize>false</externalize>\n" +
            "        <delete>true</delete>\n" +
            "        <write>true</write>\n" +
            "        <manage>true</manage>\n" +
            "      </access>\n" +
            "      <lastUpdatedBy id=\"xE7jOejl9FI\"/>\n" +
            "      <jobParameters>\n" +
            "        <lastYears>2</lastYears>\n" +
            "        <skipResourceTables>true</skipResourceTables>\n" +
            "        <skipTableTypes>\n" +
            "        </skipTableTypes>" +
            "      </jobParameters>\n" +
            "      <cronExpression>0 0 12 ? * MON-FRI</cronExpression>\n" +
            "    </jobConfiguration>", JobConfiguration.class );
=======
        JobConfiguration jc = xmlMapper.readValue(
            "<jobConfiguration lastUpdated=\"2019-03-26T22:57:44.649\" id=\"uB9oC4R2nTn\" created=\"2019-03-26T22:57:44.649\" name=\"Test Analytic\">\n"
                +
                "      <jobStatus>SCHEDULED</jobStatus>\n" +
                "      <displayName>Test Analytic</displayName>\n" +
                "      <enabled>true</enabled>\n" +
                "      <leaderOnlyJob>true</leaderOnlyJob>\n" +
                "      <externalAccess>false</externalAccess>\n" +
                "      <jobType>ANALYTICS_TABLE</jobType>\n" +
                "      <nextExecutionTime>2019-03-27T12:00:00.000</nextExecutionTime>\n" +
                "      <favorite>false</favorite>\n" +
                "      <configurable>true</configurable>\n" +
                "      <access>\n" +
                "        <read>true</read>\n" +
                "        <update>true</update>\n" +
                "        <externalize>false</externalize>\n" +
                "        <delete>true</delete>\n" +
                "        <write>true</write>\n" +
                "        <manage>true</manage>\n" +
                "      </access>\n" +
                "      <lastUpdatedBy id=\"xE7jOejl9FI\"/>\n" +
                "      <jobParameters>\n" +
                "        <lastYears>2</lastYears>\n" +
                "        <skipResourceTables>true</skipResourceTables>\n" +
                "        <skipTableTypes>\n" +
                "        </skipTableTypes>" +
                "      </jobParameters>\n" +
                "      <cronExpression>0 0 12 ? * MON-FRI</cronExpression>\n" +
                "    </jobConfiguration>",
            JobConfiguration.class );
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za

        Assert.assertEquals( "uB9oC4R2nTn", jc.getUid() );
        Assert.assertEquals( JobStatus.SCHEDULED, jc.getJobStatus() );
        Assert.assertEquals( "Test Analytic", jc.getName() );
        Assert.assertEquals( "Test Analytic", jc.getDisplayName() );
        Assert.assertTrue( jc.isEnabled() );
        Assert.assertTrue( jc.isLeaderOnlyJob() );
        Assert.assertEquals( JobType.ANALYTICS_TABLE, jc.getJobType() );
        Assert.assertNull( jc.getNextExecutionTime() );
        Assert.assertEquals( "0 0 12 ? * MON-FRI", jc.getCronExpression() );

        Assert.assertNotNull( jc.getJobParameters() );
        Assert.assertEquals( (Integer) 2, ((AnalyticsJobParameters) jc.getJobParameters()).getLastYears() );
        Assert.assertTrue( ((AnalyticsJobParameters) jc.getJobParameters()).isSkipResourceTables() );
        Assert.assertNotNull( ((AnalyticsJobParameters) jc.getJobParameters()).getSkipTableTypes() );
        Assert.assertEquals( 0, ((AnalyticsJobParameters) jc.getJobParameters()).getSkipTableTypes().size() );
    }

    @Test
    public void xmlWithJson()
        throws IOException
    {
        final XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false );
        xmlMapper.configure( DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY, false );

<<<<<<< HEAD
        JobConfiguration jc = xmlMapper.readValue( "<jobConfiguration lastUpdated=\"2019-03-26T22:57:44.649\" id=\"uB9oC4R2nTn\" created=\"2019-03-26T22:57:44.649\" name=\"Test Analytic\">\n" +
            "      <jobStatus>SCHEDULED</jobStatus>\n" +
            "      <displayName>Test Analytic</displayName>\n" +
            "      <enabled>true</enabled>\n" +
            "      <leaderOnlyJob>true</leaderOnlyJob>\n" +
            "      <externalAccess>false</externalAccess>\n" +
            "      <jobType>ANALYTICS_TABLE</jobType>\n" +
            "      <nextExecutionTime>2019-03-27T12:00:00.000</nextExecutionTime>\n" +
            "      <favorite>false</favorite>\n" +
            "      <configurable>true</configurable>\n" +
            "      <access>\n" +
            "        <read>true</read>\n" +
            "        <update>true</update>\n" +
            "        <externalize>false</externalize>\n" +
            "        <delete>true</delete>\n" +
            "        <write>true</write>\n" +
            "        <manage>true</manage>\n" +
            "      </access>\n" +
            "      <lastUpdatedBy id=\"xE7jOejl9FI\"/>\n" +
            "      <jobParameters>\n" +
            "        <lastYears>2</lastYears>\n" +
            "        <skipResourceTables>true</skipResourceTables>\n" +
            "        <skipTableTypes>\n" +
            "          <skipTableType>ENROLLMENT</skipTableType>\n" +
            "          <skipTableType>ORG_UNIT_TARGET</skipTableType>\n" +
            "          <skipTableType>VALIDATION_RESULT</skipTableType>\n" +
            "        </skipTableTypes>" +
            "      </jobParameters>\n" +
            "      <cronExpression>0 0 12 ? * MON-FRI</cronExpression>\n" +
            "    </jobConfiguration>", JobConfiguration.class );
=======
        JobConfiguration jc = xmlMapper.readValue(
            "<jobConfiguration lastUpdated=\"2019-03-26T22:57:44.649\" id=\"uB9oC4R2nTn\" created=\"2019-03-26T22:57:44.649\" name=\"Test Analytic\">\n"
                +
                "      <jobStatus>SCHEDULED</jobStatus>\n" +
                "      <displayName>Test Analytic</displayName>\n" +
                "      <enabled>true</enabled>\n" +
                "      <leaderOnlyJob>true</leaderOnlyJob>\n" +
                "      <externalAccess>false</externalAccess>\n" +
                "      <jobType>ANALYTICS_TABLE</jobType>\n" +
                "      <nextExecutionTime>2019-03-27T12:00:00.000</nextExecutionTime>\n" +
                "      <favorite>false</favorite>\n" +
                "      <configurable>true</configurable>\n" +
                "      <access>\n" +
                "        <read>true</read>\n" +
                "        <update>true</update>\n" +
                "        <externalize>false</externalize>\n" +
                "        <delete>true</delete>\n" +
                "        <write>true</write>\n" +
                "        <manage>true</manage>\n" +
                "      </access>\n" +
                "      <lastUpdatedBy id=\"xE7jOejl9FI\"/>\n" +
                "      <jobParameters>\n" +
                "        <lastYears>2</lastYears>\n" +
                "        <skipResourceTables>true</skipResourceTables>\n" +
                "        <skipTableTypes>\n" +
                "          <skipTableType>ENROLLMENT</skipTableType>\n" +
                "          <skipTableType>ORG_UNIT_TARGET</skipTableType>\n" +
                "          <skipTableType>VALIDATION_RESULT</skipTableType>\n" +
                "        </skipTableTypes>" +
                "      </jobParameters>\n" +
                "      <cronExpression>0 0 12 ? * MON-FRI</cronExpression>\n" +
                "    </jobConfiguration>",
            JobConfiguration.class );
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za

        Assert.assertEquals( JobStatus.SCHEDULED, jc.getJobStatus() );
        Assert.assertEquals( "Test Analytic", jc.getName() );
        Assert.assertEquals( "Test Analytic", jc.getDisplayName() );
        Assert.assertTrue( jc.isEnabled() );
        Assert.assertTrue( jc.isLeaderOnlyJob() );
        Assert.assertEquals( JobType.ANALYTICS_TABLE, jc.getJobType() );
        Assert.assertNull( jc.getNextExecutionTime() );
        Assert.assertEquals( "0 0 12 ? * MON-FRI", jc.getCronExpression() );

        Assert.assertNotNull( jc.getJobParameters() );
        Assert.assertEquals( (Integer) 2, ((AnalyticsJobParameters) jc.getJobParameters()).getLastYears() );
        Assert.assertTrue( ((AnalyticsJobParameters) jc.getJobParameters()).isSkipResourceTables() );
        Assert.assertNotNull( ((AnalyticsJobParameters) jc.getJobParameters()).getSkipTableTypes() );
        Assert.assertEquals( 3, ((AnalyticsJobParameters) jc.getJobParameters()).getSkipTableTypes().size() );
        Assert.assertThat( ((AnalyticsJobParameters) jc.getJobParameters()).getSkipTableTypes(), Matchers.hasItems(
            AnalyticsTableType.ENROLLMENT, AnalyticsTableType.ORG_UNIT_TARGET, AnalyticsTableType.VALIDATION_RESULT ) );
    }

    @Test
    public void json()
        throws IOException
    {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false );
        objectMapper.configure( DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY, false );

        JobConfiguration jc = objectMapper.readValue( "{\n" +
            "      \"lastUpdated\": \"2018-05-18T09:40:19.561\",\n" +
            "      \"id\": \"sHMedQF7VYa\",\n" +
            "      \"created\": \"2017-12-08T12:45:18.351\",\n" +
            "      \"name\": \"Test Analytic\",\n" +
            "      \"jobStatus\": \"SCHEDULED\",\n" +
            "      \"displayName\": \"Test Analytic\",\n" +
            "      \"enabled\": true,\n" +
            "      \"leaderOnlyJob\": true,\n" +
            "      \"externalAccess\": false,\n" +
            "      \"jobType\": \"ANALYTICS_TABLE\",\n" +
            "      \"nextExecutionTime\": \"2019-03-27T02:00:00.000\",\n" +
            "      \"cronExpression\": \"0 0 12 ? * MON-FRI\",\n" +
            "      \"lastRuntimeExecution\": \"00:00:00.060\",\n" +
            "      \"lastExecutedStatus\": \"COMPLETED\",\n" +
            "      \"lastExecuted\": \"2018-02-22T09:31:21.906\",\n" +
            "      \"favorite\": false,\n" +
            "      \"configurable\": false,\n" +
            "      \"access\": {\n" +
            "        \"read\": true,\n" +
            "        \"update\": true,\n" +
            "        \"externalize\": false,\n" +
            "        \"delete\": true,\n" +
            "        \"write\": true,\n" +
            "        \"manage\": true\n" +
            "      },\n" +
            "      \"jobParameters\":{\"lastYears\":2,\"skipResourceTables\":true,\"skipTableTypes\":[\"ENROLLMENT\",\"ORG_UNIT_TARGET\",\"VALIDATION_RESULT\"]},"
            +
            "      \"favorites\": [],\n" +
            "      \"translations\": [],\n" +
            "      \"userGroupAccesses\": [],\n" +
            "      \"attributeValues\": [],\n" +
            "      \"userAccesses\": []\n" +
            "    },", JobConfiguration.class );

        Assert.assertEquals( JobStatus.SCHEDULED, jc.getJobStatus() );
        Assert.assertEquals( "Test Analytic", jc.getName() );
        Assert.assertEquals( "Test Analytic", jc.getDisplayName() );
        Assert.assertTrue( jc.isEnabled() );
        Assert.assertTrue( jc.isLeaderOnlyJob() );
        Assert.assertEquals( JobType.ANALYTICS_TABLE, jc.getJobType() );
        Assert.assertNull( jc.getNextExecutionTime() );
        Assert.assertEquals( "0 0 12 ? * MON-FRI", jc.getCronExpression() );

        Assert.assertNotNull( jc.getJobParameters() );
        Assert.assertEquals( (Integer) 2, ((AnalyticsJobParameters) jc.getJobParameters()).getLastYears() );
        Assert.assertTrue( ((AnalyticsJobParameters) jc.getJobParameters()).isSkipResourceTables() );
        Assert.assertNotNull( ((AnalyticsJobParameters) jc.getJobParameters()).getSkipTableTypes() );
        Assert.assertEquals( 3, ((AnalyticsJobParameters) jc.getJobParameters()).getSkipTableTypes().size() );
        Assert.assertThat( ((AnalyticsJobParameters) jc.getJobParameters()).getSkipTableTypes(), Matchers.hasItems(
            AnalyticsTableType.ENROLLMENT, AnalyticsTableType.ORG_UNIT_TARGET, AnalyticsTableType.VALIDATION_RESULT ) );
    }

    @Test
    public void disabled()
        throws IOException
    {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false );
        objectMapper.configure( DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY, false );

        JobConfiguration jc = objectMapper.readValue( "{\n" +
            "      \"lastUpdated\": \"2018-05-18T09:40:19.561\",\n" +
            "      \"id\": \"sHMedQF7VYa\",\n" +
            "      \"created\": \"2017-12-08T12:45:18.351\",\n" +
            "      \"name\": \"Test Analytic\",\n" +
            "      \"jobStatus\": \"SCHEDULED\",\n" +
            "      \"displayName\": \"Test Analytic\",\n" +
            "      \"enabled\": false,\n" +
            "      \"leaderOnlyJob\": true,\n" +
            "      \"externalAccess\": false,\n" +
            "      \"jobType\": \"ANALYTICS_TABLE\",\n" +
            "      \"nextExecutionTime\": \"2019-03-27T02:00:00.000\",\n" +
            "      \"cronExpression\": \"0 0 12 ? * MON-FRI\",\n" +
            "      \"lastRuntimeExecution\": \"00:00:00.060\",\n" +
            "      \"lastExecutedStatus\": \"COMPLETED\",\n" +
            "      \"lastExecuted\": \"2018-02-22T09:31:21.906\",\n" +
            "      \"favorite\": false,\n" +
            "      \"configurable\": false,\n" +
            "      \"access\": {\n" +
            "        \"read\": true,\n" +
            "        \"update\": true,\n" +
            "        \"externalize\": false,\n" +
            "        \"delete\": true,\n" +
            "        \"write\": true,\n" +
            "        \"manage\": true\n" +
            "      },\n" +
            "      \"jobParameters\":{\"lastYears\":2,\"skipResourceTables\":true,\"skipTableTypes\":[\"ENROLLMENT\",\"ORG_UNIT_TARGET\",\"VALIDATION_RESULT\"]},"
            +
            "      \"favorites\": [],\n" +
            "      \"translations\": [],\n" +
            "      \"userGroupAccesses\": [],\n" +
            "      \"attributeValues\": [],\n" +
            "      \"userAccesses\": []\n" +
            "    },", JobConfiguration.class );

        Assert.assertEquals( JobStatus.DISABLED, jc.getJobStatus() );
        Assert.assertEquals( "Test Analytic", jc.getName() );
        Assert.assertEquals( "Test Analytic", jc.getDisplayName() );
        Assert.assertFalse( jc.isEnabled() );
        Assert.assertTrue( jc.isLeaderOnlyJob() );
        Assert.assertEquals( JobType.ANALYTICS_TABLE, jc.getJobType() );
        Assert.assertNull( jc.getNextExecutionTime() );
        Assert.assertEquals( "0 0 12 ? * MON-FRI", jc.getCronExpression() );

        Assert.assertNotNull( jc.getJobParameters() );
        Assert.assertEquals( (Integer) 2, ((AnalyticsJobParameters) jc.getJobParameters()).getLastYears() );
        Assert.assertTrue( ((AnalyticsJobParameters) jc.getJobParameters()).isSkipResourceTables() );
        Assert.assertNotNull( ((AnalyticsJobParameters) jc.getJobParameters()).getSkipTableTypes() );
        Assert.assertEquals( 3, ((AnalyticsJobParameters) jc.getJobParameters()).getSkipTableTypes().size() );
        Assert.assertThat( ((AnalyticsJobParameters) jc.getJobParameters()).getSkipTableTypes(), Matchers.hasItems(
            AnalyticsTableType.ENROLLMENT, AnalyticsTableType.ORG_UNIT_TARGET, AnalyticsTableType.VALIDATION_RESULT ) );
    }
}
