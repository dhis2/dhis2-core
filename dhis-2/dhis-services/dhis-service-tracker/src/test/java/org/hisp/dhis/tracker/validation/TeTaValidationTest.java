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
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.config.H2DhisConfigurationProvider;
import org.hisp.dhis.encryption.EncryptionStatus;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerImportService;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerImportReport;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class TeTaValidationTest
    extends AbstractImportValidationTest
{
    @Autowired
    private DhisConfigurationProvider dhisConfigurationProvider;

    @Autowired
    private TrackerImportService trackerImportService;

    @Autowired
    private TrackedEntityAttributeValueService trackedEntityAttributeValueService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private FileResourceService fileResourceService;

    @Test
    public void testTrackedEntityProgramAttributeFileResourceValue()
        throws IOException
    {
        setUpMetadata( "tracker/validations/te-program_with_tea_fileresource_metadata.json" );

        FileResource fileResource = new FileResource( "test.pdf", "application/pdf",
            0, "d41d8cd98f00b204e9800998ecf8427e", FileResourceDomain.DOCUMENT );
        fileResource.setUid( "Jzf6hHNP7jx" );
        File file = File.createTempFile( "file-resource", "test" );

        fileResourceService.saveFileResource( fileResource, file );
        assertFalse( fileResource.isAssigned() );

        TrackerImportParams trackerImportParams = createBundleFromJson(
            "tracker/validations/te-program_with_tea_fileresource_data.json" );
        trackerImportService.importTracker( trackerImportParams );

        List<TrackedEntityInstance> trackedEntityInstances = manager.getAll( TrackedEntityInstance.class );
        assertEquals( 1, trackedEntityInstances.size() );

        TrackedEntityInstance trackedEntityInstance = trackedEntityInstances.get( 0 );

        List<TrackedEntityAttributeValue> attributeValues = trackedEntityAttributeValueService
            .getTrackedEntityAttributeValues(
                trackedEntityInstance );

        assertEquals( 1, attributeValues.size() );

        fileResource = fileResourceService.getFileResource( fileResource.getUid() );
        assertTrue( fileResource.isAssigned() );
    }

    @Test
    public void testFileAlreadyAssign()
        throws IOException
    {
        setUpMetadata( "tracker/validations/te-program_with_tea_fileresource_metadata.json" );

        FileResource fileResource = new FileResource( "test.pdf", "application/pdf",
            0, "d41d8cd98f00b204e9800998ecf8427e", FileResourceDomain.DOCUMENT );
        fileResource.setUid( "Jzf6hHNP7jx" );
        File file = File.createTempFile( "file-resource", "test" );

        fileResourceService.saveFileResource( fileResource, file );
        assertFalse( fileResource.isAssigned() );
        TrackerImportParams trackerImportParams = createBundleFromJson(
            "tracker/validations/te-program_with_tea_fileresource_data.json" );
        trackerImportService.importTracker( trackerImportParams );

        List<TrackedEntityInstance> trackedEntityInstances = manager.getAll( TrackedEntityInstance.class );
        assertEquals( 1, trackedEntityInstances.size() );
        TrackedEntityInstance trackedEntityInstance = trackedEntityInstances.get( 0 );

        List<TrackedEntityAttributeValue> attributeValues = trackedEntityAttributeValueService
            .getTrackedEntityAttributeValues(
                trackedEntityInstance );

        assertEquals( 1, attributeValues.size() );

        fileResource = fileResourceService.getFileResource( fileResource.getUid() );
        assertTrue( fileResource.isAssigned() );

        trackerImportParams = createBundleFromJson(
            "tracker/validations/te-program_with_tea_fileresource_data2.json" );
        TrackerImportReport trackerImportReport = trackerImportService.importTracker( trackerImportParams );

        assertEquals( 1, trackerImportReport.getValidationReport().getErrorReports().size() );

        assertThat( trackerImportReport.getValidationReport().getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1009 ) ) ) );
    }

    @Test
    public void testNoFileRef()
        throws IOException
    {
        setUpMetadata( "tracker/validations/te-program_with_tea_fileresource_metadata.json" );

        TrackerImportParams trackerImportParams = createBundleFromJson(
            "tracker/validations/te-program_with_tea_fileresource_data.json" );

        TrackerImportReport trackerImportReport = trackerImportService.importTracker( trackerImportParams );
        assertEquals( 1, trackerImportReport.getValidationReport().getErrorReports().size() );
        assertThat( trackerImportReport.getValidationReport().getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1084 ) ) ) );

        List<TrackedEntityInstance> trackedEntityInstances = manager.getAll( TrackedEntityInstance.class );
        assertEquals( 0, trackedEntityInstances.size() );
    }

    @Test
    public void testTeaMaxTextValueLength()
        throws IOException
    {
        setUpMetadata( "tracker/validations/te-program_with_tea_fileresource_metadata.json" );
        TrackerImportParams trackerImportParams = createBundleFromJson(
            "tracker/validations/te-program_with_tea_too_long_text_value.json" );

        TrackerImportReport trackerImportReport = trackerImportService.importTracker( trackerImportParams );

        assertEquals( 1, trackerImportReport.getValidationReport().getErrorReports().size() );

        assertThat( trackerImportReport.getValidationReport().getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1077 ) ) ) );
    }

    @Test
    public void testEncryptedAttrFail()
        throws IOException
    {
        setUpMetadata( "tracker/validations/te-program_with_tea_encryption_metadata.json" );

        TrackerImportParams trackerImportParams = createBundleFromJson(
            "tracker/validations/te-program_with_tea_encryption_data.json" );

        H2DhisConfigurationProvider dhisConfigurationProvider = (H2DhisConfigurationProvider) this.dhisConfigurationProvider;
        dhisConfigurationProvider.setEncryptionStatus( EncryptionStatus.MISSING_ENCRYPTION_PASSWORD );

        TrackerImportReport trackerImportReport = trackerImportService.importTracker( trackerImportParams );

        assertEquals( 1, trackerImportReport.getValidationReport().getErrorReports().size() );

        assertThat( trackerImportReport.getValidationReport().getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1112 ) ) ) );
    }

    @Test
    public void testUniqueFailInOrgUnit()
        throws IOException
    {
        setUpMetadata( "tracker/validations/te-program_with_tea_encryption_metadata.json" );

        TrackerImportParams trackerImportParams = createBundleFromJson(
            "tracker/validations/te-program_with_tea_unique_data_in_country.json" );

        trackerImportService.importTracker( trackerImportParams );

        trackerImportParams = createBundleFromJson(
            "tracker/validations/te-program_with_tea_unique_data_in_country.json" );
        trackerImportParams.setImportStrategy( TrackerImportStrategy.CREATE_AND_UPDATE );

        TrackerImportReport trackerImportReport = trackerImportService.importTracker( trackerImportParams );

        assertEquals( 0, trackerImportReport.getValidationReport().getErrorReports().size() );

        trackerImportParams = createBundleFromJson(
            "tracker/validations/te-program_with_tea_unique_data_in_region.json" );

        trackerImportReport = trackerImportService.importTracker( trackerImportParams );

        assertEquals( 0, trackerImportReport.getValidationReport().getErrorReports().size() );
    }

    @Test
    public void testUniqueFail()
        throws IOException
    {
        setUpMetadata( "tracker/validations/te-program_with_tea_encryption_metadata.json" );

        TrackerImportParams trackerImportParams = createBundleFromJson(
            "tracker/validations/te-program_with_tea_unique_data.json" );

        TrackerImportReport trackerImportReport = trackerImportService.importTracker( trackerImportParams );

        trackerImportParams = createBundleFromJson( "tracker/validations/te-program_with_tea_unique_data2.json" );

        trackerImportReport = trackerImportService.importTracker( trackerImportParams );

        assertEquals( 1, trackerImportReport.getValidationReport().getErrorReports().size() );

        assertThat( trackerImportReport.getValidationReport().getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1064 ) ) ) );
    }

    @Test
    public void testTeaInvalidFormat()
        throws IOException
    {
        setUpMetadata( "tracker/validations/te-program_with_tea_fileresource_metadata.json" );

        TrackerImportParams trackerImportParams = createBundleFromJson(
            "tracker/validations/te-program_with_tea_invalid_format_value.json" );

        TrackerImportReport trackerImportReport = trackerImportService.importTracker( trackerImportParams );

        assertEquals( 1, trackerImportReport.getValidationReport().getErrorReports().size() );

        assertThat( trackerImportReport.getValidationReport().getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1085 ) ) ) );
    }

    @Test
    public void testTeaInvalidImage()
        throws IOException
    {
        setUpMetadata( "tracker/validations/te-program_with_tea_fileresource_metadata.json" );

        TrackerImportParams trackerImportParams = createBundleFromJson(
            "tracker/validations/te-program_with_tea_invalid_image_value.json" );

        TrackerImportReport trackerImportReport = trackerImportService.importTracker( trackerImportParams );

        assertEquals( 2, trackerImportReport.getValidationReport().getErrorReports().size() );

        assertThat( trackerImportReport.getValidationReport().getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1085 ) ) ) );

        assertThat( trackerImportReport.getValidationReport().getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1007 ) ) ) );
    }

    @Test
    public void testTeaIsNull()
        throws IOException
    {
        setUpMetadata( "tracker/validations/te-program-with-tea-mandatory-image.json" );

        TrackerImportParams trackerImportParams = createBundleFromJson(
            "tracker/validations/te-program_with_tea_invalid_value_isnull.json" );

        TrackerImportReport trackerImportReport = trackerImportService.importTracker( trackerImportParams );

        assertEquals( 1, trackerImportReport.getValidationReport().getErrorReports().size() );

        assertThat( trackerImportReport.getValidationReport().getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1076 ) ) ) );
    }
}