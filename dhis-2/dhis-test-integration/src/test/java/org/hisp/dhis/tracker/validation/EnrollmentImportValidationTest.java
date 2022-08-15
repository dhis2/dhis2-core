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
package org.hisp.dhis.tracker.validation;

import static org.hisp.dhis.tracker.Assertions.assertHasErrors;
import static org.hisp.dhis.tracker.Assertions.assertHasOnlyErrors;
import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.hisp.dhis.tracker.validation.Users.USER_2;
import static org.hisp.dhis.tracker.validation.Users.USER_4;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerImportService;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.preheat.TrackerPreheatService;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerImportReport;
import org.hisp.dhis.tracker.report.TrackerValidationReport;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class EnrollmentImportValidationTest extends TrackerTest
{

    @Autowired
    protected ProgramInstanceService programInstanceService;

    @Autowired
    private TrackerImportService trackerImportService;

    @Autowired
    private TrackerPreheatService trackerPreheatService;

    @Override
    protected void initTest()
        throws IOException
    {
        setUpMetadata( "tracker/tracker_basic_metadata.json" );
        injectAdminUser();
        assertNoErrors(
            trackerImportService.importTracker( fromJson( "tracker/validations/enrollments_te_te-data.json" ) ) );
        manager.flush();
    }

    @Test
    void testEnrollmentValidationOkAll()
        throws IOException
    {
        TrackerImportParams params = fromJson( "tracker/validations/enrollments_te_enrollments-data.json" );
        TrackerImportReport trackerImportReport = trackerImportService.importTracker( params );
        assertNoErrors( trackerImportReport );
    }

    @Test
    void testPreheatOwnershipForSubsequentEnrollment()
        throws IOException
    {
        TrackerImportParams params = fromJson( "tracker/validations/enrollments_te_enrollments-data.json" );
        TrackerImportReport trackerImportReport = trackerImportService.importTracker( params );
        assertNoErrors( trackerImportReport );
        TrackerImportParams secondParams = fromJson(
            "tracker/validations/enrollments_te_enrollments-data.json" );
        TrackerPreheat preheat = trackerPreheatService.preheat( secondParams );
        secondParams.getEnrollments().forEach( e -> {
            assertTrue( e.getOrgUnit().isEqualTo(
                preheat.getProgramOwner().get( e.getTrackedEntity() )
                    .get( e.getProgram().getIdentifier() )
                    .getOrganisationUnit() ) );
        } );
    }

    @Test
    void testDisplayIncidentDateTrueButDateValueIsInvalid()
    {
        assertThrows( IOException.class,
            () -> fromJson( "tracker/validations/enrollments_error-displayIncident.json" ) );
    }

    @Test
    void testNoWriteAccessToOrg()
        throws IOException
    {
        TrackerImportParams params = fromJson( "tracker/validations/enrollments_te_enrollments-data.json" );
        User user = userService.getUser( USER_2 );
        params.setUser( user );

        TrackerImportReport trackerImportReport = trackerImportService.importTracker( params );

        assertHasErrors( trackerImportReport, 4, TrackerErrorCode.E1000 );
    }

    @Test
    void testOnlyProgramAttributesAllowedOnEnrollments()
        throws IOException
    {
        TrackerImportParams params = fromJson( "tracker/validations/enrollments_error_non_program_attr.json" );

        TrackerImportReport trackerImportReport = trackerImportService.importTracker( params );

        assertHasErrors( trackerImportReport, 3, TrackerErrorCode.E1019 );
    }

    @Test
    void testAttributesOk()
        throws IOException
    {
        TrackerImportParams params = fromJson( "tracker/validations/enrollments_te_attr-data.json" );

        TrackerImportReport trackerImportReport = trackerImportService.importTracker( params );

        assertNoErrors( trackerImportReport );
        assertEquals( 1, trackerImportReport.getBundleReport().getTypeReportMap().get( TrackerType.ENROLLMENT )
            .getObjectReportMap().values().size() );
    }

    @Test
    void testDeleteCascadeProgramInstances()
        throws IOException
    {

        TrackerImportParams params = fromJson( "tracker/validations/enrollments_te_attr-data.json" );
        TrackerImportReport trackerImportReport = trackerImportService.importTracker( params );

        assertNoErrors( trackerImportReport );
        manager.flush();
        importProgramStageInstances();
        manager.flush();
        params = fromJson( "tracker/validations/enrollments_te_attr-data.json" );
        User user2 = userService.getUser( USER_4 );
        params.setUser( user2 );
        params.setImportStrategy( TrackerImportStrategy.DELETE );

        TrackerImportReport trackerImportDeleteReport = trackerImportService.importTracker( params );

        assertHasOnlyErrors( trackerImportDeleteReport, TrackerErrorCode.E1103, TrackerErrorCode.E1091 );
    }

    protected void importProgramStageInstances()
        throws IOException
    {
        TrackerImportParams params = fromJson( "tracker/validations/events-with-registration.json" );
        params.setImportStrategy( TrackerImportStrategy.CREATE );

        TrackerImportReport trackerImportReport = trackerImportService.importTracker( params );

        assertNoErrors( trackerImportReport );
    }

    @Test
    void testActiveEnrollmentAlreadyExists()
        throws IOException
    {
        TrackerImportParams trackerImportParams = fromJson(
            "tracker/validations/enrollments_double-tei-enrollment_part1.json" );

        TrackerImportReport trackerImportReport = trackerImportService.importTracker( trackerImportParams );

        assertNoErrors( trackerImportReport );

        TrackerImportParams trackerImportParams1 = fromJson(
            "tracker/validations/enrollments_double-tei-enrollment_part2.json" );

        trackerImportReport = trackerImportService.importTracker( trackerImportParams1 );

        TrackerValidationReport validationReport = trackerImportReport.getValidationReport();

        assertHasOnlyErrors( validationReport, TrackerErrorCode.E1015 );
    }

    @Test
    void testEnrollmentDeleteOk()
        throws IOException
    {
        TrackerImportParams paramsCreate = fromJson(
            "tracker/validations/enrollments_te_enrollments-data.json" );
        TrackerImportReport trackerImportReport = trackerImportService.importTracker( paramsCreate );
        assertNoErrors( trackerImportReport );

        manager.flush();
        manager.clear();

        TrackerImportParams paramsDelete = fromJson(
            "tracker/validations/enrollments_te_enrollments-data-delete.json" );
        paramsDelete.setImportStrategy( TrackerImportStrategy.DELETE );

        TrackerImportReport trackerImportReportDelete = trackerImportService.importTracker( paramsDelete );

        assertNoErrors( trackerImportReportDelete );
        assertEquals( 1, trackerImportReportDelete.getStats().getDeleted() );
    }

    /**
     * Notes with no value are ignored
     */
    @Test
    void testBadEnrollmentNoteNoValue()
        throws IOException
    {
        TrackerImportParams params = fromJson( "tracker/validations/enrollments_bad-note-no-value.json" );

        TrackerImportReport trackerImportReport = trackerImportService.importTracker( params );

        assertNoErrors( trackerImportReport );
    }
}
