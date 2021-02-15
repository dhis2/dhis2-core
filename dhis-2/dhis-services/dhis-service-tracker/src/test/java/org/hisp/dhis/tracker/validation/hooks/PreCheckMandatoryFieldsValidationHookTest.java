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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.ValidationMode;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.*;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/**
 * @author Enrico Colasante
 */
public class PreCheckMandatoryFieldsValidationHookTest
{
    private PreCheckMandatoryFieldsValidationHook validationHook;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private TrackerBundle bundle;

    @Mock
    private TrackerImportValidationContext ctx;

    @Mock
    private TrackerPreheat preheat;

    @Before
    public void setUp()
    {
        validationHook = new PreCheckMandatoryFieldsValidationHook();

        when( ctx.getBundle() ).thenReturn( bundle );
        when( ctx.getBundle().getImportStrategy() ).thenReturn( TrackerImportStrategy.CREATE_AND_UPDATE );
        when( bundle.getValidationMode() ).thenReturn( ValidationMode.FULL );
        when( bundle.getPreheat() ).thenReturn( preheat );
    }

    @Test
    public void verifyTrackedEntityValidationSuccess()
    {
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntityType( CodeGenerator.generateUid() )
            .orgUnit( CodeGenerator.generateUid() )
            .build();

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, trackedEntity );
        validationHook.validateTrackedEntity( reporter, trackedEntity );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    public void verifyTrackedEntityValidationFailsOnMissingOrgUnit()
    {
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntityType( CodeGenerator.generateUid() )
            .orgUnit( null )
            .build();

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, trackedEntity );
        validationHook.validateTrackedEntity( reporter, trackedEntity );

        assertMissingPropertyForTrackedEntity( reporter, "orgUnit" );
    }

    @Test
    public void verifyTrackedEntityValidationFailsOnMissingTrackedEntityType()
    {
        TrackedEntity trackedEntity = TrackedEntity.builder()
            .trackedEntityType( null )
            .orgUnit( CodeGenerator.generateUid() )
            .build();

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, trackedEntity );
        validationHook.validateTrackedEntity( reporter, trackedEntity );

        assertMissingPropertyForTrackedEntity( reporter, "trackedEntityType" );
    }

    @Test
    public void verifyEnrollmentValidationSuccess()
    {
        Enrollment enrollment = Enrollment.builder()
            .orgUnit( CodeGenerator.generateUid() )
            .program( CodeGenerator.generateUid() )
            .trackedEntity( CodeGenerator.generateUid() )
            .build();

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, enrollment );
        validationHook.validateEnrollment( reporter, enrollment );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    public void verifyEnrollmentValidationFailsOnMissingTrackedEntity()
    {
        Enrollment enrollment = Enrollment.builder()
            .orgUnit( CodeGenerator.generateUid() )
            .program( CodeGenerator.generateUid() )
            .trackedEntity( null )
            .build();

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, enrollment );
        validationHook.validateEnrollment( reporter, enrollment );

        assertMissingPropertyForEnrollment( reporter, "trackedEntity" );
    }

    @Test
    public void verifyEnrollmentValidationFailsOnMissingProgram()
    {
        Enrollment enrollment = Enrollment.builder()
            .orgUnit( CodeGenerator.generateUid() )
            .program( null )
            .trackedEntity( CodeGenerator.generateUid() )
            .build();

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, enrollment );
        validationHook.validateEnrollment( reporter, enrollment );

        assertMissingPropertyForEnrollment( reporter, "program" );
    }

    @Test
    public void verifyEnrollmentValidationFailsOnMissingOrgUnit()
    {
        Enrollment enrollment = Enrollment.builder()
            .orgUnit( null )
            .program( CodeGenerator.generateUid() )
            .trackedEntity( CodeGenerator.generateUid() )
            .build();

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, enrollment );
        validationHook.validateEnrollment( reporter, enrollment );

        assertMissingPropertyForEnrollment( reporter, "orgUnit" );
    }

    @Test
    public void verifyEventValidationSuccess()
    {
        Event event = Event.builder()
            .orgUnit( CodeGenerator.generateUid() )
            .programStage( CodeGenerator.generateUid() )
            .build();

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, event );
        validationHook.validateEvent( reporter, event );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    public void verifyEventValidationFailsOnMissingOrgUnit()
    {
        Event event = Event.builder()
            .orgUnit( null )
            .programStage( CodeGenerator.generateUid() )
            .build();

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, event );
        validationHook.validateEvent( reporter, event );

        assertMissingPropertyForEvent( reporter, "orgUnit" );
    }

    @Test
    public void verifyEventValidationFailsOnMissingProgramStage()
    {
        Event event = Event.builder()
            .orgUnit( CodeGenerator.generateUid() )
            .programStage( null )
            .build();

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, event );
        validationHook.validateEvent( reporter, event );

        assertMissingPropertyForEvent( reporter, "programStage" );
    }

    @Test
    public void verifyRelationshipValidationSuccess()
    {
        Relationship relationship = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .relationshipType( CodeGenerator.generateUid() )
            .from( RelationshipItem.builder()
                .trackedEntity( CodeGenerator.generateUid() )
                .build() )
            .to( RelationshipItem.builder()
                .trackedEntity( CodeGenerator.generateUid() )
                .build() )
            .build();

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, relationship );
        validationHook.validateRelationship( reporter, relationship );

        assertFalse( reporter.hasErrors() );
    }

    @Test
    public void verifyRelationshipValidationFailsOnMissingFrom()
    {
        Relationship relationship = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .relationshipType( CodeGenerator.generateUid() )
            .to( RelationshipItem.builder()
                .trackedEntity( CodeGenerator.generateUid() )
                .build() )
            .build();

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, relationship );
        validationHook.validateRelationship( reporter, relationship );

        assertMissingPropertyForRelationship( reporter, "from" );
    }

    @Test
    public void verifyRelationshipValidationFailsOnMissingTo()
    {
        Relationship relationship = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .relationshipType( CodeGenerator.generateUid() )
            .from( RelationshipItem.builder()
                .trackedEntity( CodeGenerator.generateUid() )
                .build() )
            .build();

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, relationship );
        validationHook.validateRelationship( reporter, relationship );

        assertMissingPropertyForRelationship( reporter, "to" );
    }

    @Test
    public void verifyRelationshipValidationFailsOnMissingRelationshipType()
    {
        Relationship relationship = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .from( RelationshipItem.builder()
                .trackedEntity( CodeGenerator.generateUid() )
                .build() )
            .to( RelationshipItem.builder()
                .trackedEntity( CodeGenerator.generateUid() )
                .build() )
            .build();

        ValidationErrorReporter reporter = new ValidationErrorReporter( ctx, relationship );
        validationHook.validateRelationship( reporter, relationship );

        assertMissingPropertyForRelationship( reporter, "relationshipType" );
    }

    private void assertMissingPropertyForTrackedEntity( ValidationErrorReporter reporter, String property )
    {
        assertMissingProperty( reporter, "tracked entity", property, TrackerErrorCode.E1121 );
    }

    private void assertMissingPropertyForEnrollment( ValidationErrorReporter reporter, String property )
    {
        assertMissingProperty( reporter, "enrollment", property, TrackerErrorCode.E1122 );
    }

    private void assertMissingPropertyForEvent( ValidationErrorReporter reporter, String property )
    {
        assertMissingProperty( reporter, "event", property, TrackerErrorCode.E1123 );
    }

    private void assertMissingPropertyForRelationship( ValidationErrorReporter reporter, String property )
    {
        assertMissingProperty( reporter, "relationship", property, TrackerErrorCode.E1124 );
    }

    private void assertMissingProperty( ValidationErrorReporter reporter, String entity, String property,
        TrackerErrorCode errorCode )
    {
        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList(), hasSize( 1 ) );
        assertThat( reporter.getReportList().get( 0 ).getErrorCode(), is( errorCode ) );
        assertThat( reporter.getReportList().get( 0 ).getErrorMessage(),
            is( "Missing required " + entity + " property: `" + property + "`." ) );
    }
}