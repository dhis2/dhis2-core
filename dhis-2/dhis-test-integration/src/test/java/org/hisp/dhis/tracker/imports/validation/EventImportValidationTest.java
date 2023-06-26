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
package org.hisp.dhis.tracker.imports.validation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.tracker.Assertions.assertHasOnlyErrors;
import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.hisp.dhis.tracker.imports.TrackerImportStrategy.CREATE_AND_UPDATE;
import static org.hisp.dhis.tracker.imports.TrackerImportStrategy.DELETE;
import static org.hisp.dhis.tracker.imports.TrackerImportStrategy.UPDATE;
import static org.hisp.dhis.tracker.imports.validation.Users.USER_2;
import static org.hisp.dhis.tracker.imports.validation.Users.USER_6;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import lombok.SneakyThrows;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.EventService;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.TrackerType;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.tracker.imports.report.TrackerTypeReport;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class EventImportValidationTest extends TrackerTest
{
    @Autowired
    protected TrackedEntityService trackedEntityService;

    @Autowired
    private EventService programStageServiceInstance;

    @Autowired
    private TrackerImportService trackerImportService;

    @Autowired
    private UserService _userService;

    @Override
    protected void initTest()
        throws IOException
    {
        userService = _userService;
        setUpMetadata( "tracker/tracker_basic_metadata.json" );
        injectAdminUser();
        assertNoErrors( trackerImportService.importTracker( fromJson(
            "tracker/validations/enrollments_te_te-data.json" ) ) );
        assertNoErrors( trackerImportService
            .importTracker( fromJson( "tracker/validations/enrollments_te_enrollments-data.json" ) ) );
    }

    @Test
    void testInvalidEnrollmentPreventsValidEventFromBeingCreated()
        throws IOException
    {
        ImportReport importReport = trackerImportService
            .importTracker( fromJson( "tracker/validations/invalid_enrollment_with_valid_event.json" ) );

        assertHasOnlyErrors( importReport, ValidationCode.E1070, ValidationCode.E5000 );
    }

    @Test
    void failValidationWhenTrackedEntityAttributeHasWrongOptionValue()
        throws IOException
    {
        ImportReport importReport = trackerImportService.importTracker( fromJson(
            "tracker/validations/events-with_invalid_option_value.json" ) );

        assertHasOnlyErrors( importReport, ValidationCode.E1125 );
    }

    @Test
    void successWhenTrackedEntityAttributeHasValidOptionValue()
        throws IOException
    {
        ImportReport importReport = trackerImportService
            .importTracker( fromJson( "tracker/validations/events-with_valid_option_value.json" ) );

        assertNoErrors( importReport );
    }

    @Test
    void testEventValidationOkAll()
        throws IOException
    {
        ImportReport importReport = trackerImportService
            .importTracker( fromJson( "tracker/validations/events-with-registration.json" ) );

        assertNoErrors( importReport );
    }

    @Test
    void testEventValidationOkWithoutAttributeOptionCombo()
        throws IOException
    {
        ImportReport importReport = trackerImportService.importTracker( fromJson(
            "tracker/validations/events-without-attribute-option-combo.json" ) );

        assertNoErrors( importReport );
    }

    @Test
    void testTrackerAndProgramEventUpdateSuccess()
        throws IOException
    {
        TrackerImportParams trackerBundleParams = fromJson(
            "tracker/validations/program_and_tracker_events.json" );
        assertNoErrors( trackerImportService.importTracker( trackerBundleParams ) );

        trackerBundleParams.setImportStrategy( UPDATE );
        ImportReport importReport = trackerImportService.importTracker( trackerBundleParams );

        assertNoErrors( importReport );
    }

    @Test
    void testCantWriteAccessCatCombo()
        throws IOException
    {
        TrackerImportParams trackerImportParams = fromJson(
            "tracker/validations/events-cat-write-access.json" );
        User user = userService.getUser( USER_6 );
        trackerImportParams.setUser( user );

        ImportReport importReport = trackerImportService.importTracker( trackerImportParams );

        assertHasOnlyErrors( importReport, ValidationCode.E1096, ValidationCode.E1099, ValidationCode.E1104,
            ValidationCode.E1095 );
    }

    @Test
    void testNoWriteAccessToOrg()
        throws IOException
    {
        TrackerImportParams trackerBundleParams = fromJson(
            "tracker/validations/events-with-registration.json" );
        User user = userService.getUser( USER_2 );
        trackerBundleParams.setUser( user );
        ImportReport importReport = trackerImportService.importTracker( trackerBundleParams );
        assertHasOnlyErrors( importReport, ValidationCode.E1000 );
    }

    @Test
    void testNonRepeatableProgramStage()
        throws IOException
    {
        TrackerImportParams trackerImportParams = fromJson(
            "tracker/validations/events_non-repeatable-programstage_part1.json" );

        ImportReport importReport = trackerImportService.importTracker( trackerImportParams );

        assertNoErrors( importReport );

        trackerImportParams = fromJson( "tracker/validations/events_non-repeatable-programstage_part2.json" );

        importReport = trackerImportService.importTracker( trackerImportParams );

        assertHasOnlyErrors( importReport, ValidationCode.E1039 );
    }

    @Test
    void testNonRepeatableProgramStageForProgramEvent()
        throws IOException
    {
        TrackerImportParams trackerImportParams = fromJson(
            "tracker/validations/program_events_non-repeatable-programstage_part1.json" );

        ImportReport importReport = trackerImportService.importTracker( trackerImportParams );

        assertNoErrors( importReport );

        trackerImportParams = fromJson( "tracker/validations/program_events_non-repeatable-programstage_part2.json" );

        importReport = trackerImportService.importTracker( trackerImportParams );

        assertNoErrors( importReport );
    }

    @Test
    void testWrongScheduledDateString()
    {
        assertThrows( IOException.class,
            () -> fromJson( "tracker/validations/events_error-no-wrong-date.json" ) );
    }

    @Test
    void testEventProgramHasNonDefaultCategoryCombo()
        throws IOException
    {
        ImportReport importReport = trackerImportService.importTracker( fromJson(
            "tracker/validations/events_non-default-combo.json" ) );

        assertHasOnlyErrors( importReport, ValidationCode.E1055 );
    }

    @Test
    void testCategoryOptionComboNotFound()
        throws IOException
    {
        ImportReport importReport = trackerImportService.importTracker( fromJson(
            "tracker/validations/events_cant-find-cat-opt-combo.json" ) );

        assertHasOnlyErrors( importReport, ValidationCode.E1115 );
    }

    @Test
    void testCategoryOptionComboNotFoundGivenSubsetOfCategoryOptions()
        throws IOException
    {
        ImportReport importReport = trackerImportService.importTracker( fromJson(
            "tracker/validations/events_cant-find-aoc-with-subset-of-cos.json" ) );

        assertHasOnlyErrors( importReport, ValidationCode.E1117 );
    }

    @Test
    void testCOFoundButAOCNotFound()
        throws IOException
    {
        ImportReport importReport = trackerImportService.importTracker( fromJson(
            "tracker/validations/events_cant-find-aoc-but-co-exists.json" ) );

        assertHasOnlyErrors( importReport, ValidationCode.E1115 );
    }

    @Test
    void testCategoryOptionsNotFound()
        throws IOException
    {
        ImportReport importReport = trackerImportService.importTracker( fromJson(
            "tracker/validations/events_cant-find-cat-option.json" ) );

        assertHasOnlyErrors( importReport, ValidationCode.E1116 );
    }

    @Test
    void testAttributeCategoryOptionNotInProgramCC()
        throws IOException
    {
        ImportReport importReport = trackerImportService.importTracker( fromJson(
            "tracker/validations/events-aoc-not-in-program-cc.json" ) );

        assertHasOnlyErrors( importReport, ValidationCode.E1054 );
    }

    @Test
    void testAttributeCategoryOptionAndCODoNotMatch()
        throws IOException
    {
        ImportReport importReport = trackerImportService.importTracker( fromJson(
            "tracker/validations/events-aoc-and-co-dont-match.json" ) );

        assertHasOnlyErrors( importReport, ValidationCode.E1117 );
    }

    @Test
    void testAttributeCategoryOptionCannotBeFoundForEventProgramCCAndGivenCategoryOption()
        throws IOException
    {
        ImportReport importReport = trackerImportService.importTracker( fromJson(
            "tracker/validations/events_cant-find-cat-option-combo-for-given-cc-and-co.json" ) );

        assertHasOnlyErrors( importReport, ValidationCode.E1117 );
    }

    @Test
    void testWrongDatesInCatCombo()
        throws IOException
    {
        ImportReport importReport = trackerImportService.importTracker( fromJson(
            "tracker/validations/events_combo-date-wrong.json" ) );

        assertHasOnlyErrors( importReport, ValidationCode.E1056, ValidationCode.E1057 );
    }

    @Test
    void testValidateAndAddNotesToEvent()
        throws IOException
    {
        Date now = new Date();
        // When
        ImportReport importReport = createEvent( "tracker/validations/events-with-notes-data.json" );
        // Then
        // Fetch the UID of the newly created event
        final Event event = getEventFromReport( importReport );
        assertThat( event.getComments(), hasSize( 3 ) );
        // Validate note content
        Stream.of( "first note", "second note", "third note" ).forEach( t -> {
            TrackedEntityComment comment = getByComment( event.getComments(), t );
            assertTrue( CodeGenerator.isValidUid( comment.getUid() ) );
            assertTrue( comment.getCreated().getTime() > now.getTime() );
            assertTrue( comment.getLastUpdated().getTime() > now.getTime() );
            assertNull( comment.getCreator() );
            assertEquals( ADMIN_USER_UID, comment.getLastUpdatedBy().getUid() );
        } );
    }

    @Test
    void testValidateAndAddNotesToUpdatedEvent()
        throws IOException
    {
        Date now = new Date();
        // Given -> Creates an event with 3 notes
        createEvent( "tracker/validations/events-with-notes-data.json" );
        // When -> Update the event and adds 3 more notes
        ImportReport importReport = createEvent(
            "tracker/validations/events-with-notes-update-data.json" );
        // Then
        final Event event = getEventFromReport( importReport );
        assertThat( event.getComments(), hasSize( 6 ) );
        // validate note content
        Stream.of( "first note", "second note", "third note", "4th note", "5th note", "6th note" ).forEach( t -> {
            TrackedEntityComment comment = getByComment( event.getComments(), t );
            assertTrue( CodeGenerator.isValidUid( comment.getUid() ) );
            assertTrue( comment.getCreated().getTime() > now.getTime() );
            assertTrue( comment.getLastUpdated().getTime() > now.getTime() );
            assertNull( comment.getCreator() );
            assertEquals( ADMIN_USER_UID, comment.getLastUpdatedBy().getUid() );
        } );
    }

    @Test
    void testUpdateDeleteEventFails()
    {
        testDeletedEventFails( UPDATE );
    }

    @Test
    void testInsertDeleteEventFails()
    {
        testDeletedEventFails( CREATE_AND_UPDATE );
    }

    @SneakyThrows
    private void testDeletedEventFails( TrackerImportStrategy importStrategy )
    {
        // Given -> Creates an event
        createEvent( "tracker/validations/events-with-notes-data.json" );
        Event event = programStageServiceInstance.getEvent( "uLxFbxfYDQE" );
        assertNotNull( event );
        // When -> Soft-delete the event
        programStageServiceInstance.deleteEvent( event );
        TrackerImportParams trackerBundleParams = fromJson(
            "tracker/validations/events-with-notes-data.json" );
        trackerBundleParams.setImportStrategy( importStrategy );
        // When
        ImportReport importReport = trackerImportService.importTracker( trackerBundleParams );

        assertHasOnlyErrors( importReport, ValidationCode.E1082 );
    }

    @Test
    void testEventDeleteOk()
        throws IOException
    {
        TrackerImportParams trackerBundleParams = fromJson(
            "tracker/validations/events-with-registration.json" );

        ImportReport importReport = trackerImportService.importTracker( trackerBundleParams );

        assertNoErrors( importReport );

        manager.flush();
        manager.clear();

        TrackerImportParams paramsDelete = fromJson(
            "tracker/validations/event-data-delete.json" );
        paramsDelete.setImportStrategy( DELETE );

        ImportReport importReportDelete = trackerImportService.importTracker( paramsDelete );
        assertNoErrors( importReportDelete );
        assertEquals( 1, importReportDelete.getStats().getDeleted() );
    }

    private ImportReport createEvent( String jsonPayload )
        throws IOException
    {
        // Given
        TrackerImportParams trackerBundleParams = fromJson( jsonPayload );
        trackerBundleParams.setImportStrategy( CREATE_AND_UPDATE );
        // When
        ImportReport importReport = trackerImportService.importTracker( trackerBundleParams );
        // Then
        assertNoErrors( importReport );
        return importReport;
    }

    private TrackedEntityComment getByComment( List<TrackedEntityComment> comments, String commentText )
    {
        for ( TrackedEntityComment comment : comments )
        {
            if ( comment.getCommentText().startsWith( commentText )
                || comment.getCommentText().endsWith( commentText ) )
            {
                return comment;
            }
        }
        fail( "Can't find a comment starting or ending with " + commentText );
        return null;
    }

    private Event getEventFromReport( ImportReport importReport )
    {
        final Map<TrackerType, TrackerTypeReport> typeReportMap = importReport.getPersistenceReport()
            .getTypeReportMap();
        String newEvent = typeReportMap.get( TrackerType.EVENT ).getEntityReportMap().get( 0 ).getUid();
        return programStageServiceInstance.getEvent( newEvent );
    }
}
