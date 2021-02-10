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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Every.everyItem;
import static org.hisp.dhis.tracker.TrackerImportStrategy.CREATE_AND_UPDATE;
import static org.hisp.dhis.tracker.TrackerImportStrategy.UPDATE;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1029;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.report.TrackerBundleReport;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerStatus;
import org.hisp.dhis.tracker.report.TrackerTypeReport;
import org.hisp.dhis.tracker.report.TrackerValidationReport;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.Ignore;
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

        TrackerBundle trackerBundle = trackerBundleService.create( trackerImportParams );
        assertEquals( 5, trackerBundle.getTrackedEntities().size() );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );
        assertEquals( 0, report.getErrorReports().size() );

        TrackerBundleReport bundleReport = trackerBundleService.commit( trackerBundle );
        assertEquals( TrackerStatus.OK, bundleReport.getStatus() );

        trackerImportParams = renderService
            .fromJson(
                new ClassPathResource( "tracker/validations/enrollments_te_enrollments-data.json" ).getInputStream(),
                TrackerImportParams.class );

        trackerImportParams.setUser( user );

        trackerBundle = trackerBundleService.create( trackerImportParams );
        assertEquals( 4, trackerBundle.getEnrollments().size() );

        report = trackerValidationService.validate( trackerBundle );
        assertEquals( 0, report.getErrorReports().size() );

        bundleReport = trackerBundleService.commit( trackerBundle );
        assertEquals( TrackerStatus.OK, bundleReport.getStatus() );
    }

    @Test
    public void testEventValidationOkAll()
        throws IOException
    {
        TrackerImportParams trackerBundleParams = createBundleFromJson( "tracker/validations/events-data.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( trackerBundleParams,
            TrackerImportStrategy.CREATE );
        assertEquals( 1, createAndUpdate.getTrackerBundle().getEvents().size() );
        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );
        assertEquals( TrackerStatus.OK, createAndUpdate.getCommitReport().getStatus() );

        assertEquals( 0, report.getErrorReports().size() );

    }

    @Test
    public void testEventInvalidUidFormat()
        throws IOException
    {
        TrackerImportParams trackerBundleParams = createBundleFromJson(
            "tracker/validations/events-invalid-uid-format.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( trackerBundleParams,
            TrackerImportStrategy.CREATE );
        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );

        assertEquals( 1, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1048 ) ) ) );
    }

    @Test
    public void testCantWriteAccessCatCombo()
        throws IOException
    {
        TrackerImportParams trackerImportParams = createBundleFromJson(
            "tracker/validations/events-cat-write-access.json" );

        User user = userService.getUser( USER_6 );
        trackerImportParams.setUser( user );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( trackerImportParams,
            TrackerImportStrategy.CREATE );
        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );

        assertEquals( 4, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1099 ) ) ) );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1104 ) ) ) );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1096 ) ) ) );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1095 ) ) ) );
    }

    @Test
    public void testEventMissingOrgUnit()
        throws IOException
    {
        TrackerImportParams trackerBundleParams = createBundleFromJson(
            "tracker/validations/events_error-orgunit-missing.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( trackerBundleParams,
            TrackerImportStrategy.CREATE );
        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );

        assertEquals( 1, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1011 ) ) ) );
    }

    @Test
    public void testEventMissingProgramAndProgramStage()
        throws IOException
    {
        TrackerImportParams trackerBundleParams = createBundleFromJson(
            "tracker/validations/events_error-program-pstage-missing.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( trackerBundleParams,
            TrackerImportStrategy.CREATE );
        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );

        assertEquals( 1, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1088 ) ) ) );
    }

    @Test
    public void testEventMissingProgramStageProgramIsRegistration()
        throws IOException
    {
        TrackerImportParams trackerBundleParams = createBundleFromJson(
            "tracker/validations/events_error-pstage-missing-isreg.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( trackerBundleParams,
            TrackerImportStrategy.CREATE );
        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );

        assertEquals( 1, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1086 ) ) ) );
    }

    @Test
    public void testEventMissingProgramStageProgramIsWithoutRegistration()
        throws IOException
    {
        TrackerImportParams trackerBundleParams = createBundleFromJson(
            "tracker/validations/events_error-pstage-missing-withoutreg.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( trackerBundleParams,
            TrackerImportStrategy.CREATE );
        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );

        assertEquals( 1, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1123 ) ) ) );
    }

    @Test
    public void testProgramStageProgramDifferentPrograms()
        throws IOException
    {
        TrackerImportParams trackerBundleParams = createBundleFromJson(
            "tracker/validations/events_error-pstage-program-different.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( trackerBundleParams,
            TrackerImportStrategy.CREATE );
        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );

        assertEquals( 1, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1089 ) ) ) );
    }

    @Test
    public void testNoWriteAccessToOrg()
        throws IOException
    {
        TrackerImportParams trackerBundleParams = createBundleFromJson( "tracker/validations/events-data.json" );

        User user = userService.getUser( USER_2 );
        trackerBundleParams.setUser( user );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( trackerBundleParams,
            TrackerImportStrategy.CREATE );
        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );

        assertEquals( 1, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1000 ) ) ) );
    }

    @Test
    public void testEventCreateAlreadyExists()
        throws IOException
    {
        TrackerImportParams trackerBundleParams = createBundleFromJson( "tracker/validations/events-data.json" );

        User user = userService.getUser( ADMIN_USER_UID );
        trackerBundleParams.setUserId( user.getUid() );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerBundleParams );
        assertEquals( 1, trackerBundle.getEvents().size() );

        // Validate first time, should contain no errors.
        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );
        printReport( report );
        assertEquals( 0, report.getErrorReports().size() );

        // Commit the validated bundle...
        trackerBundleService.commit( trackerBundle );

        trackerBundleParams.setImportStrategy( TrackerImportStrategy.CREATE );
        trackerBundle = trackerBundleService.create( trackerBundleParams );
        // Re-validate, should now contain 13 errors...
        report = trackerValidationService.validate( trackerBundle );
        printReport( report );
        assertEquals( 1, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1030 ) ) ) );

        // All should be removed
        assertEquals( 0, trackerBundle.getEnrollments().size() );
    }

    @Test
    public void testUpdateNotExists()
        throws IOException
    {
        TrackerImportParams trackerBundleParams = createBundleFromJson( "tracker/validations/events-data.json" );

        User user = userService.getUser( ADMIN_USER_UID );
        trackerBundleParams.setUserId( user.getUid() );

        trackerBundleParams.setImportStrategy( TrackerImportStrategy.UPDATE );
        TrackerBundle trackerBundle = trackerBundleService.create( trackerBundleParams );
        assertEquals( 1, trackerBundle.getEvents().size() );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );
        printReport( report );

        assertEquals( 1, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1032 ) ) ) );

        // All should be removed
        assertEquals( 0, trackerBundle.getEnrollments().size() );
    }

    @Test
    public void testMissingDate()
        throws IOException
    {
        TrackerImportParams trackerBundleParams = createBundleFromJson(
            "tracker/validations/events_error-missing-date.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( trackerBundleParams,
            TrackerImportStrategy.CREATE );

        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );

        assertEquals( 1, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1031 ) ) ) );
    }

    @Test
    public void testMissingTei()
        throws IOException
    {
        TrackerImportParams trackerBundleParams = createBundleFromJson(
            "tracker/validations/events_error-missing-tei.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( trackerBundleParams,
            TrackerImportStrategy.CREATE );
        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );

        assertEquals( 1, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1036 ) ) ) );
    }

    @Test
    public void testNonRepeatableProgramStage()
        throws IOException
    {
        TrackerImportParams trackerImportParams = fromJson(
            "tracker/validations/events_non-repeatable-programstage_part1.json",
            userService.getUser( ADMIN_USER_UID ) );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( trackerImportParams,
            TrackerImportStrategy.CREATE );
        assertEquals( 1, createAndUpdate.getTrackerBundle().getEvents().size() );

        TrackerValidationReport validationReport = createAndUpdate.getValidationReport();
        printReport( validationReport );

        assertEquals( 0, validationReport.getErrorReports().size() );

        trackerImportParams = fromJson(
            "tracker/validations/events_non-repeatable-programstage_part2.json",
            userService.getUser( ADMIN_USER_UID ) );

        createAndUpdate = validateAndCommit( trackerImportParams, TrackerImportStrategy.CREATE );
        assertEquals( 0, createAndUpdate.getTrackerBundle().getEvents().size() );

        validationReport = createAndUpdate.getValidationReport();
        printReport( validationReport );

        assertEquals( 1, validationReport.getErrorReports().size() );

        assertThat( validationReport.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1039 ) ) ) );
    }

    // TODO: Need help setting up this test. Need a user with all access, but
    // lacking the F_EDIT_EXPIRED auth.
    @Test
    @Ignore( "Need to setup metadata with user without F_EDIT_EXPIRED" )
    public void testMissingCompletedDate()
        throws IOException
    {
        TrackerImportParams trackerBundleParams = createBundleFromJson(
            "tracker/validations/events_error-no-completed-date.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( trackerBundleParams,
            TrackerImportStrategy.CREATE );

        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );

        assertEquals( 1, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1042 ) ) ) );
    }

    @Test
    public void testMissingAndAssignScheduleDate()
        throws IOException
    {
        TrackerImportParams trackerBundleParams = createBundleFromJson(
            "tracker/validations/events_error-missing-schedule-date_part1.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( trackerBundleParams,
            TrackerImportStrategy.CREATE );

        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );

        assertEquals( 1, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1050 ) ) ) );

        trackerBundleParams = createBundleFromJson(
            "tracker/validations/events_error-missing-schedule-date_part2.json" );

        createAndUpdate = validateAndCommit( trackerBundleParams,
            TrackerImportStrategy.CREATE );

        assertEquals( 1, createAndUpdate.getTrackerBundle().getEvents().size() );

        report = createAndUpdate.getValidationReport();

        printReport( report );

        assertEquals( 0, report.getErrorReports().size() );

        ProgramStageInstance psi = programStageServiceInstance.getProgramStageInstance( "ZwwuwNp6gVd" );

        assertEquals( psi.getExecutionDate(), psi.getDueDate() );
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

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( trackerBundleParams,
            TrackerImportStrategy.CREATE );

        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );

        assertEquals( 1, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1055 ) ) ) );
    }

    @Test
    public void testNoCategoryOptionCombo()
        throws IOException
    {
        TrackerImportParams trackerBundleParams = createBundleFromJson(
            "tracker/validations/events_cant-find-cat-opt-combo.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( trackerBundleParams,
            TrackerImportStrategy.CREATE );
        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );

        assertEquals( 1, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1115 ) ) ) );
    }

    @Test
    public void testNoCategoryOption()
        throws IOException
    {
        TrackerImportParams trackerBundleParams = createBundleFromJson(
            "tracker/validations/events_cant-find-cat-option.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( trackerBundleParams,
            TrackerImportStrategy.CREATE );
        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );

        assertEquals( 1, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1116 ) ) ) );
    }

    @Test
    public void testNoCategoryOptionComboSet()
        throws IOException
    {
        TrackerImportParams trackerBundleParams = createBundleFromJson(
            "tracker/validations/events_cant-find-cat-option-combo-set.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( trackerBundleParams,
            TrackerImportStrategy.CREATE );
        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );

        assertEquals( 2, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1117 ) ) ) );
    }

    @Test
    public void testWrongDatesInCatCombo()
        throws IOException
    {
        TrackerImportParams trackerBundleParams = createBundleFromJson(
            "tracker/validations/events_combo-date-wrong.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( trackerBundleParams,
            TrackerImportStrategy.CREATE );

        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );

        assertEquals( 2, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1056 ) ) ) );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1057 ) ) ) );
    }

    @Test
    public void testEventHasNoEnrollment()
        throws IOException
    {
        TrackerImportParams trackerBundleParams = createBundleFromJson(
            "tracker/validations/events_tei-not-enrolled.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( trackerBundleParams,
            TrackerImportStrategy.CREATE );
        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );

        assertEquals( 1, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1033 ) ) ) );
    }

    @Test
    public void testInvalidDataElementForDataValue()
        throws IOException
    {
        TrackerImportParams trackerBundleParams = createBundleFromJson(
            "tracker/validations/event_with_wrong_de_for_data_value.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( trackerBundleParams,
            TrackerImportStrategy.CREATE );
        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );

        assertEquals( 1, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1087 ) ) ) );
    }

    @Test
    @Ignore
    public void testTeiMultipleActiveEnrollments()
        throws IOException
    {
        TrackerImportParams trackerBundleParams = createBundleFromJson(
            "tracker/validations/events_tei-multiple-enrollments.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( trackerBundleParams,
            TrackerImportStrategy.CREATE );
        assertEquals( 0, createAndUpdate.getTrackerBundle().getEvents().size() );
        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );

        assertEquals( 1, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1038 ) ) ) );
    }

    // TODO: Can't get this to work, the preheater? inserts a program instance.
    @Test
    @Ignore( "Can't get this to work, the preheater? inserts a program instance." )
    public void testTeiMultipleActiveEnrollmentsInNonRegProgram()
        throws IOException
    {
        TrackerImportParams trackerBundleParams = createBundleFromJson(
            "tracker/validations/events_tei-multiple-enrollments-in-non-reg-program.json" );

        User user = userService.getUser( ADMIN_USER_UID );
        trackerBundleParams.setUserId( user.getUid() );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerBundleParams );
        assertEquals( 1, trackerBundle.getEvents().size() );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );
        printReport( report );

        assertEquals( 1, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1040 ) ) ) );
    }

    // TODO: Delete not working yet
    @Test
    @Ignore( "Delete not yet working" )
    public void testEventAlreadyDeleted()
        throws IOException
    {
        TrackerImportParams params = createBundleFromJson( "tracker/validations/events-data.json" );

        User user = userService.getUser( ADMIN_USER_UID );
        params.setUserId( user.getUid() );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( params,
            CREATE_AND_UPDATE );
        assertEquals( 0, createAndUpdate.getValidationReport().getErrorReports().size() );

        ValidateAndCommitTestUnit delete = validateAndCommit( params,
            TrackerImportStrategy.DELETE );
        assertEquals( 0, delete.getValidationReport().getErrorReports().size() );

        ValidateAndCommitTestUnit deleteAgain = validateAndCommit( params,
            TrackerImportStrategy.DELETE );
        assertEquals( 1, deleteAgain.getValidationReport().getErrorReports().size() );

        assertThat( deleteAgain.getValidationReport().getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1030 ) ) ) );

        // All should be removed
        assertEquals( 0, createAndUpdate.getTrackerBundle().getEnrollments().size() );
    }

    // TODO: See comments on error codes, seems to not be in use....
    @Test
    @Ignore( "Cant provoke, need more investigation" )
    public void testPeriodTypes()
        throws IOException
    {
        TrackerImportParams trackerBundleParams = createBundleFromJson(
            "tracker/validations/events_error-periodtype.json" );

        User user = userService.getUser( ADMIN_USER_UID );
        trackerBundleParams.setUserId( user.getUid() );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerBundleParams );
        assertEquals( 1, trackerBundle.getEvents().size() );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );
        printReport( report );

        assertEquals( 1, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1042 ) ) ) );
    }

    // TODO: Needs clarification, can't test this error: E1082.
    // See comments in:
    // org/hisp/dhis/tracker/validation/hooks/PreCheckDataRelationsValidationHook.java:165
    @Test
    @Ignore( "Needs clarification, can't test this error: E1082. Maybe because delete not yet working" )
    public void testProgramStageDeleted()
        throws IOException
    {
        TrackerImportParams trackerBundleParams = renderService
            .fromJson(
                new ClassPathResource( "tracker/validations/events-data.json" ).getInputStream(),
                TrackerImportParams.class );

        User user = userService.getUser( "M5zQapPyTZI" );
        trackerBundleParams.setUserId( user.getUid() );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerBundleParams );
        assertEquals( 1, trackerBundle.getEvents().size() );

        // Validate first time, should contain no errors.
        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );
        assertEquals( 0, report.getErrorReports().size() );

        // Commit the validated bundle...
        trackerBundleService.commit( trackerBundle );

        ProgramStageInstance psi = programStageServiceInstance.getProgramStageInstance( "ZwwuwNp6gVd" );
        programStageServiceInstance.deleteProgramStageInstance( psi );

        trackerBundleParams.setImportStrategy( TrackerImportStrategy.UPDATE );
        trackerBundle = trackerBundleService.create( trackerBundleParams );
        report = trackerValidationService.validate( trackerBundle );

        printReport( report );

        assertEquals( 1, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1082 ) ) ) );

        // All should be removed
        assertEquals( 0, trackerBundle.getEnrollments().size() );
    }

    // TODO: Can't provoke this state
    // see comments in:
    // org/hisp/dhis/tracker/validation/hooks/PreCheckDataRelationsValidationHook.java:212
    @Test
    @Ignore( "Can't provoke this state" )
    public void testIsRegButNoTei()
        throws IOException
    {
        TrackerImportParams trackerBundleParams = renderService
            .fromJson(
                new ClassPathResource( "tracker/validations/events_error-not-enrolled.json" ).getInputStream(),
                TrackerImportParams.class );

        User user = userService.getUser( "M5zQapPyTZI" );
        trackerBundleParams.setUserId( user.getUid() );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerBundleParams );
        assertEquals( 1, trackerBundle.getEvents().size() );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );
        printReport( report );
        assertEquals( 1, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1037 ) ) ) );

        // All should be removed
        assertEquals( 0, trackerBundle.getEnrollments().size() );
    }

    @Test
    public void testValidateAndAddNotesToEvent()
        throws IOException
    {
        Date now = new Date();

        // When

        ValidateAndCommitTestUnit createAndUpdate = createEvent( "tracker/validations/events-with-notes-data.json" );

        // Then

        // Fetch the UID of the newly created event
        final ProgramStageInstance programStageInstance = getEventFromReport( createAndUpdate );

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
        final ValidateAndCommitTestUnit createAndUpdate = createEvent(
            "tracker/validations/events-with-notes-update-data.json" );

        // Then
        final ProgramStageInstance programStageInstance = getEventFromReport( createAndUpdate );

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

        // Then
        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( trackerBundleParams, importStrategy );

        assertEquals( 0, createAndUpdate.getTrackerBundle().getEvents().size() );
        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );
        assertEquals( 1, report.getErrorReports().size() );
        assertThat( report.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1082 ) ) ) );
    }

    @Test
    @SneakyThrows
    public void testImportFailsOnOuNotMatchingProgramOu()
    {
        TrackerImportParams trackerBundleParams = createBundleFromJson(
            "tracker/validations/events-with-ou-not-in-program.json" );
        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( trackerBundleParams, CREATE_AND_UPDATE );
        assertEquals( 0, createAndUpdate.getTrackerBundle().getEvents().size() );
        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );
        assertEquals( 1, report.getErrorReports().size() );
        assertThat( report.getErrorReports(), everyItem( hasProperty( "errorCode", equalTo( E1029 ) ) ) );
    }

    private ValidateAndCommitTestUnit createEvent( String jsonPayload )
        throws IOException
    {
        // Given
        TrackerImportParams trackerBundleParams = createBundleFromJson( jsonPayload );

        // When
        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( trackerBundleParams, CREATE_AND_UPDATE );

        // Then
        assertEquals( 1, createAndUpdate.getTrackerBundle().getEvents().size() );
        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );
        assertEquals( TrackerStatus.OK, createAndUpdate.getCommitReport().getStatus() );
        assertEquals( 0, report.getErrorReports().size() );

        return createAndUpdate;
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

    private ProgramStageInstance getEventFromReport( ValidateAndCommitTestUnit createAndUpdate )
    {
        final Map<TrackerType, TrackerTypeReport> typeReportMap = createAndUpdate.getCommitReport().getTypeReportMap();
        String newEvent = typeReportMap.get( TrackerType.EVENT ).getObjectReportMap().get( 0 ).getUid();
        return programStageServiceInstance.getProgramStageInstance( newEvent );
    }
}
