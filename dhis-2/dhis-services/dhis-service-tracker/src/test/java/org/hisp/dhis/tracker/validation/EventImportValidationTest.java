package org.hisp.dhis.tracker.validation;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleParams;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleCommitReport;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleValidationReport;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.bundle.TrackerBundleParams;
import org.hisp.dhis.tracker.report.TrackerBundleReport;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerStatus;
import org.hisp.dhis.tracker.report.TrackerValidationReport;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.core.Every.everyItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
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
    private ProgramStageService programStageService;

    @Autowired
    private ProgramStageInstanceService programStageServiceInstance;

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

        TrackerBundleParams trackerBundleParams = createBundleFromJson(
            "tracker/validations/enrollments_te_te-data.json" );

        User user = userService.getUser( ADMIN_USER_UID );
        trackerBundleParams.setUser( user );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerBundleParams ).get( 0 );
        assertEquals( 3, trackerBundle.getTrackedEntities().size() );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );
        assertEquals( 0, report.getErrorReports().size() );

        TrackerBundleReport bundleReport = trackerBundleService.commit( trackerBundle );
        assertEquals( TrackerStatus.OK, bundleReport.getStatus() );

        ////////////////////////////////////////

        trackerBundleParams = renderService
            .fromJson(
                new ClassPathResource( "tracker/validations/enrollments_te_enrollments-data.json" ).getInputStream(),
                TrackerBundleParams.class );

        trackerBundleParams.setUser( user );

        trackerBundle = trackerBundleService.create( trackerBundleParams ).get( 0 );
        assertEquals( 2, trackerBundle.getEnrollments().size() );

        report = trackerValidationService.validate( trackerBundle );
        assertEquals( 0, report.getErrorReports().size() );

        bundleReport = trackerBundleService.commit( trackerBundle );
        assertEquals( TrackerStatus.OK, bundleReport.getStatus() );
    }


    @Test
    public void testEventValidationOkAll()
        throws IOException
    {
        TrackerBundleParams trackerBundleParams = createBundleFromJson( "tracker/validations/events-data.json" );

        User user = userService.getUser( ADMIN_USER_UID );
        trackerBundleParams.setUser( user );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerBundleParams ).get( 0 );
        assertEquals( 1, trackerBundle.getEvents().size() );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );
        printReport( report );

        assertEquals( 0, report.getErrorReports().size() );

        TrackerBundleReport bundleReport = trackerBundleService.commit( trackerBundle );
        assertEquals( TrackerStatus.OK, bundleReport.getStatus() );
    }


    @Test
    public void testEventMissingOrgUnit()
        throws IOException
    {
        TrackerBundleParams trackerBundleParams = createBundleFromJson(
            "tracker/validations/events_error-orgunit-missing.json" );

        User user = userService.getUser( ADMIN_USER_UID );
        trackerBundleParams.setUser( user );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerBundleParams ).get( 0 );
        assertEquals( 1, trackerBundle.getEvents().size() );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );
        printReport( report );

        assertEquals( 1, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1011 ) ) ) );
    }

    @Test
    public void testEventMissingProgramAndProgramStage()
        throws IOException
    {

        TrackerBundleParams trackerBundleParams = createBundleFromJson(
            "tracker/validations/events_error-program-pstage-missing.json" );

        User user = userService.getUser( ADMIN_USER_UID );
        trackerBundleParams.setUser( user );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerBundleParams ).get( 0 );
        assertEquals( 1, trackerBundle.getEvents().size() );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );
        printReport( report );

        assertEquals( 2, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1088 ) ) ) );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1035 ) ) ) );
    }

    @Test
    public void testEventMissingProgramStageProgramIsRegistration()
        throws IOException
    {

        TrackerBundleParams trackerBundleParams = createBundleFromJson(
            "tracker/validations/events_error-pstage-missing-isreg.json" );

        User user = userService.getUser( ADMIN_USER_UID );
        trackerBundleParams.setUser( user );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerBundleParams ).get( 0 );
        assertEquals( 1, trackerBundle.getEvents().size() );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );
        printReport( report );

        assertEquals( 2, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1086 ) ) ) );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1035 ) ) ) );
    }

    @Test
    public void testProgramStageProgramDifferentPrograms()
        throws IOException
    {

        TrackerBundleParams trackerBundleParams = createBundleFromJson(
            "tracker/validations/events_error-pstage-program-different.json" );

        User user = userService.getUser( ADMIN_USER_UID );
        trackerBundleParams.setUser( user );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerBundleParams ).get( 0 );
        assertEquals( 1, trackerBundle.getEvents().size() );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );
        printReport( report );

        assertEquals( 1, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1089 ) ) ) );
    }

    @Test
    public void testNoWriteAccessToOrg()
        throws IOException
    {

        TrackerBundleParams trackerBundleParams = createBundleFromJson( "tracker/validations/events-data.json" );

        User user = userService.getUser( USER_2 );
        trackerBundleParams.setUser( user );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerBundleParams ).get( 0 );
        assertEquals( 1, trackerBundle.getEvents().size() );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );
        printReport( report );

        assertEquals( 1, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1000 ) ) ) );
    }

    @Test
    public void testEventCreateAlreadyExists()
        throws IOException
    {

        TrackerBundleParams trackerBundleParams = createBundleFromJson( "tracker/validations/events-data.json" );

        User user = userService.getUser( ADMIN_USER_UID );
        trackerBundleParams.setUser( user );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerBundleParams ).get( 0 );
        assertEquals( 1, trackerBundle.getEvents().size() );

        // Validate first time, should contain no errors.
        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );
        printReport( report );
        assertEquals( 0, report.getErrorReports().size() );

        // Commit the validated bundle...
        trackerBundleService.commit( trackerBundle );

        trackerBundleParams.setImportStrategy( TrackerImportStrategy.CREATE );
        trackerBundle = trackerBundleService.create( trackerBundleParams ).get( 0 );
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

        TrackerBundleParams trackerBundleParams = createBundleFromJson( "tracker/validations/events-data.json" );

        User user = userService.getUser( ADMIN_USER_UID );
        trackerBundleParams.setUser( user );

        trackerBundleParams.setImportStrategy( TrackerImportStrategy.UPDATE );
        TrackerBundle trackerBundle = trackerBundleService.create( trackerBundleParams ).get( 0 );
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

        TrackerBundleParams trackerBundleParams = createBundleFromJson(
            "tracker/validations/events_error-missing-date.json" );

        User user = userService.getUser( ADMIN_USER_UID );
        trackerBundleParams.setUser( user );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerBundleParams ).get( 0 );
        assertEquals( 1, trackerBundle.getEvents().size() );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );
        printReport( report );

        assertEquals( 1, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1031 ) ) ) );
    }

    @Test
    public void testMissingTei()
        throws IOException
    {

        TrackerBundleParams trackerBundleParams = createBundleFromJson(
            "tracker/validations/events_error-missing-tei.json" );

        User user = userService.getUser( ADMIN_USER_UID );
        trackerBundleParams.setUser( user );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerBundleParams ).get( 0 );
        assertEquals( 1, trackerBundle.getEvents().size() );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );
        printReport( report );

        assertEquals( 1, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1036 ) ) ) );
    }

    @Test
    public void testMissingCompletedDate()
        throws IOException
    {
        TrackerBundleParams trackerBundleParams = createBundleFromJson(
            "tracker/validations/events_error-no-completed-date.json" );

        User user = userService.getUser( ADMIN_USER_UID );
        trackerBundleParams.setUser( user );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerBundleParams ).get( 0 );
        assertEquals( 1, trackerBundle.getEvents().size() );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );
        printReport( report );

        assertEquals( 1, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1042 ) ) ) );
    }

    @Test
    public void testNonDefaultCategoryCombo()
        throws IOException
    {
        TrackerBundleParams trackerBundleParams = createBundleFromJson(
            "tracker/validations/events_non-default-combo.json" );

        User user = userService.getUser( ADMIN_USER_UID );
        trackerBundleParams.setUser( user );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerBundleParams ).get( 0 );
        assertEquals( 1, trackerBundle.getEvents().size() );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );
        printReport( report );

        assertEquals( 1, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1055 ) ) ) );
    }



    @Test
    public void testWrongDatesInCatCombo()
        throws IOException
    {
        TrackerBundleParams trackerBundleParams = createBundleFromJson( "tracker/validations/events_combo-date-wrong.json" );

        User user = userService.getUser( ADMIN_USER_UID );
        trackerBundleParams.setUser( user );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerBundleParams ).get( 0 );
        assertEquals( 1, trackerBundle.getEvents().size() );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );
        printReport( report );

        assertEquals( 2, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1056 ) ) ) );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1057 ) ) ) );
    }

    //TODO: Delete not working yet
//    @Test
//    public void testEventAlreadyDeleted()
//        throws IOException
//    {
//
//
//        TrackerBundleParams params = createBundleFromJson( "tracker/validations/events-data.json" );
//
//        User user = userService.getUser( ADMIN_USER );
//        params.setUser( user );
//
//        ValidateAndCommit createAndUpdate = doValidateAndCommit( params,
//            TrackerImportStrategy.CREATE_AND_UPDATE );
//        assertEquals( 0, createAndUpdate.getValidationReport().getErrorReports().size() );
//
//        ValidateAndCommit delete = doValidateAndCommit( params,
//            TrackerImportStrategy.DELETE );
//        assertEquals( 0, delete.getValidationReport().getErrorReports().size() );
//
//        ValidateAndCommit deleteAgain = doValidateAndCommit( params,
//            TrackerImportStrategy.DELETE );
//        assertEquals( 1, deleteAgain.getValidationReport().getErrorReports().size() );
//
//        assertThat( deleteAgain.getValidationReport().getErrorReports(),
//            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1030 ) ) ) );
//
//        // All should be removed
//        assertEquals( 0, createAndUpdate.getTrackerBundle().getEnrollments().size() );
//    }

    // TODO: See comments on error codes, seems to not be in use....
//    @Test
//    public void testPeriodTypes()
//        throws IOException
//    {
//
//
//        TrackerBundleParams trackerBundleParams = createBundleFromJson( "tracker/validations/events_error-periodtype.json" );
//
//        User user = userService.getUser( ADMIN_USER );
//        trackerBundleParams.setUser( user );
//
//        TrackerBundle trackerBundle = trackerBundleService.create( trackerBundleParams ).get( 0 );
//        assertEquals( 1, trackerBundle.getEvents().size() );
//
//        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );
//        printErrors( report );
//
//        assertEquals( 1, report.getErrorReports().size() );
//
//        assertThat( report.getErrorReports(),
//            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1042 ) ) ) );
//    }

//    //TODO: Needs clarification, can't test this error: E1082.
//    // See comments in: org/hisp/dhis/tracker/validation/hooks/PreCheckDataRelationsValidationHook.java:165
//    @Test
//    public void testProgramStageDeleted()
//        throws IOException
//    {
//
//
//        TrackerBundleParams trackerBundleParams = renderService
//            .fromJson(
//                new ClassPathResource( "tracker/validations/events-data.json" ).getInputStream(),
//                TrackerBundleParams.class );
//
//        User user = userService.getUser( "M5zQapPyTZI" );
//        trackerBundleParams.setUser( user );
//
//        TrackerBundle trackerBundle = trackerBundleService.create( trackerBundleParams ).get( 0 );
//        assertEquals( 1, trackerBundle.getEvents().size() );
//
//        // Validate first time, should contain no errors.
//        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );
//        assertEquals( 0, report.getErrorReports().size() );
//
//        // Commit the validated bundle...
//        trackerBundleService.commit( trackerBundle );
//
//        ProgramStageInstance psi = programStageServiceInstance.getProgramStageInstance( "ZwwuwNp6gVd" );
////        psi.setDeleted( true );
////        programStageServiceInstance.updateProgramStageInstance( psi );
//        programStageServiceInstance.deleteProgramStageInstance( psi );
//
//        trackerBundleParams.setImportStrategy( TrackerImportStrategy.UPDATE );
//        trackerBundle = trackerBundleService.create( trackerBundleParams ).get( 0 );
//        report = trackerValidationService.validate( trackerBundle );
//        printErrors( report );
//
//        assertEquals( 1, report.getErrorReports().size() );
//
//        assertThat( report.getErrorReports(),
//            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1082 ) ) ) );
//
//        // All should be removed
//        assertEquals( 0, trackerBundle.getEnrollments().size() );
//    }

    //TODO: Can't provoke this state,
    // see comments in: org/hisp/dhis/tracker/validation/hooks/PreCheckDataRelationsValidationHook.java:212
//    @Test
//    public void testIsRegButNoTei()
//        throws IOException
//    {
//
//
//        TrackerBundleParams trackerBundleParams = renderService
//            .fromJson(
//                new ClassPathResource( "tracker/validations/events_error-not-enrolled.json" ).getInputStream(),
//                TrackerBundleParams.class );
//
//        User user = userService.getUser( "M5zQapPyTZI" );
//        trackerBundleParams.setUser( user );
//
//        TrackerBundle trackerBundle = trackerBundleService.create( trackerBundleParams ).get( 0 );
//        assertEquals( 1, trackerBundle.getEvents().size() );
//
//        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );
//        printErrors( report );
//        assertEquals( 1, report.getErrorReports().size() );
//
//        assertThat( report.getErrorReports(),
//            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1037 ) ) ) );
//
//        // All should be removed
//        assertEquals( 0, trackerBundle.getEnrollments().size() );
//    }

}
