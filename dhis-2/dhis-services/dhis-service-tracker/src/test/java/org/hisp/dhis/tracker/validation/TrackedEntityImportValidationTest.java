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

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleParams;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleService;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleValidationService;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleCommitReport;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleValidationReport;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.bundle.TrackerBundleParams;
import org.hisp.dhis.tracker.bundle.TrackerBundleService;
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
public class TrackedEntityImportValidationTest
    extends AbstractImportValidationTest
{
    @Autowired
    protected TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    private TrackerBundleService trackerBundleService;

    @Autowired
    private ObjectBundleService objectBundleService;

    @Autowired
    private ObjectBundleValidationService objectBundleValidationService;

    @Autowired
    private DefaultTrackerValidationService trackerValidationService;

    @Autowired
    private RenderService _renderService;

    @Autowired
    private UserService _userService;

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
        List<ErrorReport> errorReports1 = commit.getErrorReports();
        assertTrue( errorReports1.isEmpty() );

    }

    @Test
    public void testTeValidationOkGenerateId()
        throws IOException
    {
        TrackerBundleParams params = createBundleFromJson(
            "tracker/validations/te-data_ok_no_uuids.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( params, TrackerImportStrategy.CREATE );
        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );
        assertEquals( 0, report.getErrorReports().size() );
        assertEquals( TrackerStatus.OK, createAndUpdate.getCommitReport().getStatus() );
    }

    @Test
    public void testValidateInvalidUid()
        throws IOException
    {
        TrackerBundleParams params = createBundleFromJson(
            "tracker/validations/te-data_invalid_uids.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( params, TrackerImportStrategy.CREATE );
        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );
        assertEquals( 1, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1048 ) ) ) );
    }

    @Test
    public void testTeValidationOkAll()
        throws IOException
    {
        TrackerBundleParams params = createBundleFromJson( "tracker/validations/te-data_ok.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( params, TrackerImportStrategy.CREATE );
        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );
        assertEquals( 0, report.getErrorReports().size() );
        assertEquals( TrackerStatus.OK, createAndUpdate.getCommitReport().getStatus() );
    }

    @Test
    public void testNoWriteAccessFailFast()
        throws IOException
    {
        TrackerBundleParams params = createBundleFromJson( "tracker/validations/te-data_ok.json" );

        User user = userService.getUser( USER_2 );
        params.setUser( user );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( params, TrackerImportStrategy.CREATE );
        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );
        assertEquals( 13, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1000 ) ) ) );

        printReport( report );
    }

    @Test
    public void testNoWriteAccessToOrg()
        throws IOException
    {
        TrackerBundleParams params = createBundleFromJson( "tracker/validations/te-data_ok.json" );

        User user = userService.getUser( USER_2 );
        params.setUser( user );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( params, TrackerImportStrategy.CREATE );
        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );
        assertEquals( 13, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1000 ) ) ) );

        printReport( report );
    }

    @Test
    public void testNoWriteAccessInAcl()
        throws IOException
    {
        TrackerBundleParams params = createBundleFromJson( "tracker/validations/te-data_ok.json" );

        User user = userService.getUser( USER_1 );
        params.setUser( user );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( params, TrackerImportStrategy.CREATE );
        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );
        assertEquals( 13, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1001 ) ) ) );

        printReport( report );
    }

    @Test
    public void testWriteAccessInAclViaUserGroup()
        throws IOException
    {
        TrackerBundleParams params = createBundleFromJson( "tracker/validations/te-data_ok.json" );

        User user = userService.getUser( USER_3 );
        params.setUser( user );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( params, TrackerImportStrategy.CREATE );
        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );
        assertEquals( 0, report.getErrorReports().size() );
        assertEquals( TrackerStatus.OK, createAndUpdate.getCommitReport().getStatus() );
    }

    @Test
    public void testNonExistingTeType()
        throws IOException
    {
        TrackerBundleParams params = createBundleFromJson(
            "tracker/validations/te-data_error_teType-non-existing.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( params, TrackerImportStrategy.CREATE );
        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );
        assertEquals( 1, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1005 ) ) ) );

        printReport( report );
    }

    @Test
    public void testNoOrgUnit()
        throws IOException
    {
        TrackerBundleParams params = createBundleFromJson(
            "tracker/validations/te-data_error_orgunit-null.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( params, TrackerImportStrategy.CREATE );
        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );
        assertEquals( 1, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1049 ) ) ) );

        printReport( report );
    }

    @Test
    public void testNonExistingOrgUnit()
        throws IOException
    {
        TrackerBundleParams params = createBundleFromJson(
            "tracker/validations/te-data_error_orgunit-non-existing.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( params, TrackerImportStrategy.CREATE );
        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );
        assertEquals( 1, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1049 ) ) ) );
    }

    @Test
    public void testGeoOk()
        throws IOException
    {
        TrackerBundleParams params = createBundleFromJson(
            "tracker/validations/te-data_error_geo-ok.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( params, TrackerImportStrategy.CREATE );
        TrackerValidationReport report = createAndUpdate.getValidationReport();

        printReport( report );
        assertEquals( 0, report.getErrorReports().size() );
    }

    @Test
    public void testTeCreateAlreadyExists()
        throws IOException
    {
        TrackerBundleParams trackerBundleParams = createBundleFromJson( "tracker/validations/te-data_ok.json" );

        User user = userService.getUser( ADMIN_USER_UID );
        trackerBundleParams.setUser( user );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerBundleParams ).get( 0 );
        assertEquals( 13, trackerBundle.getTrackedEntities().size() );

        // Validate first time, should contain no errors.
        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );
        assertEquals( 0, report.getErrorReports().size() );

        // Commit the validated bundle...
        trackerBundleService.commit( trackerBundle );

        // Re-validate, should now contain 13 errors...
        report = trackerValidationService.validate( trackerBundle );
        assertEquals( 13, report.getErrorReports().size() );
        assertThat( report.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1002 ) ) ) );

        // Tracker should now have 13 teis removed from the collection, since they all failed.
        assertEquals( 0, trackerBundle.getTrackedEntities().size() );

        printReport( report );
    }

    @Test
    public void testTeAttrNonExistentAttr()
        throws IOException
    {
        TrackerBundleParams params = createBundleFromJson(
            "tracker/validations/te-data_error_attr-non-existing.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( params, TrackerImportStrategy.CREATE );
        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );
        assertEquals( 2, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1006 ) ) ) );

        printReport( report );
    }

    @Test
    public void testUpdate()
        throws IOException
    {
        TrackerBundleParams trackerBundleParams = createBundleFromJson( "tracker/validations/te-data_ok.json" );

        User user = userService.getUser( ADMIN_USER_UID );
        trackerBundleParams.setUser( user );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerBundleParams ).get( 0 );
        assertEquals( 13, trackerBundle.getTrackedEntities().size() );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );
        assertEquals( 0, report.getErrorReports().size() );

        TrackerBundleReport bundleReport = trackerBundleService.commit( trackerBundle );
        assertEquals( TrackerStatus.OK, bundleReport.getStatus() );

        trackerBundleParams.setImportStrategy( TrackerImportStrategy.UPDATE );
        TrackerBundle updateBundle = trackerBundleService.create( trackerBundleParams ).get( 0 );

        report = trackerValidationService.validate( updateBundle );
        TrackerBundleReport bundleReport2 = trackerBundleService.commit( updateBundle );
        assertEquals( 0, report.getErrorReports().size() );
        assertEquals( TrackerStatus.OK, bundleReport2.getStatus() );

        TrackerBundleParams trackerBundleParamsUpdate = createBundleFromJson( "tracker/validations/te-data.json" );

        trackerBundleParamsUpdate.setUser( user );

        trackerBundleParamsUpdate.setImportStrategy( TrackerImportStrategy.UPDATE );
        TrackerBundle trackerBundleUpdate = trackerBundleService.create( trackerBundleParamsUpdate ).get( 0 );
        assertEquals( 1, trackerBundleUpdate.getTrackedEntities().size() );

        TrackerValidationReport reportUpdate = trackerValidationService.validate( trackerBundleUpdate );
        assertEquals( 0, reportUpdate.getErrorReports().size() );

        TrackerBundleReport bundleReportUpdate = trackerBundleService.commit( trackerBundleUpdate );
        assertEquals( TrackerStatus.OK, bundleReportUpdate.getStatus() );

        // isInactive should now be true
        TrackedEntityInstance nCc1rCEOKaY = trackedEntityInstanceService.getTrackedEntityInstance( "NCc1rCEOKaY" );
        assertEquals( true, nCc1rCEOKaY.isInactive() );
    }

    @Test
    public void testDelete()
        throws IOException
    {
        TrackerBundleParams trackerBundleParams = createBundleFromJson( "tracker/validations/te-data_ok.json" );

        User user = userService.getUser( ADMIN_USER_UID );
        trackerBundleParams.setUser( user );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerBundleParams ).get( 0 );
        assertEquals( 13, trackerBundle.getTrackedEntities().size() );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );
        assertEquals( 0, report.getErrorReports().size() );

        TrackerBundleReport bundleReport = trackerBundleService.commit( trackerBundle );
        assertEquals( TrackerStatus.OK, bundleReport.getStatus() );

        TrackerBundleParams trackerBundleParamsUpdate = createBundleFromJson( "tracker/validations/te-data.json" );

        trackerBundleParamsUpdate.setUser( user );

        trackerBundleParamsUpdate.setImportStrategy( TrackerImportStrategy.DELETE );
        TrackerBundle trackerBundleUpdate = trackerBundleService.create( trackerBundleParamsUpdate ).get( 0 );
        assertEquals( 1, trackerBundleUpdate.getTrackedEntities().size() );

        TrackerValidationReport reportUpdate = trackerValidationService.validate( trackerBundleUpdate );
        printReport( reportUpdate );

        assertEquals( 0, reportUpdate.getErrorReports().size() );

        TrackerBundleReport bundleReportUpdate = trackerBundleService.commit( trackerBundleUpdate );
        assertEquals( TrackerStatus.OK, bundleReportUpdate.getStatus() );

        // isInactive should now be true
        TrackedEntityInstance nCc1rCEOKaY = trackedEntityInstanceService.getTrackedEntityInstance( "NCc1rCEOKaY" );
        assertEquals( true, nCc1rCEOKaY.isInactive() );
        //TODO: NOT WORKING... yet? should it not be deleted?
//        assertEquals( true, nCc1rCEOKaY.isDeleted() );
    }

    @Test
    public void testUpdateNotExists()
        throws IOException
    {
        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( "tracker/validations/te-data_ok.json",
            TrackerImportStrategy.DELETE );
        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );
        assertEquals( 13, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1063 ) ) ) );
    }

    @Test
    public void testDeleteNotExists()
        throws IOException
    {
        TrackerBundleParams params = createBundleFromJson( "tracker/validations/te-data_ok.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( params, TrackerImportStrategy.DELETE );
        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );
        assertEquals( 13, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1063 ) ) ) );
    }

    @Test
    public void testDeleteCascadeProgramInstances()
        throws IOException
    {
        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit(
            "tracker/validations/enrollments_te_te-data.json",
            TrackerImportStrategy.CREATE );
        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );
        assertEquals( 0, report.getErrorReports().size() );
        assertEquals( TrackerStatus.OK, createAndUpdate.getCommitReport().getStatus() );

        importProgramInstances();

        TrackerBundleParams params = renderService
            .fromJson( new ClassPathResource( "tracker/validations/enrollments_te_te-data.json" ).getInputStream(),
                TrackerBundleParams.class );

        User user2 = userService.getUser( USER_4 );
        params.setUser( user2 );

        params.setImportStrategy( TrackerImportStrategy.DELETE );
        TrackerBundle trackerBundle = trackerBundleService.create( params ).get( 0 );
        assertEquals( 4, trackerBundle.getTrackedEntities().size() );

        report = trackerValidationService.validate( trackerBundle );
        printReport( report );
        assertEquals( 2, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1100 ) ) ) );
    }

    protected void importProgramInstances()
        throws IOException
    {
        TrackerBundleParams params = createBundleFromJson(
            "tracker/validations/enrollments_te_enrollments-data.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( params, TrackerImportStrategy.CREATE );
        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );
        assertEquals( 0, report.getErrorReports().size() );
        assertEquals( TrackerStatus.OK, createAndUpdate.getCommitReport().getStatus() );
    }
}
