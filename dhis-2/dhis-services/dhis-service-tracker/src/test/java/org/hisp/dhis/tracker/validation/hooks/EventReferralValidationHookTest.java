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
import java.util.List;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.relationship.RelationshipType;
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

/**
 * @author rajazubair
 */

@ExtendWith( MockitoExtension.class )
class EventReferralValidationHookTest extends DhisConvenienceTest
{
    public static final String TEI_UID = CodeGenerator.generateUid();

    public static final String PROGRAM_UID = CodeGenerator.generateUid();

    public static final String PROGRAM_STAGE_UID = CodeGenerator.generateUid();

    public static final String PROGRAM_STAGE_REFERRAL_UID = CodeGenerator.generateUid();

    private Program program;

    private ProgramStage programStage;

    private ProgramStage programStageReferral;

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
    void verifyValidationSuccessForNonReferralEvent()
    {
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( PROGRAM_STAGE_UID ) ) )
            .thenReturn( programStage );

        subject.validateEvent( reporter, bundle, createEvent( PROGRAM_STAGE_UID, Arrays.asList( relationship ) ) );

        assertEquals( 0, reporter.getReportList().size() );
    }

    @Test
    void verifyValidationSuccessIfReferralHasCompleteRelationShip()
    {
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( PROGRAM_STAGE_REFERRAL_UID ) ) )
            .thenReturn( programStageReferral );

        subject.validateEvent( reporter, bundle,
            createEvent( PROGRAM_STAGE_REFERRAL_UID, Arrays.asList( relationship ) ) );

        assertEquals( 0, reporter.getReportList().size() );
    }

    @Test
    void verifyValidationFailIfReferralHasInCompleteRelationShip()
    {
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( PROGRAM_STAGE_REFERRAL_UID ) ) )
            .thenReturn( programStageReferral );

        subject.validateEvent( reporter, bundle,
            createEvent( PROGRAM_STAGE_REFERRAL_UID, Arrays.asList( relationshipIncomplete ) ) );

        assertAll( () -> assertEquals( 1, reporter.getReportList().size() ),
            () -> assertTrue( reporter.hasErrorReport( r -> r.getErrorCode() == TrackerErrorCode.E1312 ) ) );
    }

    @Test
    void verifyValidationFailIfReferralHasNoRelationShip()
    {
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( PROGRAM_STAGE_REFERRAL_UID ) ) )
            .thenReturn( programStageReferral );

        subject.validateEvent( reporter, bundle, createEvent( PROGRAM_STAGE_REFERRAL_UID, Arrays.asList() ) );

        assertAll( () -> assertEquals( 1, reporter.getReportList().size() ),
            () -> assertTrue( reporter.hasErrorReport( r -> r.getErrorCode() == TrackerErrorCode.E1311 ) ) );
    }

    private Event createEvent( String psiUid, List<Relationship> relationships )
    {
        return Event.builder()
            .event( CodeGenerator.generateUid() )
            .program( MetadataIdentifier.ofUid( program ) )
            .programStage( MetadataIdentifier.ofUid( psiUid ) )
            .relationships( relationships )
            .build();
    }

    private void init()
    {
        program = createProgram( 'P' );
        program.setProgramType( ProgramType.WITH_REGISTRATION );
        program.setUid( PROGRAM_UID );

        programStage = createProgramStage( 'S', program );
        programStage.setUid( PROGRAM_STAGE_UID );
        programStage.setProgram( program );

        programStageReferral = createProgramStage( 'S', program );
        programStageReferral.setUid( PROGRAM_STAGE_REFERRAL_UID );
        programStageReferral.setProgram( program );
        programStageReferral.setReferral( true );

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
