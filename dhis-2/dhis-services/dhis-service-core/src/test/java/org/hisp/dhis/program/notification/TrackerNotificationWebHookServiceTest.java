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
package org.hisp.dhis.program.notification;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
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

public class TrackerNotificationWebHookServiceTest extends DhisConvenienceTest
{
    private static final String URL = "https://www.google.com";

    private OrganisationUnit organisationUnitA;

    private Program programA;

    private ProgramStage programStageA;

    private ProgramInstance programInstance;

    private ProgramNotificationTemplate pnt;

    private ResponseEntity<String> responseEntity;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private ProgramInstanceService programInstanceService;

    @Mock
    private ProgramStageInstanceService programStageInstanceService;

    @Mock
    private ProgramNotificationTemplateService templateService;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RenderService renderService;

    @InjectMocks
    private DefaultTrackerNotificationWebHookService subject;

    @Before
    public void initTest()
    {
        organisationUnitA = createOrganisationUnit( 'A' );
        programA = createProgram( 'A', new HashSet<>(), organisationUnitA );
        programStageA = createProgramStage( 'A', programA );
        TrackedEntityInstance tei = createTrackedEntityInstance( organisationUnitA );

        programInstance = new ProgramInstance();
        programInstance.setAutoFields();
        programInstance.setProgram( programA );
        programInstance.setOrganisationUnit( organisationUnitA );
        programInstance.setEnrollmentDate( new Date() );
        programInstance.setIncidentDate( new Date() );
        programInstance.setEntityInstance( tei );

        pnt = new ProgramNotificationTemplate();
        pnt.setNotificationRecipient( ProgramNotificationRecipient.WEB_HOOK );
        pnt.setMessageTemplate( URL );
        pnt.setDeliveryChannels( Sets.newHashSet( DeliveryChannel.HTTP ) );

        programA.setNotificationTemplates( Sets.newHashSet( pnt ) );

        responseEntity = new ResponseEntity<String>( HttpStatus.OK );

    }

    @Test
    public void testTrackerEnrollmentNotificationWebHook()
    {
        when( programInstanceService.getProgramInstance( anyString() ) ).thenReturn( programInstance );
        when( templateService.isProgramLinkedToWebHookNotification( any( Program.class ) ) ).thenReturn( true );
        when( templateService.getProgramLinkedToWebHookNotifications( any( Program.class ) ) )
            .thenReturn( Lists.newArrayList( pnt ) );
        when( renderService.toJsonAsString( any( Map.class ) ) ).thenReturn( "body" );
        when( restTemplate.exchange( any( URI.class ), any( HttpMethod.class ), any( HttpEntity.class ),
            eq( String.class ) ) ).thenReturn( responseEntity );

        ArgumentCaptor<URI> urlCaptor = ArgumentCaptor.forClass( URI.class );
        ArgumentCaptor<HttpMethod> httpMethodCaptor = ArgumentCaptor.forClass( HttpMethod.class );
        ArgumentCaptor<HttpEntity> httpEntityCaptor = ArgumentCaptor.forClass( HttpEntity.class );
        ArgumentCaptor<Map> bodyCaptor = ArgumentCaptor.forClass( Map.class );

        subject.handleEnrollment( programInstance.getUid() );

        verify( renderService, times( 1 ) ).toJsonAsString( bodyCaptor.capture() );
        verify( restTemplate, times( 1 ) ).exchange( urlCaptor.capture(),
            httpMethodCaptor.capture(), httpEntityCaptor.capture(), eq( String.class ) );

        assertTrue( bodyCaptor.getValue().containsKey( ProgramTemplateVariable.PROGRAM_NAME.name() ) );
        assertTrue( bodyCaptor.getValue().containsKey( ProgramTemplateVariable.ORG_UNIT_NAME.name() ) );
        assertTrue( bodyCaptor.getValue().containsKey( ProgramTemplateVariable.ENROLLMENT_DATE.name() ) );
        assertTrue( bodyCaptor.getValue().containsKey( ProgramTemplateVariable.PROGRAM_ID.name() ) );
        assertTrue( bodyCaptor.getValue().containsKey( ProgramTemplateVariable.TRACKED_ENTITY_ID.name() ) );
        assertEquals( URL, urlCaptor.getValue().toString() );
        assertEquals( HttpMethod.POST, httpMethodCaptor.getValue() );
        assertTrue( httpEntityCaptor.getValue().getHeaders().get( "Content-Type" ).contains( "application/json" ) );
    }
}
