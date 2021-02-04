package org.hisp.dhis.tracker.validation;

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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.core.Every.everyItem;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.bundle.TrackerBundleService;
import org.hisp.dhis.tracker.report.TrackerBundleReport;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerStatus;
import org.hisp.dhis.tracker.report.TrackerValidationReport;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class EnrollmentAttrValidationTests
    extends AbstractImportValidationTest
{
    @Autowired
    protected TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    private TrackerBundleService trackerBundleService;

    @Autowired
    private DefaultTrackerValidationService trackerValidationService;

    @Autowired
    private TrackedEntityAttributeService trackedEntityAttributeService;

    @Override
    protected void initTest()
        throws IOException
    {
        setUpMetadata( "tracker/tracker_basic_metadata_mandatory_attr.json" );

        TrackerImportParams trackerBundleParams = fromJson( "tracker/validations/enrollments_te_te-data_2.json" );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerBundleParams );
        assertEquals( 1, trackerBundle.getTrackedEntities().size() );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );
        assertEquals( 0, report.getErrorReports().size() );

        TrackerBundleReport bundleReport = trackerBundleService.commit( trackerBundle );
        assertEquals( TrackerStatus.OK, bundleReport.getStatus() );
    }

    @Test
    public void testAttributesMissingUid()
        throws IOException
    {
        TrackerImportParams params = createBundleFromJson( "tracker/validations/enrollments_te_attr-missing-uuid.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( params, TrackerImportStrategy.CREATE );

        TrackerValidationReport validationReport = createAndUpdate.getValidationReport();
        printReport( validationReport );

        assertEquals( 1, validationReport.getErrorReports().size() );

        assertThat( validationReport.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1075 ) ) ) );
    }

    @Test
    public void testAttributesMissingValues()
        throws IOException
    {
        TrackerImportParams params = createBundleFromJson(
            "tracker/validations/enrollments_te_attr-missing-value.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( params, TrackerImportStrategy.CREATE );

        TrackerValidationReport validationReport = createAndUpdate.getValidationReport();
        printReport( validationReport );

        assertEquals( 1, validationReport.getErrorReports().size() );

        assertThat( validationReport.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1076 ) ) ) );
    }

    //TODO: Fails with: (need to figure out how to force deletion here first)
    // * ERROR 22:47:50,353 Failed to invoke method deleteTrackedEntityAttribute on DeletionHandler 'ProgramDeletionHandler' (DefaultDeletionManager.java [main])
    @Test
    @Ignore( "Delete not impl." )
    public void testAttributesMissingTeA()
        throws IOException
    {
        TrackedEntityAttribute sTJvSLN7Kcb = trackedEntityAttributeService.getTrackedEntityAttribute( "sTJvSLN7Kcb" );
        trackedEntityAttributeService.deleteTrackedEntityAttribute( sTJvSLN7Kcb );

        TrackerImportParams params = createBundleFromJson(
            "tracker/validations/enrollments_te_attr-data.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( params, TrackerImportStrategy.CREATE );
        assertEquals( 1, createAndUpdate.getTrackerBundle().getEnrollments().size() );

        TrackerValidationReport validationReport = createAndUpdate.getValidationReport();
        printReport( validationReport );

        assertEquals( 1, validationReport.getErrorReports().size() );

        assertThat( validationReport.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1017 ) ) ) );
    }

    @Test
    public void testAttributesMissingMandatory()
        throws IOException
    {
        TrackerImportParams params = createBundleFromJson(
            "tracker/validations/enrollments_te_attr-missing-mandatory.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( params, TrackerImportStrategy.CREATE );

        TrackerValidationReport validationReport = createAndUpdate.getValidationReport();
        printReport( validationReport );

        assertEquals( 1, validationReport.getErrorReports().size() );

        assertThat( validationReport.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1018 ) ) ) );
    }

    @Test
    public void testAttributesOnlyProgramAttrAllowed()
        throws IOException
    {
        TrackerImportParams params = createBundleFromJson(
            "tracker/validations/enrollments_te_attr-only-program-attr.json" );

        ValidateAndCommitTestUnit createAndUpdate = validateAndCommit( params, TrackerImportStrategy.CREATE );

        TrackerValidationReport validationReport = createAndUpdate.getValidationReport();
        printReport( validationReport );

        assertEquals( 1, validationReport.getErrorReports().size() );

        assertThat( validationReport.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1019 ) ) ) );
    }
}