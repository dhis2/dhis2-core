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

import static org.hisp.dhis.organisationunit.FeatureType.MULTI_POLYGON;
import static org.hisp.dhis.organisationunit.FeatureType.NONE;
import static org.hisp.dhis.tracker.TrackerType.EVENT;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1012;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1074;
import static org.hisp.dhis.tracker.validation.hooks.AssertValidationErrorReporter.hasTrackerError;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.GeometryFactory;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Enrico Colasante
 */
@MockitoSettings( strictness = Strictness.LENIENT )
@ExtendWith( MockitoExtension.class )
class EventGeoValidationHookTest
{

    private static final String PROGRAM_STAGE = "ProgramStage";

    private EventGeoValidationHook hookToTest;

    @Mock
    private TrackerPreheat preheat;

    private TrackerBundle bundle;

    private ValidationErrorReporter reporter;

    @BeforeEach
    public void setUp()
    {
        hookToTest = new EventGeoValidationHook();

        bundle = TrackerBundle.builder()
            .preheat( preheat )
            .build();

        ProgramStage programStage = new ProgramStage();
        programStage.setFeatureType( FeatureType.POINT );
        when( preheat.getProgramStage( MetadataIdentifier.ofUid( PROGRAM_STAGE ) ) )
            .thenReturn( programStage );

        TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
        reporter = new ValidationErrorReporter( idSchemes );
    }

    @Test
    void testGeometryIsValid()
    {
        // given
        Event event = new Event();
        event.setProgramStage( MetadataIdentifier.ofUid( PROGRAM_STAGE ) );
        event.setGeometry( new GeometryFactory().createPoint() );

        // when
        this.hookToTest.validateEvent( reporter, bundle, event );

        // then
        assertFalse( reporter.hasErrors() );
    }

    @Test
    void testEventWithNoProgramStageThrowsAnError()
    {
        // given
        Event event = new Event();
        event.setProgramStage( null );
        event.setGeometry( new GeometryFactory().createPoint() );

        when( preheat.getProgramStage( (MetadataIdentifier) null ) ).thenReturn( null );

        // when
        assertThrows( NullPointerException.class, () -> this.hookToTest.validateEvent( reporter, bundle, event ) );
    }

    @Test
    void testProgramStageWithNullFeatureTypeFailsGeometryValidation()
    {
        // given
        Event event = new Event();
        event.setEvent( CodeGenerator.generateUid() );
        event.setProgramStage( MetadataIdentifier.ofUid( PROGRAM_STAGE ) );
        event.setGeometry( new GeometryFactory().createPoint() );

        // when
        when( preheat.getProgramStage( event.getProgramStage() ) ).thenReturn( new ProgramStage() );

        this.hookToTest.validateEvent( reporter, bundle, event );

        // then
        hasTrackerError( reporter, E1074, EVENT, event.getUid() );
    }

    @Test
    void testProgramStageWithFeatureTypeNoneFailsGeometryValidation()
    {
        // given
        Event event = new Event();
        event.setEvent( CodeGenerator.generateUid() );
        event.setProgramStage( MetadataIdentifier.ofUid( PROGRAM_STAGE ) );
        event.setGeometry( new GeometryFactory().createPoint() );

        // when
        ProgramStage programStage = new ProgramStage();
        programStage.setFeatureType( NONE );
        when( preheat.getProgramStage( event.getProgramStage() ) ).thenReturn( programStage );

        this.hookToTest.validateEvent( reporter, bundle, event );

        // then
        hasTrackerError( reporter, E1012, EVENT, event.getUid() );
    }

    @Test
    void testProgramStageWithFeatureTypeDifferentFromGeometryFails()
    {
        // given
        Event event = new Event();
        event.setEvent( CodeGenerator.generateUid() );
        event.setProgramStage( MetadataIdentifier.ofUid( PROGRAM_STAGE ) );
        event.setGeometry( new GeometryFactory().createPoint() );

        // when
        ProgramStage programStage = new ProgramStage();
        programStage.setFeatureType( MULTI_POLYGON );
        when( preheat.getProgramStage( event.getProgramStage() ) ).thenReturn( programStage );

        this.hookToTest.validateEvent( reporter, bundle, event );

        // then
        hasTrackerError( reporter, E1012, EVENT, event.getUid() );
    }
}
