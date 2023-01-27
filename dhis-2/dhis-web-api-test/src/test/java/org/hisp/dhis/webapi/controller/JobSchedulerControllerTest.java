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

import static java.lang.String.format;
import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonString;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the
 * {@link org.hisp.dhis.webapi.controller.scheduling.JobSchedulerController}.
 *
 * @author Jan Bernitt
 */
class JobSchedulerControllerTest extends DhisControllerConvenienceTest
{
    private String jobIdA;

    private String jobIdB;

    private String jobIdC;

    @BeforeEach
    void setUp()
    {
        jobIdA = assertStatus( HttpStatus.CREATED, POST( "/jobConfigurations",
            "{'name':'a','jobType':'DISABLE_INACTIVE_USERS','cronExpression':'0 0 1 ? * *','jobParameters':{'inactiveMonths':'3'}}" ) );
        jobIdB = assertStatus( HttpStatus.CREATED, POST( "/jobConfigurations",
            "{'name':'b','jobType':'DATA_INTEGRITY','cronExpression':'0 0 12 ? * MON-FRI'}" ) );
        jobIdC = assertStatus( HttpStatus.CREATED, POST( "/jobConfigurations",
            "{'name':'c','jobType':'RESOURCE_TABLE','cronExpression':'0 0 3 ? * MON'}" ) );
    }

    @Test
    void testCreateQueue()
    {
        assertStatus( HttpStatus.CREATED, POST( "/scheduler/queues/testQueue",
            format( "{'cronExpression':'0 0 1 ? * *','sequence':['%s','%s','%s']}", jobIdA, jobIdB, jobIdC ) ) );
    }

    @Test
    void testCreateQueue_QueueAlreadyExists()
    {
        assertStatus( HttpStatus.CREATED, POST( "/scheduler/queues/testQueue",
            format( "{'cronExpression':'0 0 1 ? * *','sequence':['%s','%s','%s']}", jobIdA, jobIdB, jobIdC ) ) );

        JsonWebMessage message = assertWebMessage( "Conflict", 409, "ERROR",
            "Job queue already exist: `testQueue`",
            POST( "/scheduler/queues/testQueue", "{'cronExpression':'0 0 1 ? * *','sequence':[]}" )
                .content( HttpStatus.CONFLICT ) );
        assertEquals( ErrorCode.E7021, message.getErrorCode() );
    }

    @Test
    void testCreateQueue_JobInSequenceDoesNotExist()
    {
        assertWebMessage( "Not Found", 404, "ERROR",
            "JobConfiguration with id no-uid could not be found.",
            POST( "/scheduler/queues/testQueue",
                format( "{'cronExpression':'0 0 1 ? * *','sequence':['%s','%s','%s']}", jobIdA, "no-uid", jobIdC ) )
                    .content( HttpStatus.NOT_FOUND ) );
    }

    @Test
    void testCreateQueue_OnlyOnJobInSequence()
    {
        assertWebMessage( "Conflict", 409, "ERROR",
            "Job queue must have at least two jobs.",
            POST( "/scheduler/queues/testQueue",
                format( "{'cronExpression':'0 0 1 ? * *','sequence':['%s']}", jobIdA ) )
                    .content( HttpStatus.CONFLICT ) );
    }

    @Test
    void testCreateQueue_JobAlreadyUsedInOtherQueue()
    {
        assertStatus( HttpStatus.CREATED, POST( "/scheduler/queues/Q1",
            format( "{'cronExpression':'0 0 1 ? * *','sequence':['%s','%s']}", jobIdA, jobIdB ) ) );

        JsonWebMessage message = assertWebMessage( "Conflict", 409, "ERROR",
            "Job `" + jobIdA + "` is already used in another queue: `Q1`",
            POST( "/scheduler/queues/Q2",
                format( "{'cronExpression':'0 0 1 ? * *','sequence':['%s','%s']}", jobIdA, jobIdC ) ).content(
                    HttpStatus.CONFLICT ) );
        assertEquals( ErrorCode.E7022, message.getErrorCode() );
    }

    @Test
    void testCreateQueue_IllegalCronExpression()
    {
        assertWebMessage( "Conflict", 409, "ERROR",
            "Cron expression is invalid: `Cron expression must consist of 6 fields (found 5 in \"0 1 ? * *\")`",
            POST( "/scheduler/queues/testQueue",
                format( "{'cronExpression':'0 1 ? * *','sequence':['%s','%s']}", jobIdA, jobIdB ) )
                    .content( HttpStatus.CONFLICT ) );
    }

    @Test
    void testGetQueue()
    {
        assertStatus( HttpStatus.CREATED, POST( "/scheduler/queues/testQueue",
            format( "{'cronExpression':'0 0 1 ? * *','sequence':['%s','%s','%s']}", jobIdA, jobIdB, jobIdC ) ) );

        assertEquals( List.of( jobIdA, jobIdB, jobIdC ), GET( "/scheduler/queues/testQueue" )
            .content().getArray( "sequence" ).stringValues() );
    }

    @Test
    void testGetQueue_NotExistingQueue()
    {
        JsonWebMessage message = assertWebMessage( "Not Found", 404, "ERROR",
            "Job queue does not exist: `foo`",
            GET( "/scheduler/queues/foo" ).content( HttpStatus.NOT_FOUND ) );
        assertEquals( ErrorCode.E7020, message.getErrorCode() );
    }

    @Test
    void testGetQueueNames()
    {
        assertStatus( HttpStatus.CREATED, POST( "/scheduler/queues/testQueue",
            format( "{'cronExpression':'0 0 1 ? * *','sequence':['%s','%s','%s']}", jobIdA, jobIdB, jobIdC ) ) );

        assertEquals( List.of( "testQueue" ), GET( "/scheduler/queues/" ).content().stringValues() );
    }

    @Test
    void testGetQueueNames_NoQueuesExist()
    {
        JsonArray names = GET( "/scheduler/queues/" ).content();
        assertTrue( names.isArray() );
        assertTrue( names.isEmpty() );
    }

    @Test
    void testUpdateQueue()
    {
        assertStatus( HttpStatus.CREATED, POST( "/scheduler/queues/testQueue",
            format( "{'cronExpression':'0 0 1 ? * *','sequence':['%s','%s']}", jobIdA, jobIdC ) ) );

        assertStatus( HttpStatus.NO_CONTENT, PUT( "/scheduler/queues/testQueue",
            format( "{'cronExpression':'0 0 2 ? * *','sequence':['%s','%s']}", jobIdB, jobIdA ) ) );

        JsonObject queue = GET( "/scheduler/queues/testQueue" ).content();
        assertEquals( "0 0 2 ? * *", queue.getString( "cronExpression" ).string() );
        assertEquals( List.of( jobIdB, jobIdA ), queue.getArray( "sequence" ).stringValues() );
    }

    @Test
    void testUpdateQueue_NotExistingQueue()
    {
        JsonWebMessage message = assertWebMessage( "Not Found", 404, "ERROR",
            "Job queue does not exist: `foo`",
            PUT( "/scheduler/queues/foo", "{}" ).content( HttpStatus.NOT_FOUND ) );
        assertEquals( ErrorCode.E7020, message.getErrorCode() );
    }

    @Test
    void testUpdateQueue_IllegalCronExpression()
    {
        assertStatus( HttpStatus.CREATED, POST( "/scheduler/queues/testQueue",
            format( "{'cronExpression':'0 0 1 ? * *','sequence':['%s','%s']}", jobIdA, jobIdC ) ) );

        assertWebMessage( "Conflict", 409, "ERROR",
            "Cron expression is invalid: `Cron expression must consist of 6 fields (found 5 in \"0 1 ? * *\")`",
            PUT( "/scheduler/queues/testQueue",
                format( "{'cronExpression':'0 1 ? * *','sequence':['%s','%s']}", jobIdA, jobIdB ) )
                    .content( HttpStatus.CONFLICT ) );
    }

    @Test
    void testDeleteQueue()
    {
        assertStatus( HttpStatus.CREATED, POST( "/scheduler/queues/testQueue",
            format( "{'cronExpression':'0 0 1 ? * *','sequence':['%s','%s','%s']}", jobIdA, jobIdB, jobIdC ) ) );

        assertStatus( HttpStatus.NO_CONTENT, DELETE( "/scheduler/queues/testQueue" ) );
        assertStatus( HttpStatus.NOT_FOUND, GET( "/scheduler/queues/testQueue" ) );
    }

    @Test
    void testDeleteQueue_NotExistingQueue()
    {
        JsonWebMessage message = assertWebMessage( "Not Found", 404, "ERROR",
            "Job queue does not exist: `foo`",
            DELETE( "/scheduler/queues/foo" ).content( HttpStatus.NOT_FOUND ) );
        assertEquals( ErrorCode.E7020, message.getErrorCode() );
    }

    @Test
    void testGetSchedulerEntries_NoQueues()
    {
        JsonArray entries = GET( "/scheduler?order=name" ).content();

        assertEquals( 3, entries.size() );
        assertEquals( List.of( "a", "b", "c" ), entries.asList( JsonObject.class )
            .viewAsList( entry -> entry.getString( "name" ) ).toList( JsonString::string ) );

        JsonObject jobA = entries.getObject( 0 );
        assertEquals( "a", jobA.getString( "name" ).string() );
        assertEquals( "DISABLE_INACTIVE_USERS", jobA.getString( "type" ).string() );
        assertEquals( "0 0 1 ? * *", jobA.getString( "cronExpression" ).string() );
        assertTrue( jobA.getString( "nextExecutionTime" ).string().startsWith( "202" ) );
        assertTrue( jobA.getBoolean( "configurable" ).booleanValue() );
        assertEquals( jobIdA, jobA.getArray( "sequence" ).getObject( 0 ).getString( "id" ).string() );
    }

    @Test
    void testGetSchedulerEntries_NoQueuesNoSort()
    {
        JsonArray entries = GET( "/scheduler" ).content();

        assertEquals( 3, entries.size() );
        assertEquals( Set.of( "a", "b", "c" ), Set.copyOf( entries.asList( JsonObject.class )
            .viewAsList( entry -> entry.getString( "name" ) ).toList( JsonString::string ) ) );
    }

    @Test
    void testGetSchedulerEntries()
    {
        assertStatus( HttpStatus.CREATED, POST( "/scheduler/queues/testQueue",
            format( "{'cronExpression':'0 0 1 ? * *','sequence':['%s','%s']}", jobIdA, jobIdC ) ) );

        JsonArray entries = GET( "/scheduler?order=name" ).content();

        assertEquals( 2, entries.size() );
        assertEquals( List.of( "b", "testQueue" ), entries.asList( JsonObject.class )
            .viewAsList( entry -> entry.getString( "name" ) ).toList( JsonString::string ) );
        assertEquals( List.of( "a", "c" ), entries.getObject( 1 ).getArray( "sequence" )
            .asList( JsonObject.class ).viewAsList(
                entry -> entry.getString( "name" ) )
            .toList( JsonString::string ) );
    }

    @Test
    void testGetQueueableJobs()
    {
        assertStatus( HttpStatus.CREATED, POST( "/scheduler/queues/testQueue",
            format( "{'cronExpression':'0 0 1 ? * *','sequence':['%s','%s']}", jobIdA, jobIdC ) ) );

        assertEquals( List.of( "b" ), GET( "/scheduler/queueable" ).content()
            .asList( JsonObject.class ).viewAsList(
                entry -> entry.getString( "name" ) )
            .toList( JsonString::string ) );

        assertEquals( List.of( "b" ), GET( "/scheduler/queueable?name=testQueue" ).content()
            .asList( JsonObject.class ).viewAsList(
                entry -> entry.getString( "name" ) )
            .toList( JsonString::string ) );
    }

    @Test
    void testCreateQueueWithTestJobs()
    {
        jobIdA = assertStatus( HttpStatus.CREATED, POST( "/jobConfigurations",
            "{'name':'t1','jobType':'TEST','cronExpression':'0 0 1 ? * *','jobParameters':{}}" ) );
        jobIdB = assertStatus( HttpStatus.CREATED, POST( "/jobConfigurations",
            "{'name':'t2','jobType':'TEST','cronExpression':'0 0 2 ? * *','jobParameters':{'items':12, 'failAtItem':7}}" ) );
        jobIdC = assertStatus( HttpStatus.CREATED, POST( "/jobConfigurations",
            "{'name':'t3','jobType':'TEST','cronExpression':'0 0 3 ? * *','jobParameters':{'itemDuration':200}}" ) );

        assertStatus( HttpStatus.CREATED, POST( "/scheduler/queues/testQueue",
            format( "{'cronExpression':'0 0 1 ? * *','sequence':['%s','%s','%s']}", jobIdA, jobIdB, jobIdC ) ) );
    }
}
