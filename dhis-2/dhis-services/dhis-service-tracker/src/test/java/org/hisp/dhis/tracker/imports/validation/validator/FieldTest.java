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
package org.hisp.dhis.tracker.imports.validation.validator;

import static org.hisp.dhis.tracker.imports.TrackerImportStrategy.CREATE_AND_UPDATE;
import static org.hisp.dhis.tracker.imports.TrackerImportStrategy.UPDATE;
import static org.hisp.dhis.tracker.imports.TrackerType.ENROLLMENT;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1000;
import static org.hisp.dhis.tracker.imports.validation.validator.AssertValidations.assertHasError;
import static org.hisp.dhis.tracker.imports.validation.validator.Field.field;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hisp.dhis.tracker.imports.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.TrackerType;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.domain.TrackerDto;
import org.hisp.dhis.tracker.imports.validation.Error;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.tracker.imports.validation.ValidationCode;
import org.hisp.dhis.tracker.imports.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FieldTest
{

    private Reporter reporter;

    private TrackerBundle bundle;

    @BeforeEach
    void setUp()
    {
        TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder()
            .build();
        reporter = new Reporter( idSchemes );
        bundle = TrackerBundle.builder().build();
    }

    @Test
    void testFieldWithValidator()
    {
        Enrollment enrollment = Enrollment.builder()
            .enrollment( "Kj6vYde4LHh" )
            .trackedEntity( "PuBvJxDB73z" )
            .build();

        Validator<String> isValidUid = ( r, b, uid ) -> {
            // to demonstrate that we are getting the trackedEntity field
            r.addError( new Error( uid, E1000, ENROLLMENT, uid ) );
        };

        Validator<Enrollment> validator = field( Enrollment::getTrackedEntity, isValidUid );

        validator.validate( reporter, bundle, enrollment );

        assertContainsOnly( List.of( "PuBvJxDB73z" ), actualErrorMessages() );
    }

    @Test
    void testFieldWithValidatorDoesNotCallValidatorIfItShouldNotRunOnGivenStrategy()
    {
        bundle = TrackerBundle.builder().importStrategy( UPDATE ).build();
        Enrollment enrollment = Enrollment.builder()
            .enrollment( "Kj6vYde4LHh" )
            .trackedEntity( "PuBvJxDB73z" )
            .build();

        Validator<String> isValidUid = new Validator<>()
        {
            @Override
            public void validate( Reporter reporter, TrackerBundle bundle, String input )
            {
                addError( reporter, "V1" );
            }

            @Override
            public boolean needsToRun( TrackerImportStrategy strategy )
            {
                return strategy == TrackerImportStrategy.DELETE;
            }
        };

        Validator<Enrollment> validator = field( Enrollment::getTrackedEntity, isValidUid );

        validator.validate( reporter, bundle, enrollment );

        assertIsEmpty( reporter.getErrors() );
    }

    @Test
    void testFieldWithValidatorDoesNotCallValidatorIfItShouldNotRunOnGivenStrategyForATrackerDto()
    {
        bundle = TrackerBundle.builder()
            .importStrategy( CREATE_AND_UPDATE )
            .resolvedStrategyMap( new EnumMap<>( Map.of(
                ENROLLMENT, Map.of( "Kj6vYde4LHh", UPDATE ) ) ) )
            .build();
        Enrollment enrollment = Enrollment.builder()
            .enrollment( "Kj6vYde4LHh" )
            .trackedEntity( "PuBvJxDB73z" )
            .build();

        Validator<String> isValidUid = new Validator<>()
        {
            @Override
            public void validate( Reporter reporter, TrackerBundle bundle, String input )
            {
                addError( reporter, "V1" );
            }

            @Override
            public boolean needsToRun( TrackerImportStrategy strategy )
            {
                return strategy == TrackerImportStrategy.CREATE;
            }
        };

        Validator<Enrollment> validator = field( Enrollment::getTrackedEntity, isValidUid );

        validator.validate( reporter, bundle, enrollment );

        assertIsEmpty( reporter.getErrors() );
    }

    @Test
    void testFieldWithPredicateFailing()
    {
        Enrollment enrollment = Enrollment.builder()
            .enrollment( "Kj6vYde4LHh" )
            .trackedEntity( "PuBvJxDB73z" )
            .build();

        Validator<Enrollment> validator = field( Enrollment::getTrackedEntity, e -> false, E1000,
            "wrong 1", "wrong 2" );

        validator.validate( reporter, bundle, enrollment );

        assertHasError( reporter, enrollment, E1000, "wrong" );
    }

    @Test
    void testFieldWithPredicateSucceeding()
    {
        Enrollment enrollment = Enrollment.builder()
            .enrollment( "Kj6vYde4LHh" )
            .trackedEntity( "PuBvJxDB73z" )
            .build();

        Validator<Enrollment> validator = field( Enrollment::getTrackedEntity, e -> true, E1000,
            "wrong 1", "wrong 2" );

        validator.validate( reporter, bundle, enrollment );

        assertIsEmpty( reporter.getErrors() );
    }

    /**
     * Add error with given message to {@link Reporter}. Every {@link Error} is
     * attributed to a {@link TrackerDto}, which makes adding errors cumbersome
     * when you do not care about any particular tracker type, uid or error
     * code.
     */
    private static void addError( Reporter reporter, String message )
    {
        reporter.addError( new Error( message, ValidationCode.E9999, TrackerType.TRACKED_ENTITY, "uid" ) );
    }

    private List<String> actualErrorMessages()
    {
        return reporter.getErrors().stream().map( Error::getMessage ).collect( Collectors.toList() );
    }
}
