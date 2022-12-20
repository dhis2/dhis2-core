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
import static org.hisp.dhis.tracker.validation.ValidationCode.E1011;
import static org.hisp.dhis.tracker.validation.ValidationCode.E1012;
import static org.hisp.dhis.tracker.validation.validator.Seq.seq;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

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

class SeqTest
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
    void testSeqCallsUntilFirstError()
    {
        // @formatter:off
        Validator<String> validator = seq(
                new MatchingValidator( "V1", "one" ), // no error so moving on to the next validator
                seq(
                    new MatchingValidator( "V2", "two" ), // input matches so no further validators are called
                    seq(
                        new MatchingValidator( "V3", "two" ),
                        new MatchingValidator( "V4", "two" )
                    )
                )
        );

        validator.validate( reporter, bundle, "two" );

        assertContainsOnly( List.of( "V2" ), actualErrorMessages() );
    }

    @Test
    void testWrappedAddErrorIfNullStopsExecutionIfGivenObjectIsNull()
    {
        // @formatter:off
        Validator<String> validator = seq(
                (r, b, s) -> r.addErrorIfNull(s, dummyDto(), E1011),
                (r, b, s) -> addError(r, "V2")
        );

        validator.validate( reporter, bundle, null);

        assertContainsOnly( List.of( E1011.getMessage() ), actualErrorMessages() );
    }
    @Test
    void testWrappedAddErrorIfNullStopsExecutionIfGivenObjectIsNullAndNextCallInSameValidatorDoesNotAddError()
    {
        // @formatter:off
        Validator<String> validator = seq(
                (r, b, s) -> {
                    r.addErrorIfNull(s, dummyDto(), E1011);
                    r.addErrorIfNull("not null", dummyDto(), E1012);
                },
                (r, b, s) -> addError(r, "V2")
        );

        validator.validate( reporter, bundle, null);

        assertContainsOnly( List.of( E1011.getMessage() ), actualErrorMessages() );
    }
    @Test
    void testWrappedAddErrorIfNullDoesNotStopExecutionIfGivenObjectIsNotNull()
    {
        // @formatter:off
        Validator<String> validator = seq(
                (r, b, s) -> r.addErrorIfNull(s, dummyDto(), E1011),
                (r, b, s) -> addError(r, "V2")
        );

        validator.validate( reporter, bundle, "non null string" );

        assertContainsOnly( List.of( "V2" ), actualErrorMessages() );
    }

    @Test
    void testWrappedAddErrorIfStopsExecutionIfGivenPredicateSucceeds()
    {
        // @formatter:off
        Validator<String> validator = seq(
                (r, b, s) -> r.addErrorIf(() -> true, dummyDto(), E1011),
                (r, b, s) -> addError(r, "V2")
        );

        validator.validate( reporter, bundle, "irrelevant input");

        assertContainsOnly( List.of( E1011.getMessage() ), actualErrorMessages() );
    }

    @Test
    void testWrappedAddErrorIfStopsExecutionIfGivenPredicateSucceedsAndNextCallInSameValidatorDoesNotAddError()
    {
        // @formatter:off
        Validator<String> validator = seq(
                (r, b, s) -> {
                    r.addErrorIf(() -> true, dummyDto(), E1011);
                    r.addErrorIf(() -> false, dummyDto(), E1012);
                },
                (r, b, s) -> addError(r, "V2")
        );

        validator.validate( reporter, bundle, "irrelevant input");

        assertContainsOnly( List.of( E1011.getMessage() ), actualErrorMessages() );
    }
    @Test
    void testWrappedAddErrorIfDoesNotStopExecutionIfGivenPredicateFails()
    {
        // @formatter:off
        Validator<String> validator = seq(
                (r, b, s) -> r.addErrorIf(() -> false, dummyDto(), E1011),
                (r, b, s) -> addError(r, "V2")
        );

        validator.validate( reporter, bundle, "irrelevant input" );

        assertContainsOnly( List.of( "V2" ), actualErrorMessages() );
    }

    @Test
    void testSeqDoesNotCallValidatorsIfItShouldNotRunOnGivenStrategy()
    {
        bundle = TrackerBundle.builder().importStrategy(UPDATE).build();

        Validator<String> validator = seq(
                new Validator<>() {
                    @Override
                    public void validate(Reporter reporter, TrackerBundle bundle, String input) {
                        addError(reporter, "V1");
                    }

                    @Override
                    public boolean needsToRun(TrackerImportStrategy strategy) {
                        return strategy == DELETE;
                    }
                }
        );

        validator.validate( reporter, bundle, "irrelevant input" );

        assertIsEmpty(actualErrorMessages());
    }

    @Test
    void testSeqDoesNotCallValidatorsIfItShouldNotRunOnGivenStrategyForATrackerDto()
    {
        bundle = TrackerBundle.builder()
                .importStrategy(CREATE_AND_UPDATE)
                .resolvedStrategyMap(new EnumMap<>(Map.of(
                        TrackerType.EVENT, Map.of("event1", UPDATE)
                )))
                .build();

        // @formatter:off
        Validator<Event> validator = seq(
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
     * Adds an error with the {@link #message} as the error message if the input
     * equals {@link #matches}.
     */
    @RequiredArgsConstructor
    private static class MatchingValidator implements Validator<String>
    {
        private final String message;

        private final String matches;

        @Override
        public void validate( Reporter reporter, TrackerBundle bundle, String input )
        {
            if ( matches.equals( input ) )
            {
                addError(reporter, message);
            }
        }
    }

    /**
     * Add error with given message to {@link Reporter}. Every {@link Error} is attributed to a
     * {@link TrackerDto}, which makes adding errors cumbersome when you do not care about any particular tracker type, uid or error code.
     */
    private static void addError( Reporter reporter, String message )
    {
        reporter.addError( new Error( message, ValidationCode.E9999, TrackerType.TRACKED_ENTITY, "uid" ) );
    }

    /**
     * Used to add errors to {@link Reporter}. Needed since every {@link Error} is attributed to a
     * {@link TrackerDto}. In these tests we are not concerned with any particular type.
     *
     * @return tracker dto
     */
    private static TrackerDto dummyDto() {
       return new TrackerDto() {
           @Override
           public String getUid() {
               return "uid";
           }

           @Override
           public TrackerType getTrackerType() {
               return TrackerType.TRACKED_ENTITY;
           }
       };
    }

    private List<String> actualErrorMessages()
    {
        return reporter.getErrors().stream().map( Error::getMessage ).collect( Collectors.toList() );
    }
}