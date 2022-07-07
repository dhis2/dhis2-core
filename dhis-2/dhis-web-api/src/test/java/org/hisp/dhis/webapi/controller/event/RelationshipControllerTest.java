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
package org.hisp.dhis.webapi.controller.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hisp.dhis.dxf2.events.relationship.RelationshipService;
import org.hisp.dhis.dxf2.events.trackedentity.Relationship;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.util.NestedServletException;

/**
 * @author Enrico Colasante
 */
public class RelationshipControllerTest
{
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private MockMvc mockMvc;

    private static final String TEI_ID = "TEI_ID";

    private static final String EVENT_ID = "EVENT_ID";

    private static final String ENROLLMENT_ID = "ENROLLMENT_ID";

    private static final String REL_ID = "REL_ID";

    private TrackedEntityInstance tei = new TrackedEntityInstance();

    private ProgramInstance enrollment = new ProgramInstance();

    private ProgramStageInstance event = new ProgramStageInstance();

    private Relationship relationship = new Relationship();

    @Mock
    private RelationshipService relationshipService;

    @Mock
    private TrackedEntityInstanceService trackedEntityInstanceService;

    @Mock
    private ProgramInstanceService programInstanceService;

    @Mock
    private ProgramStageInstanceService programStageInstanceService;

    @InjectMocks
    private RelationshipController relationshipController;

    private final static String ENDPOINT = "/relationships";

    @Before
    public void setUp()
    {
        mockMvc = MockMvcBuilders.standaloneSetup( relationshipController ).build();
    }

    @Test( expected = NestedServletException.class )
    public void verifyEndpointWithNoArgs()
        throws Exception
    {
        mockMvc.perform( get( ENDPOINT ) );
    }

    @Test( expected = NestedServletException.class )
    public void verifyEndpointWithNotFoundTei()
        throws Exception
    {
        mockMvc.perform( get( ENDPOINT ).param( "tei", TEI_ID ) );
    }

    @Test
    public void verifyEndpointWithTei()
        throws Exception
    {
        when( trackedEntityInstanceService.getTrackedEntityInstance( TEI_ID ) ).thenReturn( tei );
        mockMvc.perform( get( ENDPOINT ).param( "tei", TEI_ID ) ).andExpect( status().isOk() );

        verify( trackedEntityInstanceService ).getTrackedEntityInstance( TEI_ID );
        verify( relationshipService ).getRelationshipsByTrackedEntityInstance( eq( tei ), any(), eq( false ) );
    }

    @Test( expected = NestedServletException.class )
    public void verifyEndpointWithNotFoundEvent()
        throws Exception
    {
        mockMvc.perform( get( ENDPOINT ).param( "event", EVENT_ID ) );
    }

    @Test
    public void verifyEndpointWithEvent()
        throws Exception
    {
        when( programStageInstanceService.getProgramStageInstance( EVENT_ID ) ).thenReturn( event );
        mockMvc.perform( get( ENDPOINT ).param( "event", EVENT_ID ) ).andExpect( status().isOk() );

        verify( programStageInstanceService ).getProgramStageInstance( EVENT_ID );
        verify( relationshipService ).getRelationshipsByProgramStageInstance( eq( event ), any(), eq( false ) );
    }

    @Test( expected = NestedServletException.class )
    public void verifyEndpointWithNotFoundEnrollment()
        throws Exception
    {
        mockMvc.perform( get( ENDPOINT ).param( "enrollment", ENROLLMENT_ID ) ).andExpect( status().isBadRequest() );
    }

    @Test
    public void verifyEndpointWithEnrollment()
        throws Exception
    {
        when( programInstanceService.getProgramInstance( ENROLLMENT_ID ) ).thenReturn( enrollment );
        mockMvc.perform( get( ENDPOINT ).param( "enrollment", ENROLLMENT_ID ) ).andExpect( status().isOk() );

        verify( programInstanceService ).getProgramInstance( ENROLLMENT_ID );
        verify( relationshipService ).getRelationshipsByProgramInstance( eq( enrollment ), any(), eq( false ) );
    }

    @Test( expected = NestedServletException.class )
    public void testGetRelationshipNotPresent()
        throws Exception
    {
        mockMvc.perform( get( ENDPOINT + "/" + REL_ID ) );
    }

    @Test
    public void testGetRelationship()
        throws Exception
    {
        when( relationshipService.getRelationshipByUid( REL_ID ) ).thenReturn( relationship );
        mockMvc.perform( get( ENDPOINT + "/" + REL_ID ) ).andExpect( status().isOk() );
    }

    @Test
    public void testDeleteRelationship()
        throws Exception
    {
        when( relationshipService.getRelationshipByUid( REL_ID ) ).thenReturn( relationship );
        mockMvc.perform( get( ENDPOINT + "/" + REL_ID ) ).andExpect( status().isOk() );
    }
}
