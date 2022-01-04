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
package org.hisp.dhis.webapi.controller.tracker.imports;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.webapi.controller.tracker.imports.TrackerImportController.TRACKER_JOB_ADDED;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.LinkedList;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig;
import org.hisp.dhis.dxf2.events.event.csv.CsvEventService;
import org.hisp.dhis.render.DefaultRenderService;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.system.notification.Notification;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.tracker.DefaultTrackerImportService;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.report.TrackerBundleReport;
import org.hisp.dhis.tracker.report.TrackerImportReport;
import org.hisp.dhis.tracker.report.TrackerStatus;
import org.hisp.dhis.tracker.report.TrackerTimingsStats;
import org.hisp.dhis.tracker.report.TrackerValidationReport;
import org.hisp.dhis.webapi.controller.exception.NotFoundException;
import org.hisp.dhis.webapi.controller.tracker.TrackerControllerSupport;
import org.hisp.dhis.webapi.service.DefaultContextService;
import org.hisp.dhis.webapi.strategy.tracker.imports.TrackerImportStrategyHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * @author Giuseppe Nespolino <g.nespolino@gmail.com>
 */
@ExtendWith( MockitoExtension.class )
class TrackerImportControllerTest
{

    private final static String ENDPOINT = "/" + TrackerControllerSupport.RESOURCE_PATH;

    private MockMvc mockMvc;

    @Mock
    private DefaultTrackerImportService trackerImportService;

    @Mock
    private TrackerImportStrategyHandler importStrategy;

    @Mock
    private CsvEventService<Event> csvEventService;

    @Mock
    private Notifier notifier;

    private RenderService renderService;

    @BeforeEach
    public void setUp()
    {
        renderService = new DefaultRenderService( JacksonObjectMapperConfig.jsonMapper,
            JacksonObjectMapperConfig.xmlMapper,
            mock( SchemaService.class ) );

        // Controller under test
        final TrackerImportController controller = new TrackerImportController( importStrategy, trackerImportService,
            csvEventService, new DefaultContextService(), notifier );

        mockMvc = MockMvcBuilders.standaloneSetup( controller ).build();
    }

    @Test
    void verifyAsync()
        throws Exception
    {

        // Then
        mockMvc.perform( post( ENDPOINT )
            .content( "{}" )
            .contentType( MediaType.APPLICATION_JSON )
            .accept( MediaType.APPLICATION_JSON ) )
            .andExpect( status().isOk() )
            .andExpect( jsonPath( "$.message" ).value( TRACKER_JOB_ADDED ) )
            .andExpect( content().contentType( "application/json" ) );
    }

    @Test
    void verifyAsyncForCsv()
        throws Exception
    {

        // Then
        mockMvc.perform( post( ENDPOINT )
            .content( "{}" )
            .contentType( "text/csv" ) )
            .andExpect( status().isOk() )
            .andExpect( jsonPath( "$.message" ).value( TRACKER_JOB_ADDED ) )
            .andExpect( content().contentType( "application/json" ) );

        verify( csvEventService ).readEvents( any(), eq( true ) );
        verify( importStrategy ).importReport( any() );
    }

    @Test
    void verifySyncResponseShouldBeOkWhenImportReportStatusIsOk()
        throws Exception
    {
        // When
        when( importStrategy.importReport( any() ) ).thenReturn( TrackerImportReport.withImportCompleted(
            TrackerStatus.OK,
            TrackerBundleReport.builder()
                .status( TrackerStatus.OK )
                .build(),
            TrackerValidationReport.builder()
                .build(),
            new TrackerTimingsStats(),
            new HashMap<>() ) );

        // Then
        String contentAsString = mockMvc.perform( post( ENDPOINT + "?async=false" )
            .content( "{}" )
            .contentType( MediaType.APPLICATION_JSON )
            .accept( MediaType.APPLICATION_JSON ) )
            .andExpect( status().isOk() )
            .andExpect( jsonPath( "$.message" ).doesNotExist() )
            .andExpect( content().contentType( "application/json" ) )
            .andReturn()
            .getResponse()
            .getContentAsString();

        verify( importStrategy ).importReport( any() );

        try
        {
            renderService.fromJson( contentAsString, TrackerImportReport.class );
        }
        catch ( Exception e )
        {
            fail( "response content : " + contentAsString + "\n" + " is not of TrackerImportReport type" );
        }
    }

    @Test
    void verifySyncResponseForCsvShouldBeOkWhenImportReportStatusIsOk()
        throws Exception
    {
        // When
        when( importStrategy.importReport( any() ) ).thenReturn( TrackerImportReport.withImportCompleted(
            TrackerStatus.OK,
            TrackerBundleReport.builder()
                .status( TrackerStatus.OK )
                .build(),
            TrackerValidationReport.builder()
                .build(),
            new TrackerTimingsStats(),
            new HashMap<>() ) );

        // Then
        String contentAsString = mockMvc.perform( post( ENDPOINT + "?async=false&skipFirst=true" )
            .content( "{}" )
            .contentType( "text/csv" ) )
            .andExpect( status().isOk() )
            .andExpect( jsonPath( "$.message" ).doesNotExist() )
            .andExpect( content().contentType( "application/json" ) )
            .andReturn()
            .getResponse()
            .getContentAsString();

        verify( csvEventService ).readEvents( any(), eq( true ) );
        verify( importStrategy ).importReport( any() );

        try
        {
            renderService.fromJson( contentAsString, TrackerImportReport.class );
        }
        catch ( Exception e )
        {
            fail( "response content : " + contentAsString + "\n" + " is not of TrackerImportReport type" );
        }
    }

    @Test
    void verifySyncResponseShouldBeConflictWhenImportReportStatusIsError()
        throws Exception
    {
        String errorMessage = "errorMessage";
        // When
        when( importStrategy.importReport( any() ) ).thenReturn( TrackerImportReport.withError( "errorMessage",
            TrackerValidationReport.builder()
                .build(),
            new TrackerTimingsStats() ) );

        // Then
        String contentAsString = mockMvc.perform( post( ENDPOINT + "?async=false" )
            .content( "{}" )
            .contentType( MediaType.APPLICATION_JSON )
            .accept( MediaType.APPLICATION_JSON ) )
            .andExpect( status().isConflict() )
            .andExpect( jsonPath( "$.message" ).value( errorMessage ) )
            .andExpect( content().contentType( "application/json" ) )
            .andReturn()
            .getResponse()
            .getContentAsString();

        verify( importStrategy ).importReport( any() );

        try
        {
            renderService.fromJson( contentAsString, TrackerImportReport.class );
        }
        catch ( Exception e )
        {
            fail( "response content : " + contentAsString + "\n" + " is not of TrackerImportReport type" );
        }
    }

    @Test
    void verifySyncResponseForCsvShouldBeConflictWhenImportReportStatusIsError()
        throws Exception
    {
        String errorMessage = "errorMessage";
        // When
        when( importStrategy.importReport( any() ) ).thenReturn( TrackerImportReport.withError( "errorMessage",
            TrackerValidationReport.builder()
                .build(),
            new TrackerTimingsStats() ) );

        // Then
        String contentAsString = mockMvc.perform( post( ENDPOINT + "?async=false&skipFirst=true" )
            .content( "{}" )
            .contentType( "text/csv" ) )
            .andExpect( status().isConflict() )
            .andExpect( jsonPath( "$.message" ).value( errorMessage ) )
            .andExpect( content().contentType( "application/json" ) )
            .andReturn()
            .getResponse()
            .getContentAsString();

        verify( csvEventService ).readEvents( any(), eq( true ) );
        verify( importStrategy ).importReport( any() );

        try
        {
            renderService.fromJson( contentAsString, TrackerImportReport.class );
        }
        catch ( Exception e )
        {
            fail( "response content : " + contentAsString + "\n" + " is not of TrackerImportReport type" );
        }
    }

    @Test
    void verifyShouldFindJob()
        throws Exception
    {
        String uid = CodeGenerator.generateUid();
        // When
        when( notifier.getNotificationsByJobId( JobType.TRACKER_IMPORT_JOB, uid ) )
            .thenReturn( new LinkedList<>( singletonList( new Notification() ) ) );

        // Then
        mockMvc.perform( get( ENDPOINT + "/jobs/" + uid )
            .content( "{}" )
            .contentType( MediaType.APPLICATION_JSON )
            .accept( MediaType.APPLICATION_JSON ) )
            .andExpect( status().isOk() )
            .andExpect( jsonPath( "$.message" ).doesNotExist() )
            .andExpect( jsonPath( "$" ).isArray() )
            .andExpect( jsonPath( "$", hasSize( 1 ) ) )
            .andExpect( jsonPath( "$.[0].uid" ).isNotEmpty() )
            .andExpect( content().contentType( "application/json" ) )
            .andReturn()
            .getResponse()
            .getContentAsString();

        verify( notifier ).getNotificationsByJobId( JobType.TRACKER_IMPORT_JOB, uid );
    }

    @Test
    void verifyShouldFindJobReport()
        throws Exception
    {
        String uid = CodeGenerator.generateUid();

        TrackerImportReport trackerImportReport = TrackerImportReport.withImportCompleted(
            TrackerStatus.OK,
            TrackerBundleReport.builder()
                .status( TrackerStatus.OK )
                .build(),
            TrackerValidationReport.builder()
                .build(),
            new TrackerTimingsStats(),
            new HashMap<>() );

        // When
        when( notifier.getJobSummaryByJobId( JobType.TRACKER_IMPORT_JOB, uid ) )
            .thenReturn( trackerImportReport );

        when( trackerImportService.buildImportReport( any(), any() ) ).thenReturn( trackerImportReport );

        // Then
        String contentAsString = mockMvc.perform( get( ENDPOINT + "/jobs/" + uid + "/report" )
            .content( "{}" )
            .contentType( MediaType.APPLICATION_JSON )
            .accept( MediaType.APPLICATION_JSON ) )
            .andExpect( status().isOk() )
            .andExpect( jsonPath( "$.message" ).doesNotExist() )
            .andExpect( content().contentType( "application/json" ) )
            .andReturn()
            .getResponse()
            .getContentAsString();

        verify( notifier ).getJobSummaryByJobId( JobType.TRACKER_IMPORT_JOB, uid );
        verify( trackerImportService ).buildImportReport( any(), any() );

        try
        {
            renderService.fromJson( contentAsString, TrackerImportReport.class );
        }
        catch ( Exception e )
        {
            fail( "response content : " + contentAsString + "\n" + " is not of TrackerImportReport type" );
        }
    }

    @Test
    void verifyShouldThrowWhenJobReportNotFound()
        throws Exception
    {
        String uid = CodeGenerator.generateUid();

        // When
        when( notifier.getJobSummaryByJobId( JobType.TRACKER_IMPORT_JOB, uid ) )
            .thenReturn( null );

        // Then
        mockMvc.perform( get( ENDPOINT + "/jobs/" + uid + "/report" )
            .content( "{}" )
            .contentType( MediaType.APPLICATION_JSON )
            .accept( MediaType.APPLICATION_JSON ) ).andExpect( status().isNotFound() )
            .andExpect( result -> assertTrue( result.getResolvedException() instanceof NotFoundException ) );
    }
}
