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
package org.hisp.dhis.webapi.controller.tracker.export;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.dxf2.events.TrackedEntityInstanceParams;
import org.hisp.dhis.dxf2.events.enrollment.Enrollment;
import org.hisp.dhis.dxf2.events.enrollment.EnrollmentService;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.webapi.controller.tracker.export.support.EnrollmentsSupportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith( MockitoExtension.class )
public class TrackerEnrollmentsExportControllerTest
{
    MockMvc mockMvc;

    @Mock
    TrackerEnrollmentCriteriaMapper enrollmentCriteriaMapper;

    @Mock
    EnrollmentService enrollmentService;

    @Mock
    FieldFilterService fieldFilterService;

    @Captor
    ArgumentCaptor<TrackedEntityInstanceParams> entityInstanceParamsArgumentCaptor;

    @InjectMocks
    EnrollmentsSupportService enrollmentsSupportService;

    String enrollment = CodeGenerator.generateUid();

    @BeforeEach
    void setUp()
    {
        mockMvc = MockMvcBuilders
            .standaloneSetup( new TrackerEnrollmentsExportController( enrollmentCriteriaMapper, enrollmentService,
                fieldFilterService, enrollmentsSupportService ) )
            .build();
    }

    @Test
    void givenEnrollmentNotFound_shouldThrowNotFound()
        throws Exception
    {
        mockMvc.perform( get( "/tracker/enrollments/{id}", enrollment ) )
            .andExpect( status().isNotFound() );
    }

    @Test
    void givenRelationshipField_shouldSearchWithIncludeRelationships()
        throws Exception
    {
        when( enrollmentService.getEnrollment( eq( enrollment ), entityInstanceParamsArgumentCaptor.capture() ) )
            .thenReturn( new Enrollment() );

        mockMvc.perform( get( "/tracker/enrollments/{id}", enrollment ).param( "fields", "relationships" ) )
            .andExpect( status().isOk() );

        assertFalse( entityInstanceParamsArgumentCaptor.getValue().isIncludeAttributes() );
        assertFalse( entityInstanceParamsArgumentCaptor.getValue().isIncludeEvents() );
        assertTrue( entityInstanceParamsArgumentCaptor.getValue().isIncludeRelationships() );
    }

    @Test
    void givenEventsField_shouldSearchWithIncludeEvents()
        throws Exception
    {
        when( enrollmentService.getEnrollment( eq( enrollment ), entityInstanceParamsArgumentCaptor.capture() ) )
            .thenReturn( new Enrollment() );

        mockMvc.perform( get( "/tracker/enrollments/{id}", enrollment ).param( "fields", "events" ) )
            .andExpect( status().isOk() );

        assertFalse( entityInstanceParamsArgumentCaptor.getValue().isIncludeAttributes() );
        assertFalse( entityInstanceParamsArgumentCaptor.getValue().isIncludeRelationships() );
        assertTrue( entityInstanceParamsArgumentCaptor.getValue().isIncludeEvents() );
    }

    @Test
    void givenAttributesField_shouldSearchWithIncludeAttributes()
        throws Exception
    {
        when( enrollmentService.getEnrollment( eq( enrollment ), entityInstanceParamsArgumentCaptor.capture() ) )
            .thenReturn( new Enrollment() );

        mockMvc.perform( get( "/tracker/enrollments/{id}", enrollment ).param( "fields", "attributes" ) )
            .andExpect( status().isOk() );

        assertTrue( entityInstanceParamsArgumentCaptor.getValue().isIncludeAttributes() );
        assertFalse( entityInstanceParamsArgumentCaptor.getValue().isIncludeRelationships() );
        assertFalse( entityInstanceParamsArgumentCaptor.getValue().isIncludeEvents() );
    }
}
