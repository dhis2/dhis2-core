/*
 * Copyright (c) 2004-2021, University of Oslo
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
import static org.hisp.dhis.webapi.utils.WebClientUtils.assertStatus;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.JsonObject;
import org.junit.Test;
import org.springframework.http.HttpStatus;

/**
 * Tests the
 * {@link org.hisp.dhis.webapi.controller.scheduling.JobConfigurationController}.
 *
 * Since test setup uses a mock for the
 * {@link org.hisp.dhis.scheduling.SchedulingManager} the actual scheduling
 * cannot be tested.
 *
 * This tests focuses on creation including the serialiastion of job parameters.
 *
 * @author Jan Bernitt
 */
public class JobConfigurationControllerTest extends DhisControllerConvenienceTest
{
    @Test
    public void testCONTINUOUS_ANALYTICS_TABLE()
    {
        String jobId = assertStatus( HttpStatus.CREATED, POST( "/jobConfigurations",
            "{'name':'test','jobType':'CONTINUOUS_ANALYTICS_TABLE','delay':'1'," +
                "'jobParameters':{'fullUpdateHourOfDay':'1','lastYears':'2'," +
                "'skipTableTypes':['DATA_VALUE','COMPLETENESS','COMPLETENESS_TARGET','ORG_UNIT_TARGET','EVENT','ENROLLMENT','VALIDATION_RESULT']}}" ) );

        JsonObject parameters = assertJobConfigurationExists( jobId, "CONTINUOUS_ANALYTICS_TABLE" );

        assertEquals( 1, parameters.getNumber( "fullUpdateHourOfDay" ).intValue() );
        assertEquals( 2, parameters.getNumber( "lastYears" ).intValue() );
        assertContainsOnly( parameters.getArray( "skipTableTypes" ).stringValues(),
            "ENROLLMENT", "VALIDATION_RESULT", "DATA_VALUE", "COMPLETENESS", "EVENT", "ORG_UNIT_TARGET",
            "COMPLETENESS_TARGET" );
    }

    @Test
    public void testDISABLE_INACTIVE_USERS()
    {
        String jobId = assertStatus( HttpStatus.CREATED, POST( "/jobConfigurations",
            "{'name':'test','jobType':'DISABLE_INACTIVE_USERS','cronExpression':'0 0 1 ? * *','jobParameters':{'inactiveMonths':'3'}}" ) );

        JsonObject parameters = assertJobConfigurationExists( jobId, "DISABLE_INACTIVE_USERS" );
        assertEquals( 3, parameters.getNumber( "inactiveMonths" ).intValue() );
    }

    @Test
    public void testDATA_INTEGRITY()
    {
        String jobId = assertStatus( HttpStatus.CREATED, POST( "/jobConfigurations",
            "{'name':'test','jobType':'DATA_INTEGRITY','cronExpression':'0 0 12 ? * MON-FRI'}" ) );

        JsonObject parameters = assertJobConfigurationExists( jobId, "DATA_INTEGRITY" );
        assertFalse( parameters.exists() );
    }

    @Test
    public void testRESOURCE_TABLE()
    {
        String jobId = assertStatus( HttpStatus.CREATED, POST( "/jobConfigurations",
            "{'name':'test','jobType':'RESOURCE_TABLE','cronExpression':'0 0 3 ? * MON'}" ) );

        JsonObject parameters = assertJobConfigurationExists( jobId, "RESOURCE_TABLE" );
        assertFalse( parameters.exists() );
    }

    @Test
    public void testANALYTICS_TABLE()
    {
        String jobId = assertStatus( HttpStatus.CREATED, POST( "/jobConfigurations",
            "{'name':'test','jobType':'ANALYTICS_TABLE','cronExpression':'0 0 3 ? * *'," +
                "'jobParameters':{'lastYears':'1','skipTableTypes':['DATA_VALUE','COMPLETENESS','ENROLLMENT'],'skipResourceTables':true}}" ) );

        JsonObject parameters = assertJobConfigurationExists( jobId, "ANALYTICS_TABLE" );
        assertEquals( 1, parameters.getNumber( "lastYears" ).intValue() );
        assertTrue( parameters.getBoolean( "skipResourceTables" ).booleanValue() );
        assertContainsOnly( parameters.getArray( "skipTableTypes" ).stringValues(),
            "DATA_VALUE", "COMPLETENESS", "ENROLLMENT" );
    }

    @Test
    public void testPROGRAM_NOTIFICATIONS()
    {
        String jobId = assertStatus( HttpStatus.CREATED, POST( "/jobConfigurations",
            "{'name':'test','jobType':'PROGRAM_NOTIFICATIONS','cronExpression':'0 0 1 ? * *'}" ) );

        JsonObject parameters = assertJobConfigurationExists( jobId, "PROGRAM_NOTIFICATIONS" );
        assertFalse( parameters.exists() );
    }

    @Test
    public void testMONITORING()
    {
        String jobId = assertStatus( HttpStatus.CREATED, POST( "/jobConfigurations",
            "{'name':'test','jobType':'MONITORING','cronExpression':'0 0 12 ? * MON-FRI'," +
                "'jobParameters':{'relativeStart':'1','relativeEnd':'2','validationRuleGroups':[],'sendNotifications':true,'persistResults':true}}" ) );

        JsonObject parameters = assertJobConfigurationExists( jobId, "MONITORING" );
        assertEquals( 1, parameters.getNumber( "relativeStart" ).intValue() );
        assertEquals( 2, parameters.getNumber( "relativeEnd" ).intValue() );
        assertTrue( parameters.getBoolean( "sendNotifications" ).booleanValue() );
        assertTrue( parameters.getBoolean( "persistResults" ).booleanValue() );
        assertTrue( parameters.getArray( "validationRuleGroups" ).isEmpty() );
    }

    private JsonObject assertJobConfigurationExists( String jobId, String expectedJobType )
    {
        JsonObject jobConfiguration = GET( "/jobConfigurations/{id}", jobId ).content();

        assertEquals( jobId, jobConfiguration.getString( "id" ).string() );
        assertEquals( "test", jobConfiguration.getString( "name" ).string() );
        assertEquals( "SCHEDULED", jobConfiguration.getString( "jobStatus" ).string() );
        assertTrue( jobConfiguration.getBoolean( "enabled" ).booleanValue() );
        assertEquals( expectedJobType, jobConfiguration.getString( "jobType" ).string() );
        return jobConfiguration.getObject( "jobParameters" );
    }
}
