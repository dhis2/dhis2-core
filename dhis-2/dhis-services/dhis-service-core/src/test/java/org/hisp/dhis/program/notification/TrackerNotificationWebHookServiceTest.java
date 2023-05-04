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
package org.hisp.dhis.program.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Stream;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentService;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.EventService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Zubair Asghar
 */

@ExtendWith( MockitoExtension.class )
class TrackerNotificationWebHookServiceTest extends DhisConvenienceTest
{

    private static final String URL = "https://www.google.com";

    private DataElement dataElement;

    private EventDataValue dataValue;

    private TrackedEntityAttribute trackedEntityAttribute;

    private TrackedEntityAttributeValue trackedEntityAttributeValue;

    private ProgramTrackedEntityAttribute programTrackedEntityAttribute;

    private OrganisationUnit organisationUnitA;

    private Program programA;

    private ProgramStage programStageA;

    private Enrollment enrollment;

    private Event event;

    private ProgramNotificationTemplate programNotification;

    private ProgramNotificationTemplate programStageNotification;

    private ResponseEntity<String> responseEntity;

    @Mock
    private EnrollmentService enrollmentService;

    @Mock
    private EventService eventService;

    @Mock
    private ProgramNotificationTemplateService templateService;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RenderService renderService;

    @InjectMocks
    private DefaultTrackerNotificationWebHookService subject;

    @BeforeEach
    public void initTest()
    {
        trackedEntityAttribute = createTrackedEntityAttribute( 'A' );
        dataElement = createDataElement( 'D' );
        organisationUnitA = createOrganisationUnit( 'A' );
        programA = createProgram( 'A', new HashSet<>(), organisationUnitA );
        programTrackedEntityAttribute = createProgramTrackedEntityAttribute( programA, trackedEntityAttribute );
        programA.getProgramAttributes().add( programTrackedEntityAttribute );
        TrackedEntityInstance tei = createTrackedEntityInstance( organisationUnitA );
        trackedEntityAttributeValue = createTrackedEntityAttributeValue( 'I', tei, trackedEntityAttribute );
        tei.getTrackedEntityAttributeValues().add( trackedEntityAttributeValue );

        programStageA = createProgramStage( 'A', programA );

        enrollment = new Enrollment();
        enrollment.setAutoFields();
        enrollment.setProgram( programA );
        enrollment.setOrganisationUnit( organisationUnitA );
        enrollment.setEnrollmentDate( new Date() );
        enrollment.setIncidentDate( new Date() );
        enrollment.setEntityInstance( tei );

        event = new Event();
        event.setAutoFields();
        event.setProgramStage( programStageA );
        event.setOrganisationUnit( organisationUnitA );
        enrollment.setEnrollmentDate( new Date() );
        event.setExecutionDate( new Date() );
        event.setDueDate( new Date() );
        event.setEnrollment( enrollment );

        dataValue = new EventDataValue();
        dataValue.setValue( "dataValue123" );
        dataValue.setDataElement( dataElement.getUid() );
        dataValue.setAutoFields();
        event.getEventDataValues().add( dataValue );

        programNotification = new ProgramNotificationTemplate();
        programNotification.setNotificationRecipient( ProgramNotificationRecipient.WEB_HOOK );
        programNotification.setMessageTemplate( URL );
        programNotification.setDeliveryChannels( Sets.newHashSet( DeliveryChannel.HTTP ) );

        programA.setNotificationTemplates( Sets.newHashSet( programNotification ) );

        programStageNotification = new ProgramNotificationTemplate();
        programStageNotification.setNotificationRecipient( ProgramNotificationRecipient.WEB_HOOK );
        programStageNotification.setMessageTemplate( URL );
        programStageNotification.setDeliveryChannels( Sets.newHashSet( DeliveryChannel.HTTP ) );

        programStageA.setNotificationTemplates( Sets.newHashSet( programStageNotification ) );

        responseEntity = new ResponseEntity<>( HttpStatus.OK );

    }

    @Test
    void testTrackerEnrollmentNotificationWebHook()
    {
        when( enrollmentService.getEnrollment( anyString() ) ).thenReturn( enrollment );
        when( templateService.isProgramLinkedToWebHookNotification( any( Program.class ) ) ).thenReturn( true );
        when( templateService.getProgramLinkedToWebHookNotifications( any( Program.class ) ) )
            .thenReturn( Lists.newArrayList( programNotification ) );
        when( renderService.toJsonAsString( any( Map.class ) ) ).thenReturn( "body" );
        when( restTemplate.exchange( any( URI.class ), any( HttpMethod.class ), any( HttpEntity.class ),
            eq( String.class ) ) ).thenReturn( responseEntity );

        ArgumentCaptor<URI> urlCaptor = ArgumentCaptor.forClass( URI.class );
        ArgumentCaptor<HttpMethod> httpMethodCaptor = ArgumentCaptor.forClass( HttpMethod.class );
        ArgumentCaptor<HttpEntity<?>> httpEntityCaptor = ArgumentCaptor.forClass( HttpEntity.class );
        ArgumentCaptor<Map<?, ?>> bodyCaptor = ArgumentCaptor.forClass( Map.class );

        subject.handleEnrollment( enrollment.getUid() );

        verify( renderService, times( 1 ) ).toJsonAsString( bodyCaptor.capture() );
        verify( restTemplate, times( 1 ) ).exchange( urlCaptor.capture(),
            httpMethodCaptor.capture(), httpEntityCaptor.capture(), eq( String.class ) );

        Stream.of( ProgramTemplateVariable.values() )
            .forEach( v -> assertTrue( bodyCaptor.getValue().containsKey( v.name() ) ) );

        assertTrue( bodyCaptor.getValue().containsKey( trackedEntityAttribute.getUid() ) );
        assertEquals( URL, urlCaptor.getValue().toString() );
        assertEquals( HttpMethod.POST, httpMethodCaptor.getValue() );
        assertTrue( httpEntityCaptor.getValue().getHeaders().get( "Content-Type" ).contains( "application/json" ) );
    }

    @Test
    void testTrackerEventNotificationWebHook()
    {
        when( eventService.getEvent( anyString() ) ).thenReturn( event );
        when( templateService.isProgramStageLinkedToWebHookNotification( any( ProgramStage.class ) ) )
            .thenReturn( true );
        when( templateService.getProgramStageLinkedToWebHookNotifications( any( ProgramStage.class ) ) )
            .thenReturn( Lists.newArrayList( programStageNotification ) );
        when( renderService.toJsonAsString( any( Map.class ) ) ).thenReturn( "body" );
        when( restTemplate.exchange( any( URI.class ), any( HttpMethod.class ), any( HttpEntity.class ),
            eq( String.class ) ) ).thenReturn( responseEntity );

        ArgumentCaptor<URI> urlCaptor = ArgumentCaptor.forClass( URI.class );
        ArgumentCaptor<HttpMethod> httpMethodCaptor = ArgumentCaptor.forClass( HttpMethod.class );
        ArgumentCaptor<HttpEntity<?>> httpEntityCaptor = ArgumentCaptor.forClass( HttpEntity.class );
        ArgumentCaptor<Map<?, ?>> bodyCaptor = ArgumentCaptor.forClass( Map.class );

        subject.handleEvent( event.getUid() );

        verify( renderService, times( 1 ) ).toJsonAsString( bodyCaptor.capture() );
        verify( restTemplate, times( 1 ) ).exchange( urlCaptor.capture(),
            httpMethodCaptor.capture(), httpEntityCaptor.capture(), eq( String.class ) );

        Stream.of( ProgramStageTemplateVariable.values() )
            .forEach( v -> assertTrue( bodyCaptor.getValue().containsKey( v.name() ) ) );

        assertTrue( bodyCaptor.getValue().containsKey( dataElement.getUid() ) );
        assertEquals( URL, urlCaptor.getValue().toString() );
        assertEquals( HttpMethod.POST, httpMethodCaptor.getValue() );
        assertTrue( httpEntityCaptor.getValue().getHeaders().get( "Content-Type" ).contains( "application/json" ) );
    }
}
