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
package org.hisp.dhis.tracker.validation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Every.everyItem;
import static org.hisp.dhis.tracker.TrackerImportStrategy.CREATE_AND_UPDATE;
import static org.hisp.dhis.tracker.TrackerImportStrategy.UPDATE;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import lombok.SneakyThrows;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleParams;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleCommitReport;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleValidationReport;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerImportService;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerImportReport;
import org.hisp.dhis.tracker.report.TrackerStatus;
import org.hisp.dhis.tracker.report.TrackerTypeReport;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class EventImportValidationTest
    extends AbstractImportValidationTest
{
    @Autowired
    protected TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    private RenderService _renderService;

    @Autowired
    private UserService _userService;

    @Autowired
    private ProgramStageInstanceService programStageServiceInstance;

    @Autowired
    protected TrackerImportService trackerImportService;

    @Override
    public boolean emptyDatabaseAfterTest()
    {
        return true;
    }

    @Override
    protected void setUpTest()
        throws IOException
    {
        renderService = _renderService;
        userService = _userService;

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "tracker/tracker_basic_metadata.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validationReport = objectBundleValidationService.validate( bundle );
        List<ErrorReport> errorReports = validationReport.getErrorReports();
        assertTrue( errorReports.isEmpty() );

        ObjectBundleCommitReport commit = objectBundleService.commit( bundle );
        List<ErrorReport> objectReport = commit.getErrorReports();
        assertTrue( objectReport.isEmpty() );

        TrackerImportParams trackerImportParams = createBundleFromJson(
            "tracker/validations/enrollments_te_te-data.json" );

        User user = userService.getUser( ADMIN_USER_UID );
        trackerImportParams.setUser( user );

        TrackerImportReport trackerImportReport = trackerImportService.importTracker( trackerImportParams );

        assertEquals( 0, trackerImportReport.getValidationReport().getErrorReports().size() );
        assertEquals( TrackerStatus.OK, trackerImportReport.getStatus() );

        trackerImportParams = renderService
            .fromJson(
                new ClassPathResource( "tracker/validations/enrollments_te_enrollments-data.json" ).getInputStream(),
                TrackerImportParams.class );

        trackerImportParams.setUser( user );

        trackerImportReport = trackerImportService.importTracker( trackerImportParams );

        assertEquals( 0, trackerImportReport.getValidationReport().getErrorReports().size() );
        assertEquals( TrackerStatus.OK, trackerImportReport.getStatus() );
    }

    @Test
    public void failValidationWhenTrackedEntityAttributeHasWrongOptionValue()
        throws IOException
    {
        TrackerImportParams params = createBundleFromJson(
            "tracker/validations/events-with_invalid_option_value.json" );
        params.setImportStrategy( TrackerImportStrategy.CREATE );

        TrackerImportReport trackerImportReport = trackerImportService.importTracker( params );

        assertEquals( 1, trackerImportReport.getValidationReport().getErrorReports().size() );

        assertThat( trackerImportReport.getValidationReport().getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1125 ) ) ) );
    }

    @Test
    public void successWhenTrackedEntityAttributeHasValidOptionValue()
        throws IOException
    {
        TrackerImportParams params = createBundleFromJson(
            "tracker/validations/events-with_valid_option_value.json" );
        params.setImportStrategy( TrackerImportStrategy.CREATE );

        TrackerImportReport trackerImportReport = trackerImportService.importTracker( params );

        assertEquals( 0, trackerImportReport.getValidationReport().getErrorReports().size() );
    }

    @Test
    public void testEventValidationOkAll()
        throws IOException
    {
        TrackerImportParams trackerBundleParams = createBundleFromJson( "tracker/validations/events-data.json" );
        trackerBundleParams.setImportStrategy( TrackerImportStrategy.CREATE );

        TrackerImportReport trackerImportReport = trackerImportService.importTracker( trackerBundleParams );

        assertEquals( TrackerStatus.OK, trackerImportReport.getStatus() );
        assertEquals( 0, trackerImportReport.getValidationReport().getErrorReports().size() );

    }

    @Test
    public void testCantWriteAccessCatCombo()
        throws IOException
    {
        TrackerImportParams trackerImportParams = createBundleFromJson(
            "tracker/validations/events-cat-write-access.json" );

        User user = userService.getUser( USER_6 );
        trackerImportParams.setUser( user );
        trackerImportParams.setImportStrategy( TrackerImportStrategy.CREATE );

        TrackerImportReport trackerImportReport = trackerImportService.importTracker( trackerImportParams );

        assertEquals( 4, trackerImportReport.getValidationReport().getErrorReports().size() );

        assertThat( trackerImportReport.getValidationReport().getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1099 ) ) ) );

        assertThat( trackerImportReport.getValidationReport().getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1104 ) ) ) );

        assertThat( trackerImportReport.getValidationReport().getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1096 ) ) ) );

        assertThat( trackerImportReport.getValidationReport().getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1095 ) ) ) );
    }

    @Test
    public void testNoWriteAccessToOrg()
        throws IOException
    {
        TrackerImportParams trackerBundleParams = createBundleFromJson( "tracker/validations/events-data.json" );

        User user = userService.getUser( USER_2 );
        trackerBundleParams.setUser( user );
        trackerBundleParams.setImportStrategy( TrackerImportStrategy.CREATE );

        TrackerImportReport trackerImportReport = trackerImportService.importTracker( trackerBundleParams );

        assertEquals( 1, trackerImportReport.getValidationReport().getErrorReports().size() );

        assertThat( trackerImportReport.getValidationReport().getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1000 ) ) ) );
    }

    @Test
    public void testNonRepeatableProgramStage()
        throws IOException
    {
        TrackerImportParams trackerImportParams = fromJson(
            "tracker/validations/events_non-repeatable-programstage_part1.json",
            userService.getUser( ADMIN_USER_UID ) );
        trackerImportParams.setImportStrategy( TrackerImportStrategy.CREATE );

        TrackerImportReport trackerImportReport = trackerImportService.importTracker( trackerImportParams );

        assertEquals( 0, trackerImportReport.getValidationReport().getErrorReports().size() );

        trackerImportParams = fromJson(
            "tracker/validations/events_non-repeatable-programstage_part2.json",
            userService.getUser( ADMIN_USER_UID ) );

        trackerImportParams.setImportStrategy( TrackerImportStrategy.CREATE );

        trackerImportReport = trackerImportService.importTracker( trackerImportParams );

        assertEquals( 1, trackerImportReport.getValidationReport().getErrorReports().size() );

        assertThat( trackerImportReport.getValidationReport().getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1039 ) ) ) );
    }

    @Test( expected = IOException.class )
    public void testWrongScheduledDateString()
        throws IOException
    {
        createBundleFromJson( "tracker/validations/events_error-no-wrong-date.json" );
    }

    @Test
    public void testNonDefaultCategoryCombo()
        throws IOException
    {
        TrackerImportParams trackerBundleParams = createBundleFromJson(
            "tracker/validations/events_non-default-combo.json" );
        trackerBundleParams.setImportStrategy( TrackerImportStrategy.CREATE );

        TrackerImportReport trackerImportReport = trackerImportService.importTracker( trackerBundleParams );

        assertEquals( 1, trackerImportReport.getValidationReport().getErrorReports().size() );

        assertThat( trackerImportReport.getValidationReport().getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1055 ) ) ) );
    }

    @Test
    public void testNoCategoryOptionCombo()
        throws IOException
    {
        TrackerImportParams trackerBundleParams = createBundleFromJson(
            "tracker/validations/events_cant-find-cat-opt-combo.json" );
        trackerBundleParams.setImportStrategy( TrackerImportStrategy.CREATE );

        TrackerImportReport trackerImportReport = trackerImportService.importTracker( trackerBundleParams );

        assertEquals( 1, trackerImportReport.getValidationReport().getErrorReports().size() );

        assertThat( trackerImportReport.getValidationReport().getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1115 ) ) ) );
    }

    @Test
    public void testNoCategoryOption()
        throws IOException
    {
        TrackerImportParams trackerBundleParams = createBundleFromJson(
            "tracker/validations/events_cant-find-cat-option.json" );
        trackerBundleParams.setImportStrategy( TrackerImportStrategy.CREATE );

        TrackerImportReport trackerImportReport = trackerImportService.importTracker( trackerBundleParams );
        assertEquals( 1, trackerImportReport.getValidationReport().getErrorReports().size() );

        assertThat( trackerImportReport.getValidationReport().getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1116 ) ) ) );
    }

    @Test
    public void testNoCategoryOptionComboSet()
        throws IOException
    {
        TrackerImportParams trackerBundleParams = createBundleFromJson(
            "tracker/validations/events_cant-find-cat-option-combo-set.json" );
        trackerBundleParams.setImportStrategy( TrackerImportStrategy.CREATE );

        TrackerImportReport trackerImportReport = trackerImportService.importTracker( trackerBundleParams );

        assertEquals( 2, trackerImportReport.getValidationReport().getErrorReports().size() );

        assertThat( trackerImportReport.getValidationReport().getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1117 ) ) ) );
    }

    @Test
    public void testWrongDatesInCatCombo()
        throws IOException
    {
        TrackerImportParams trackerBundleParams = createBundleFromJson(
            "tracker/validations/events_combo-date-wrong.json" );
        trackerBundleParams.setImportStrategy( TrackerImportStrategy.CREATE );

        TrackerImportReport trackerImportReport = trackerImportService.importTracker( trackerBundleParams );

        assertEquals( 2, trackerImportReport.getValidationReport().getErrorReports().size() );

        assertThat( trackerImportReport.getValidationReport().getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1056 ) ) ) );

        assertThat( trackerImportReport.getValidationReport().getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1057 ) ) ) );
    }

    @Test
    public void testValidateAndAddNotesToEvent()
        throws IOException
    {
        Date now = new Date();

        // When

        TrackerImportReport trackerImportReport = createEvent( "tracker/validations/events-with-notes-data.json" );

        // Then

        // Fetch the UID of the newly created event
        final ProgramStageInstance programStageInstance = getEventFromReport( trackerImportReport );

        assertThat( programStageInstance.getComments(), hasSize( 3 ) );
        // Validate note content
        Stream.of( "first note", "second note", "third note" ).forEach( t -> {

            TrackedEntityComment comment = getByComment( programStageInstance.getComments(), t );
            assertTrue( CodeGenerator.isValidUid( comment.getUid() ) );
            assertTrue( comment.getCreated().getTime() > now.getTime() );
            assertTrue( comment.getLastUpdated().getTime() > now.getTime() );
            assertNull( comment.getCreator() );
            assertEquals( ADMIN_USER_UID, comment.getLastUpdatedBy().getUid() );
        } );
    }

    @Test
    public void testValidateAndAddNotesToUpdatedEvent()
        throws IOException
    {
        Date now = new Date();

        // Given -> Creates an event with 3 notes
        createEvent( "tracker/validations/events-with-notes-data.json" );

        // When -> Update the event and adds 3 more notes
        TrackerImportReport trackerImportReport = createEvent(
            "tracker/validations/events-with-notes-update-data.json" );

        // Then
        final ProgramStageInstance programStageInstance = getEventFromReport( trackerImportReport );

        assertThat( programStageInstance.getComments(), hasSize( 6 ) );

        // validate note content
        Stream.of( "first note", "second note", "third note", "4th note", "5th note", "6th note" ).forEach( t -> {

            TrackedEntityComment comment = getByComment( programStageInstance.getComments(), t );
            assertTrue( CodeGenerator.isValidUid( comment.getUid() ) );
            assertTrue( comment.getCreated().getTime() > now.getTime() );
            assertTrue( comment.getLastUpdated().getTime() > now.getTime() );
            assertNull( comment.getCreator() );
            assertEquals( ADMIN_USER_UID, comment.getLastUpdatedBy().getUid() );
        } );
    }

    @Test
    public void testUpdateDeleteEventFails()
    {
        testDeletedEventFails( UPDATE );
    }

    @Test
    public void testInserDeleteEventFails()
    {
        testDeletedEventFails( CREATE_AND_UPDATE );
    }

    @SneakyThrows
    private void testDeletedEventFails( TrackerImportStrategy importStrategy )
    {
        // Given -> Creates an event
        createEvent( "tracker/validations/events-with-notes-data.json" );

        ProgramStageInstance event = programStageServiceInstance.getProgramStageInstance( "uLxFbxfYDQE" );
        assertNotNull( event );

        // When -> Soft-delete the event
        programStageServiceInstance.deleteProgramStageInstance( event );

        TrackerImportParams trackerBundleParams = createBundleFromJson(
            "tracker/validations/events-with-notes-data.json" );
        trackerBundleParams.setImportStrategy( TrackerImportStrategy.CREATE );

        // When
        TrackerImportReport trackerImportReport = trackerImportService.importTracker( trackerBundleParams );

        assertEquals( 1, trackerImportReport.getValidationReport().getErrorReports().size() );
        assertThat( trackerImportReport.getValidationReport().getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1082 ) ) ) );
    }

    private TrackerImportReport createEvent( String jsonPayload )
        throws IOException
    {
        // Given
        TrackerImportParams trackerBundleParams = createBundleFromJson( jsonPayload );
        trackerBundleParams.setImportStrategy( CREATE_AND_UPDATE );

        // When
        TrackerImportReport trackerImportReport = trackerImportService.importTracker( trackerBundleParams );

        // Then
        assertEquals( TrackerStatus.OK, trackerImportReport.getStatus() );
        assertEquals( 0, trackerImportReport.getValidationReport().getErrorReports().size() );

        return trackerImportReport;
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

    private ProgramStageInstance getEventFromReport( TrackerImportReport trackerImportReport )
    {
        final Map<TrackerType, TrackerTypeReport> typeReportMap = trackerImportReport.getBundleReport()
            .getTypeReportMap();
        String newEvent = typeReportMap.get( TrackerType.EVENT ).getObjectReportMap().get( 0 ).getUid();
        return programStageServiceInstance.getProgramStageInstance( newEvent );
    }
}
