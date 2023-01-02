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
package org.hisp.dhis.tracker.validation.validator;

import static org.hisp.dhis.tracker.TrackerImportStrategy.CREATE;
import static org.hisp.dhis.tracker.TrackerImportStrategy.CREATE_AND_UPDATE;
import static org.hisp.dhis.tracker.TrackerImportStrategy.DELETE;
import static org.hisp.dhis.tracker.TrackerImportStrategy.UPDATE;
import static org.hisp.dhis.tracker.validation.validator.All.all;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.TrackerDto;
import org.hisp.dhis.tracker.validation.Error;
import org.hisp.dhis.tracker.validation.Reporter;
import org.hisp.dhis.tracker.validation.ValidationCode;
import org.hisp.dhis.tracker.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AllTest
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
    void testAllAreCalled()
    {
        // @formatter:off
        Validator<String> validator = all(
            ( r, b, s ) -> addError( r, "V1" ),
            ( r, b, s ) -> addError( r, "V2" ),
            all(
                ( r, b, s ) -> addError( r, "V3" ),
                ( r, b, s ) -> addError( r, "V4" ),
                all(
                    ( r, b, s ) -> addError( r, "V5" )
                )
            )
        );

        validator.validate( reporter, bundle, "irrelevant input" );

        assertEquals( List.of( "V1", "V2", "V3", "V4", "V5" ), actualErrorMessages() );
    }

    @Test
    void testAllDoesNotCallValidatorIfItShouldNotRunOnGivenStrategy()
    {
        bundle = TrackerBundle.builder().importStrategy(UPDATE).build();

        // @formatter:off
        Validator<String> validator = all(
                (r, b, s) -> addError(r, "V1"),
                new Validator<>() {
                    @Override
                    public void validate(Reporter reporter, TrackerBundle bundle, String input) {
                        addError(reporter, "V2");
                    }

                    @Override
                    public boolean needsToRun(TrackerImportStrategy strategy) {
                        return strategy == DELETE;
                    }
                }
        );

        validator.validate( reporter, bundle, "irrelevant input" );

        assertContainsOnly( List.of( "V1"), actualErrorMessages() );
    }

    @Test
    void testAllDoesNotCallValidatorIfItShouldNotRunOnGivenStrategyForATrackerDto()
    {
        bundle = TrackerBundle.builder()
                .importStrategy(CREATE_AND_UPDATE)
                .resolvedStrategyMap(new EnumMap<>(Map.of(
                        TrackerType.EVENT, Map.of("event1", UPDATE)
                )))
                .build();

        // @formatter:off
        Validator<Event> validator = all(
                new Validator<>() {
                    @Override
                    public void validate(Reporter reporter, TrackerBundle bundle, Event input) {
                        addError(reporter, "V1");
                    }

                    @Override
                    public boolean needsToRun(TrackerImportStrategy strategy) {
                        return strategy == CREATE;
                    }
                },
                new Validator<>() {
                    @Override
                    public void validate(Reporter reporter, TrackerBundle bundle, Event input) {
                        addError(reporter, "V2");
                    }

                    @Override
                    public boolean needsToRun(TrackerImportStrategy strategy) {
                        return strategy == UPDATE;
                    }
                }
        );

        validator.validate( reporter, bundle, Event.builder().event("event1").build());

        assertContainsOnly( List.of( "V2"), actualErrorMessages() );
    }

    /**
     * Add error with given message to {@link Reporter}. Every {@link Error} is attributed to a
     * {@link TrackerDto}, which makes adding errors cumbersome when you do not care about any particular tracker type, uid or error code.
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