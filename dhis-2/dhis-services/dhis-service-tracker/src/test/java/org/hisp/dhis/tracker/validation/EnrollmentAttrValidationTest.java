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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.core.Every.everyItem;
import static org.hisp.dhis.tracker.Assertions.assertNoImportErrors;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerImportService;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerImportReport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class EnrollmentAttrValidationTest extends TrackerTest
{
    @Autowired
    protected TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    private TrackerImportService trackerImportService;

    @Override
    protected void initTest()
        throws IOException
    {
        setUpMetadata( "tracker/tracker_basic_metadata_mandatory_attr.json" );
        injectAdminUser();
        TrackerImportParams trackerBundleParams = fromJson( "tracker/validations/enrollments_te_te-data_2.json" );
        TrackerImportReport trackerImportReport = trackerImportService.importTracker( trackerBundleParams );
        assertNoImportErrors( trackerImportReport );
        manager.flush();
    }

    @Test
    void failValidationWhenTrackedEntityAttributeHasWrongOptionValue()
        throws IOException
    {
        TrackerImportParams params = fromJson(
            "tracker/validations/enrollments_te_with_invalid_option_value.json" );
        TrackerImportReport trackerImportReport = trackerImportService.importTracker( params );
        assertEquals( 1, trackerImportReport.getValidationReport().getErrors().size() );
        assertThat( trackerImportReport.getValidationReport().getErrors(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1125 ) ) ) );
    }

    @Test
    void successValidationWhenTrackedEntityAttributeHasValidOptionValue()
        throws IOException
    {
        TrackerImportParams params = fromJson(
            "tracker/validations/enrollments_te_with_valid_option_value.json" );
        TrackerImportReport trackerImportReport = trackerImportService.importTracker( params );
        assertEquals( 0, trackerImportReport.getValidationReport().getErrors().size() );
    }

    @Test
    void testAttributesMissingUid()
        throws IOException
    {
        TrackerImportParams params = fromJson(
            "tracker/validations/enrollments_te_attr-missing-uuid.json" );

        TrackerImportReport trackerImportReport = trackerImportService.importTracker( params );

        assertEquals( 1, trackerImportReport.getValidationReport().getErrors().size() );
        assertThat( trackerImportReport.getValidationReport().getErrors(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1075 ) ) ) );
    }

    @Test
    void testAttributesMissingValues()
        throws IOException
    {
        TrackerImportParams params = fromJson(
            "tracker/validations/enrollments_te_attr-missing-value.json" );
        TrackerImportReport trackerImportReport = trackerImportService.importTracker( params );
        assertEquals( 1, trackerImportReport.getValidationReport().getErrors().size() );
        assertThat( trackerImportReport.getValidationReport().getErrors(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1076 ) ) ) );
    }

    @Test
    void testAttributesMissingTeA()
        throws IOException
    {
        TrackerImportParams params = fromJson( "tracker/validations/enrollments_te_attr-non-existing.json" );
        TrackerImportReport trackerImportReport = trackerImportService.importTracker( params );
        assertEquals( 1, trackerImportReport.getValidationReport().getErrors().size() );
        assertThat( trackerImportReport.getValidationReport().getErrors(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1006 ) ) ) );
    }

    @Test
    void testAttributesMissingMandatory()
        throws IOException
    {
        TrackerImportParams params = fromJson(
            "tracker/validations/enrollments_te_attr-missing-mandatory.json" );
        TrackerImportReport trackerImportReport = trackerImportService.importTracker( params );
        assertEquals( 1, trackerImportReport.getValidationReport().getErrors().size() );
        assertThat( trackerImportReport.getValidationReport().getErrors(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1018 ) ) ) );
    }

    @Test
    void testAttributesUniquenessInSameTei()
        throws IOException
    {
        TrackerImportParams params = fromJson(
            "tracker/validations/enrollments_te_unique_attr_same_tei.json" );
        TrackerImportReport trackerImportReport = trackerImportService.importTracker( params );
        assertEquals( 0, trackerImportReport.getValidationReport().getErrors().size() );
    }

    @Test
    void testAttributesUniquenessAlreadyInDB()
        throws IOException
    {
        TrackerImportParams params = fromJson( "tracker/validations/enrollments_te_te-data_3.json" );
        TrackerImportReport trackerImportReport = trackerImportService.importTracker( params );
        assertNoImportErrors( trackerImportReport );
        manager.flush();
        manager.clear();
        params = fromJson( "tracker/validations/enrollments_te_unique_attr_same_tei.json" );
        trackerImportReport = trackerImportService.importTracker( params );
        assertEquals( 0, trackerImportReport.getValidationReport().getErrors().size() );
        manager.flush();
        manager.clear();
        params = fromJson( "tracker/validations/enrollments_te_unique_attr_in_db.json" );
        trackerImportReport = trackerImportService.importTracker( params );
        assertEquals( 1, trackerImportReport.getValidationReport().getErrors().size() );
        assertThat( trackerImportReport.getValidationReport().getErrors(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1064 ) ) ) );
    }

    @Test
    void testAttributesUniquenessInDifferentTeis()
        throws IOException
    {
        TrackerImportParams params = fromJson( "tracker/validations/enrollments_te_te-data_3.json" );
        TrackerImportReport trackerImportReport = trackerImportService.importTracker( params );
        assertNoImportErrors( trackerImportReport );
        manager.flush();
        manager.clear();
        params = fromJson( "tracker/validations/enrollments_te_unique_attr.json" );

        trackerImportReport = trackerImportService.importTracker( params );

        assertEquals( 2, trackerImportReport.getValidationReport().getErrors().size() );
        assertThat( trackerImportReport.getValidationReport().getErrors(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1064 ) ) ) );
    }

    @Test
    void testAttributesOnlyProgramAttrAllowed()
        throws IOException
    {
        TrackerImportParams params = fromJson(
            "tracker/validations/enrollments_te_attr-only-program-attr.json" );
        TrackerImportReport trackerImportReport = trackerImportService.importTracker( params );
        assertEquals( 1, trackerImportReport.getValidationReport().getErrors().size() );
        assertThat( trackerImportReport.getValidationReport().getErrors(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1019 ) ) ) );
    }
}
