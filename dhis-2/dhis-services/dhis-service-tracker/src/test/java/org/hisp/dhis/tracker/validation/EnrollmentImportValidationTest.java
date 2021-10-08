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
import static org.hamcrest.core.Every.everyItem;
import static org.hisp.dhis.tracker.TrackerImportStrategy.CREATE_AND_UPDATE;
import static org.hisp.dhis.tracker.TrackerImportStrategy.UPDATE;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import lombok.SneakyThrows;

import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerImportService;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.bundle.TrackerBundleService;
import org.hisp.dhis.tracker.report.*;
import org.hisp.dhis.user.User;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class EnrollmentImportValidationTest
    extends AbstractImportValidationTest
{
    @Autowired
    protected ProgramInstanceService programInstanceService;

    @Autowired
    private TrackerBundleService trackerBundleService;

    @Autowired
    private TrackerImportService trackerImportService;

    @Override
    protected void initTest()
        throws IOException
    {

        setUpMetadata( "tracker/tracker_basic_metadata.json" );

        TrackerImportParams trackerBundleParams = createBundleFromJson(
            "tracker/validations/enrollments_te_te-data.json" );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerBundleParams );
        assertEquals( 5, trackerBundle.getTrackedEntities().size() );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );
        assertEquals( 0, report.getErrorReports().size() );

        TrackerBundleReport bundleReport = trackerBundleService.commit( trackerBundle );
        assertEquals( TrackerStatus.OK, bundleReport.getStatus() );
    }

    @Test
    public void testEnrollmentValidationOkAll()
        throws IOException
    {
        TrackerImportParams params = createBundleFromJson(
            "tracker/validations/enrollments_te_enrollments-data.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( params, TrackerImportStrategy.CREATE );

        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );
        assertEquals( 0, report.getErrorReports().size() );

        assertEquals( TrackerStatus.OK, createAndUpdate.getCommitReport().getStatus() );
    }

    @Test
    public void tesValidationInvalidUid()
        throws IOException
    {
        TrackerImportParams params = createBundleFromJson(
            "tracker/validations/enrollments_te_invalid-uid.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( params, TrackerImportStrategy.CREATE );

        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );
        assertEquals( 1, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1048 ) ) ) );
    }

    @Test
    public void testDatesMissing()
        throws IOException
    {
        TrackerImportParams params = createBundleFromJson(
            "tracker/validations/enrollments_error-dates-missing.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( params, TrackerImportStrategy.CREATE );

        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );
        assertEquals( 2, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1025 ) ) ) );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1023 ) ) ) );
    }

    @Test
    public void testDatesInFuture()
        throws IOException
    {
        TrackerImportParams params = createBundleFromJson(
            "tracker/validations/enrollments_error-dates-future.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( params, TrackerImportStrategy.CREATE );

        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );
        assertEquals( 2, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1020 ) ) ) );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1021 ) ) ) );
    }

    @Test( expected = IOException.class )
    public void testDisplayIncidentDateTrueButDateValueIsInvalid()
        throws IOException
    {
        createBundleFromJson( "tracker/validations/enrollments_error-displayIncident.json" );
    }

    @Test
    public void testDisplayIncidentDateTrueButDateValueNotPresent()
        throws IOException
    {
        TrackerImportParams params = createBundleFromJson(
            "tracker/validations/enrollments_error-displayIncident-missing-occurredAt.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( params, TrackerImportStrategy.CREATE );

        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );
        assertEquals( 1, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1023 ) ) ) );
    }

    @Test
    public void testMissingProgram()
        throws IOException
    {
        TrackerImportParams params = createBundleFromJson(
            "tracker/validations/enrollments_error-program-missing.json" );

        User user = userService.getUser( ADMIN_USER_UID );
        params.setUserId( user.getUid() );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( params, TrackerImportStrategy.CREATE );

        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );
        assertEquals( 1, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1069 ) ) ) );
    }

    @Test
    public void testMissingOrgUnit()
        throws IOException
    {
        TrackerImportParams params = createBundleFromJson(
            "tracker/validations/enrollments_error-orgunit-missing.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( params, TrackerImportStrategy.CREATE );

        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );
        assertEquals( 1, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1070 ) ) ) );
    }

    @Test
    public void testNoWriteAccessToOrg()
        throws IOException
    {
        TrackerImportParams params = createBundleFromJson(
            "tracker/validations/enrollments_te_enrollments-data.json" );

        User user = userService.getUser( USER_2 );
        params.setUser( user );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( params, TrackerImportStrategy.CREATE );

        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );
        assertEquals( 4, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1000 ) ) ) );
    }

    @Test
    public void testEnrollmentCreateAlreadyExists()
        throws IOException
    {
        TrackerImportParams params = createBundleFromJson( "tracker/validations/enrollments_te_enrollments-data.json" );

        TrackerBundle trackerBundle = trackerBundleService.create( params );
        assertEquals( 4, trackerBundle.getEnrollments().size() );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );
        assertEquals( 0, report.getErrorReports().size() );

        trackerBundleService.commit( trackerBundle );

        report = trackerValidationService.validate( trackerBundle );
        printReport( report );

        assertEquals( 4, report.getErrorReports().size() );
        assertThat( report.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1080 ) ) ) );

        // All should be removed
        assertEquals( 0, trackerBundle.getEnrollments().size() );

        printReport( report );
    }

    @Test
    public void testUpdateNotExists()
        throws IOException
    {
        TrackerImportParams params = createBundleFromJson(
            "tracker/validations/enrollments_te_enrollments-data.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( params, TrackerImportStrategy.UPDATE );

        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );
        assertEquals( 4, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1081 ) ) ) );
    }

    @Test
    public void testDeleteNotExists()
        throws IOException
    {
        TrackerImportParams params = createBundleFromJson(
            "tracker/validations/enrollments_te_enrollments-data.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( params, TrackerImportStrategy.DELETE );

        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );
        assertEquals( 4, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1081 ) ) ) );
    }

    @SneakyThrows
    private void testDeletedEnrollmentFails( TrackerImportStrategy importStrategy )
    {

        // Given -> Creates an enrollment
        createEnrollment( "tracker/validations/enrollments_te_enrollments-data-soft_deleted_test.json" );

        // When -> Soft-delete the enrollment
        programInstanceService.deleteProgramInstance( programInstanceService.getProgramInstance( "wMNWZ6hnuhS" ) );

        TrackerImportParams trackerBundleParams = createBundleFromJson(
            "tracker/validations/enrollments_te_enrollments-data-soft_deleted_test.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( trackerBundleParams, importStrategy );

        assertEquals( 0, createAndUpdate.getTrackerBundle().getEnrollments().size() );
        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );
        assertEquals( 1, report.getErrorReports().size() );
        assertThat( report.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1113 ) ) ) );
    }

    @Test
    public void testUpdateDeletedEnrollmentFails()
    {
        testDeletedEnrollmentFails( UPDATE );
    }

    @Test
    public void testInserDeletedEnrollmentFails()
    {
        testDeletedEnrollmentFails( CREATE_AND_UPDATE );
    }

    @SneakyThrows
    private void createEnrollment( String jsonPayload )
    {
        // Given
        TrackerImportParams trackerBundleParams = createBundleFromJson( jsonPayload );

        // When
        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( trackerBundleParams, CREATE_AND_UPDATE );

        // Then
        assertEquals( 1, createAndUpdate.getTrackerBundle().getEnrollments().size() );
        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );
        assertEquals( TrackerStatus.OK, createAndUpdate.getCommitReport().getStatus() );
        assertEquals( 0, report.getErrorReports().size() );
    }

    @Test
    public void testNonRegProgram()
        throws IOException
    {
        TrackerImportParams params = createBundleFromJson(
            "tracker/validations/enrollments_error-nonreg-program.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( params, TrackerImportStrategy.CREATE );

        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );
        assertEquals( 1, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1014 ) ) ) );
    }

    @Test
    public void testNonExistTe()
        throws IOException
    {
        TrackerImportParams params = createBundleFromJson(
            "tracker/validations/enrollments_error-nonexist-te.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( params, TrackerImportStrategy.CREATE );

        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );
        assertEquals( 1, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1068 ) ) ) );
    }

    @Test
    public void testTrackedEntityTypeMismatch()
        throws IOException
    {
        TrackerImportParams params = createBundleFromJson(
            "tracker/validations/enrollments_error-program-tet-mismatch-te.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( params, TrackerImportStrategy.CREATE );

        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );
        assertEquals( 1, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1022 ) ) ) );
    }

    @Test
    public void testOnlyProgramAttributesAllowedOnEnrollments()
        throws IOException
    {
        TrackerImportParams params = createBundleFromJson(
            "tracker/validations/enrollments_error_non_program_attr.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( params, TrackerImportStrategy.CREATE );

        TrackerValidationReport validationReport = createAndUpdate.getValidationReport();
        printReport( validationReport );

        assertEquals( 3, validationReport.getErrorReports().size() );

        assertThat( validationReport.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1019 ) ) ) );
    }

    @Test
    public void testAttributesOk()
        throws IOException
    {
        TrackerImportParams params = createBundleFromJson( "tracker/validations/enrollments_te_attr-data.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( params, TrackerImportStrategy.CREATE );
        assertEquals( 1, createAndUpdate.getTrackerBundle().getEnrollments().size() );

        TrackerValidationReport validationReport = createAndUpdate.getValidationReport();
        printReport( validationReport );

        assertEquals( 0, validationReport.getErrorReports().size() );

        assertThat( validationReport.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1019 ) ) ) );
    }

    @Test
    public void testDeleteCascadeProgramInstances()
        throws IOException
    {
        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit(
            "tracker/validations/enrollments_te_attr-data.json",
            TrackerImportStrategy.CREATE );
        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );
        assertEquals( 0, report.getErrorReports().size() );
        assertEquals( TrackerStatus.OK, createAndUpdate.getCommitReport().getStatus() );

        importProgramStageInstances();

        TrackerImportParams params = renderService
            .fromJson( new ClassPathResource( "tracker/validations/enrollments_te_attr-data.json" ).getInputStream(),
                TrackerImportParams.class );

        User user2 = userService.getUser( USER_4 );
        params.setUser( user2 );
        params.setImportStrategy( TrackerImportStrategy.DELETE );

        TrackerBundle trackerBundle = trackerBundleService.create( params );
        assertEquals( 1, trackerBundle.getEnrollments().size() );

        report = trackerValidationService.validate( trackerBundle );
        printReport( report );
        assertEquals( 2, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1103 ) ) ) );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1091 ) ) ) );
    }

    protected void importProgramStageInstances()
        throws IOException
    {
        TrackerImportParams params = createBundleFromJson( "tracker/validations/events-data.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( params, TrackerImportStrategy.CREATE );
        TrackerValidationReport report = createAndUpdate.getValidationReport();
        printReport( report );
        assertEquals( 0, report.getErrorReports().size() );
        assertEquals( TrackerStatus.OK, createAndUpdate.getCommitReport().getStatus() );
    }

    // TODO: Empty json geo obj OR (missing field in obj) causes strange json
    // mapping exception, should we capture this?
    // com.fasterxml.jackson.databind.JsonMappingException: (was
    // java.lang.NullPointerException) (through reference chain:
    // org.hisp.dhis.tracker.bundle.TrackerBundleParams["enrollments"]->java.util.ArrayList[0]->org.hisp.dhis.tracker.domain.Enrollment["geometry"])
    @Test
    @Ignore( "Validation not possible yet exception surface before this validation" )
    public void testBadGeoOnEnrollment()
        throws IOException
    {
        TrackerImportParams params = createBundleFromJson(
            "tracker/validations/enrollments_bad-geo.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( params, TrackerImportStrategy.CREATE );
        assertEquals( 1, createAndUpdate.getTrackerBundle().getEnrollments().size() );

        TrackerValidationReport validationReport = createAndUpdate.getValidationReport();
        printReport( validationReport );

        assertEquals( 0, validationReport.getErrorReports().size() );

        assertThat( validationReport.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1019 ) ) ) );
    }

    /*
     * FAILS ERROR 00:26:29,461 Value too long for column
     * "GEOMETRY BINARY(255)":
     * "X'aced000573720021636f6d2e7669766964736f6c7574696f6e732e6a74732e67656f6d2e506f696e7444077bad161cbb2a0200014c000b636f6f7264696e61... (1168)"
     * ; SQL statement: insert into programinstance (uid, created, lastUpdated,
     * createdAtClient, lastUpdatedAtClient, incidentDate, enrollmentdate,
     * enddate, followup, completedBy, geometry, deleted, storedby, status,
     * trackedentityinstanceid, programid, organisationunitid,
     * programinstanceid) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
     * ?, ?, ?) [22001-196] (SqlExceptionHelper.java [main])
     */
    @Test
    public void testBadGeoOnEnrollmentMissingFeatureType()
        throws IOException
    {
        TrackerImportParams params = createBundleFromJson(
            "tracker/validations/enrollments_bad-geo-missing-featuretype.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( params, TrackerImportStrategy.CREATE );

        TrackerValidationReport validationReport = createAndUpdate.getValidationReport();
        printReport( validationReport );

        assertEquals( 1, validationReport.getErrorReports().size() );

        assertThat( validationReport.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1074 ) ) ) );
    }

    @Test
    public void testBadGeoOnEnrollmentMissingTypeOnGeo()
        throws IOException
    {
        TrackerImportParams params = createBundleFromJson(
            "tracker/validations/enrollments_bad-geo-missing-geotype.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( params, TrackerImportStrategy.CREATE );

        TrackerValidationReport validationReport = createAndUpdate.getValidationReport();
        printReport( validationReport );

        assertEquals( 1, validationReport.getErrorReports().size() );

        assertThat( validationReport.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1012 ) ) ) );
    }

    @Test
    public void testActiveEnrollmentAlreadyExists()
        throws IOException
    {
        TrackerImportParams trackerImportParams = createBundleFromJson(
            "tracker/validations/enrollments_double-tei-enrollment_part1.json" );

        TrackerImportReport trackerImportReport = trackerImportService.importTracker( trackerImportParams );

        TrackerValidationReport validationReport = trackerImportReport.getValidationReport();

        assertEquals( 0, validationReport.getErrorReports().size() );

        TrackerImportParams trackerImportParams1 = createBundleFromJson(
            "tracker/validations/enrollments_double-tei-enrollment_part2.json" );

        trackerImportReport = trackerImportService.importTracker( trackerImportParams1 );

        validationReport = trackerImportReport.getValidationReport();

        assertEquals( 1, validationReport.getErrorReports().size() );

        assertThat( validationReport.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1015 ) ) ) );
    }

    // TODO: E1093 can't reproduce
    @Test
    @Ignore( "Not possible to provoke, maybe be removed in the new importer" )
    public void testEnrollmentNoAccessToCheck()
        throws IOException
    {
        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit(
            "tracker/validations/enrollments_no-access-check_part1.json", TrackerImportStrategy.CREATE );
        assertEquals( 0, createAndUpdate.getTrackerBundle().getEnrollments().size() );
        TrackerValidationReport validationReport = createAndUpdate.getValidationReport();
        printReport( validationReport );

        assertEquals( 0, validationReport.getErrorReports().size() );

        createAndUpdate = validateAndCommit(
            "tracker/validations/enrollments_no-access-check_part2.json", TrackerImportStrategy.CREATE );
        assertEquals( 1, createAndUpdate.getTrackerBundle().getEnrollments().size() );

        validationReport = createAndUpdate.getValidationReport();
        printReport( validationReport );

        assertEquals( 1, validationReport.getErrorReports().size() );

        assertThat( validationReport.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1093 ) ) ) );
    }

    /**
     * Notes with no value are ignored
     *
     */
    @Test
    public void testBadEnrollmentNoteNoValue()
        throws IOException
    {
        TrackerImportParams params = createBundleFromJson(
            "tracker/validations/enrollments_bad-note-no-value.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( params, TrackerImportStrategy.CREATE );
        assertEquals( 1, createAndUpdate.getTrackerBundle().getEnrollments().size() );

        TrackerValidationReport validationReport = createAndUpdate.getValidationReport();
        printReport( validationReport );

        assertEquals( 0, validationReport.getErrorReports().size() );
    }

    @Test
    public void testBadEnrollmentNoteBadUUID()
        throws IOException
    {
        TrackerImportParams params = createBundleFromJson(
            "tracker/validations/enrollments_bad-note-bad-uuid.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( params, TrackerImportStrategy.CREATE );
        assertEquals( 0, createAndUpdate.getTrackerBundle().getEnrollments().size() );

        TrackerValidationReport validationReport = createAndUpdate.getValidationReport();
        printReport( validationReport );

        assertEquals( 1, validationReport.getErrorReports().size() );

        assertThat( validationReport.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1048 ) ) ) );
    }

    @Test
    public void testBadEnrollmentNoteUUIDExist()
        throws IOException
    {
        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit(
            "tracker/validations/enrollments_bad-note-uuid-exists-part1.json", TrackerImportStrategy.CREATE );
        assertEquals( 1, createAndUpdate.getTrackerBundle().getEnrollments().size() );
        assertEquals( TrackerStatus.OK, createAndUpdate.getCommitReport().getStatus() );

        createAndUpdate = validateAndCommit(
            "tracker/validations/enrollments_bad-note-uuid-exists-part2.json", TrackerImportStrategy.CREATE );

        TrackerValidationReport validationReport = createAndUpdate.getValidationReport();
        printReport( validationReport );

        assertEquals( 1, validationReport.getWarningReports().size() );
        assertThat( validationReport.getWarningReports(),
            everyItem( hasProperty( "warningCode", equalTo( TrackerErrorCode.E1119 ) ) ) );
    }
}
