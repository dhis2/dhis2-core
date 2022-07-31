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
package org.hisp.dhis.tracker.validation.hooks;

import static org.hisp.dhis.relationship.RelationshipEntity.TRACKED_ENTITY_INSTANCE;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.RelationshipItem;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Sets;

/**
 * @author rajazubair
 */

@ExtendWith( MockitoExtension.class )
public class EventReferralValidationHookTest extends DhisConvenienceTest
{
    public static final String ORG_UNIT_UID = CodeGenerator.generateUid();

    public static final String TEI_TYPE_UID = CodeGenerator.generateUid();

    public static final String TEI_UID = CodeGenerator.generateUid();

    public static final String PROGRAM_UID = CodeGenerator.generateUid();

    public static final String PROGRAM_STAGE_UID = CodeGenerator.generateUid();

    public static final String PROGRAM_STAGE_REFERRAL_UID = CodeGenerator.generateUid();

    public static final String ENROLLMENT_UID = CodeGenerator.generateUid();

    private OrganisationUnit orgUnit;

    private TrackedEntityType teiType;

    private Program program;

    private ProgramStage programStage;

    private ProgramStage programStageReferral;

    private TrackedEntityInstance tei;

    private ProgramInstance programInstance;

    private Relationship relationship;

    private Relationship relationshipIncomplete;

    private EventReferralValidationHook subject;

    private TrackerBundle bundle;

    @Mock
    private TrackerPreheat preheat;

    private ValidationErrorReporter reporter;

    @BeforeEach
    void setUp()
    {
        subject = new EventReferralValidationHook();

        bundle = TrackerBundle.builder().preheat( preheat ).build();
        TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
        reporter = new ValidationErrorReporter( idSchemes );

        init();
    }

    @Test
    public void verifyValidationSuccessForNonReferralEvent()
    {
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( PROGRAM_STAGE_UID ) ) )
            .thenReturn( programStage );

        Event event = Event.builder()
            .event( CodeGenerator.generateUid() )
            .program( MetadataIdentifier.ofUid( program ) )
            .programStage( MetadataIdentifier.ofUid( PROGRAM_STAGE_UID ) )
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_UID ) )
            .attributeOptionCombo( MetadataIdentifier.EMPTY_UID )
            .enrollment( ENROLLMENT_UID )
            .relationships( Arrays.asList( relationship ) )
            .build();

        subject.validateEvent( reporter, bundle, event );

        assertEquals( 0, reporter.getReportList().size() );
    }

    @Test
    public void verifyValidationSuccessIfReferralHasCompleteRelationShip()
    {
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( PROGRAM_STAGE_REFERRAL_UID ) ) )
            .thenReturn( programStageReferral );

        Event event = Event.builder()
            .event( CodeGenerator.generateUid() )
            .program( MetadataIdentifier.ofUid( program ) )
            .programStage( MetadataIdentifier.ofUid( PROGRAM_STAGE_REFERRAL_UID ) )
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_UID ) )
            .attributeOptionCombo( MetadataIdentifier.EMPTY_UID )
            .enrollment( ENROLLMENT_UID )
            .relationships( Arrays.asList( relationship ) )
            .build();

        subject.validateEvent( reporter, bundle, event );

        assertEquals( 0, reporter.getReportList().size() );
    }

    @Test
    public void verifyValidationFailIfReferralHasInCompleteRelationShip()
    {
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( PROGRAM_STAGE_REFERRAL_UID ) ) )
            .thenReturn( programStageReferral );

        Event event = Event.builder()
            .event( CodeGenerator.generateUid() )
            .program( MetadataIdentifier.ofUid( program ) )
            .programStage( MetadataIdentifier.ofUid( PROGRAM_STAGE_REFERRAL_UID ) )
            .orgUnit( MetadataIdentifier.ofUid( ORG_UNIT_UID ) )
            .attributeOptionCombo( MetadataIdentifier.EMPTY_UID )
            .enrollment( ENROLLMENT_UID )
            .relationships( Arrays.asList( relationshipIncomplete ) )
            .build();

        subject.validateEvent( reporter, bundle, event );

        assertAll( () -> assertEquals( 1, reporter.getReportList().size() ),
            () -> assertTrue( reporter.hasErrorReport( r -> r.getErrorCode() == TrackerErrorCode.E1312 ) ) );
    }

    @Test
    public void verifyValidationFailIfReferralHasNoRelationShip()
    {
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( PROGRAM_STAGE_REFERRAL_UID ) ) )
            .thenReturn( programStageReferral );

        Event event = Event.builder()
            .programStage( MetadataIdentifier.ofUid( PROGRAM_STAGE_REFERRAL_UID ) )
            .build();

        subject.validateEvent( reporter, bundle, event );

        assertAll( () -> assertEquals( 1, reporter.getReportList().size() ),
            () -> assertTrue( reporter.hasErrorReport( r -> r.getErrorCode() == TrackerErrorCode.E1311 ) ) );
    }

    private void init()
    {
        orgUnit = createOrganisationUnit( "A" );
        orgUnit.setUid( ORG_UNIT_UID );

        teiType = createTrackedEntityType( 'T' );
        teiType.setUid( TEI_TYPE_UID );

        program = createProgram( 'P' );
        program.setProgramType( ProgramType.WITH_REGISTRATION );
        program.setTrackedEntityType( teiType );
        program.setUid( PROGRAM_UID );
        program.setOrganisationUnits( Sets.newHashSet( orgUnit ) );

        programStage = createProgramStage( 'S', program );
        programStage.setUid( PROGRAM_STAGE_UID );
        programStage.setProgram( program );

        programStageReferral = createProgramStage( 'S', program );
        programStageReferral.setUid( PROGRAM_STAGE_REFERRAL_UID );
        programStageReferral.setProgram( program );
        programStageReferral.setReferral( true );

        tei = createTrackedEntityInstance( orgUnit );
        tei.setUid( TEI_UID );
        tei.setTrackedEntityType( teiType );

        programInstance = createProgramInstance( program, tei, orgUnit );
        programInstance.setUid( PROGRAM_STAGE_UID );

        RelationshipType relType = createRelTypeConstraint( TRACKED_ENTITY_INSTANCE, TRACKED_ENTITY_INSTANCE );

        relationship = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .from( RelationshipItem.builder().trackedEntity( TEI_UID ).build() )
            .to( RelationshipItem.builder().trackedEntity( TEI_UID ).build() )
            .relationshipType( MetadataIdentifier.ofUid( relType.getUid() ) )
            .build();

        relationshipIncomplete = Relationship.builder()
            .relationship( CodeGenerator.generateUid() )
            .from( RelationshipItem.builder().trackedEntity( TEI_UID ).build() )
            .relationshipType( MetadataIdentifier.ofUid( relType.getUid() ) )
            .build();
    }
}
