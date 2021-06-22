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
package org.hisp.dhis.tracker.validation.hooks;

import static org.mockito.Mockito.*;

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.hisp.dhis.tracker.validation.service.TrackerImportAccessManager;
import org.hisp.dhis.user.User;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.google.common.collect.Lists;

/**
 * @author Enrico Colasante
 */
public class PreCheckSecurityValidationHookTest
{
    private final static String TEI_UID = "TEIId";

    private final static String ENROLLMENT_UID = "EnrollmentId";

    private final static String EVENT_UID = "EventId";

    private final static String ORG_UNIT_UID = "OrgUnitId";

    private OrganisationUnit orgUnit;

    private TrackedEntity trackedEntity;

    private Enrollment enrollment;

    private Event event;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private TrackerBundle bundle;

    @Mock
    private TrackerImportValidationContext ctx;

    @Mock
    private TrackerImportAccessManager accessManager;

    @InjectMocks
    private PreCheckSecurityValidationHook validationHook;

    @Before
    public void setUp()
    {
        trackedEntity = TrackedEntity.builder().trackedEntity( TEI_UID ).orgUnit( ORG_UNIT_UID ).build();
        enrollment = Enrollment.builder().enrollment( ENROLLMENT_UID ).orgUnit( ORG_UNIT_UID ).build();
        event = Event.builder().event( EVENT_UID ).orgUnit( ORG_UNIT_UID ).build();
        User user = new User();
        bundle = TrackerBundle.builder()
            .trackedEntities( Lists.newArrayList( trackedEntity ) )
            .enrollments( Lists.newArrayList( enrollment ) )
            .events( Lists.newArrayList( event ) )
            .user( user )
            .build();
        when( ctx.getBundle() ).thenReturn( bundle );
        orgUnit = new OrganisationUnit();
        orgUnit.setUid( ORG_UNIT_UID );
        when( ctx.getOrganisationUnit( ORG_UNIT_UID ) ).thenReturn( orgUnit );
    }

    @Test
    public void verifyAccessManagedIsCalledWhenTrackedEntityIsValidated()
    {
        // given

        // when
        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, trackedEntity );
        validationHook.validateTrackedEntity( reporter, trackedEntity );

        // then
        verify( accessManager ).checkOrgUnitInCaptureScope( reporter, orgUnit );
    }

    @Test
    public void verifyAccessManagedIsCalledWhenEnrollemntIsValidated()
    {
        // given

        // when
        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, enrollment );
        validationHook.validateEnrollment( reporter, enrollment );

        // then
        verify( accessManager ).checkOrgUnitInCaptureScope( reporter, orgUnit );
    }

    @Test
    public void verifyAccessManagedIsCalledWhenEventIsValidated()
    {
        // given

        // when
        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, event );
        validationHook.validateEvent( reporter, event );

        // then
        verify( accessManager ).checkOrgUnitInCaptureScope( reporter, orgUnit );
    }

    @Test
    public void verifyAccessManagedIsNeverCalledWhenRelationshipIsValidated()
    {
        // given

        // when
        Relationship relationship = new Relationship();
        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, relationship );
        validationHook.validateRelationship( reporter, relationship );

        // then
        verify( accessManager, never() ).checkOrgUnitInCaptureScope( reporter, orgUnit );
    }
}