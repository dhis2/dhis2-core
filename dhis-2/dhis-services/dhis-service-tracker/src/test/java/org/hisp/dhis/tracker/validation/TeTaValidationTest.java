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
 *
 */

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Every.everyItem;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.hisp.dhis.config.H2DhisConfigurationProvider;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.encryption.EncryptionStatus;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.bundle.TrackerBundleService;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerValidationReport;
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
    private TrackerBundleService trackerBundleService;

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

        validateAndCommit( "tracker/validations/te-program_with_tea_fileresource_data.json" );

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

        validateAndCommit( "tracker/validations/te-program_with_tea_fileresource_data.json" );

        List<TrackedEntityInstance> trackedEntityInstances = manager.getAll( TrackedEntityInstance.class );
        assertEquals( 1, trackedEntityInstances.size() );
        TrackedEntityInstance trackedEntityInstance = trackedEntityInstances.get( 0 );

        List<TrackedEntityAttributeValue> attributeValues = trackedEntityAttributeValueService
            .getTrackedEntityAttributeValues(
                trackedEntityInstance );

        assertEquals( 1, attributeValues.size() );

        fileResource = fileResourceService.getFileResource( fileResource.getUid() );
        assertTrue( fileResource.isAssigned() );

        TrackerValidationReport report = validate( "tracker/validations/te-program_with_tea_fileresource_data.json" );

        assertEquals( 1, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1009 ) ) ) );
    }

    @Test
    public void testNoFileRef()
        throws IOException
    {
        setUpMetadata( "tracker/validations/te-program_with_tea_fileresource_metadata.json" );

        TrackerImportParams trackerImportParams = fromJson(
            "tracker/validations/te-program_with_tea_fileresource_data.json", userService.getUser( ADMIN_USER_UID ) );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerImportParams );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );
        assertEquals( 1, report.getErrorReports().size() );
        assertThat( report.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1084 ) ) ) );

        trackerBundleService.commit( trackerBundle );

        List<TrackedEntityInstance> trackedEntityInstances = manager.getAll( TrackedEntityInstance.class );
        assertEquals( 0, trackedEntityInstances.size() );
    }

    @Test
    public void testGeneratedValuePatternDoNotMatch()
        throws IOException
    {
        setUpMetadata( "tracker/validations/te-program_with_tea_fileresource_metadata.json" );

        TrackerImportParams trackerImportParams = fromJson(
            "tracker/validations/te-program_with_tea_generated_data.json", userService.getUser( ADMIN_USER_UID ) );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerImportParams );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );
        assertEquals( 1, report.getErrorReports().size() );
        printReport( report );
        assertThat( report.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1008 ) ) ) );

        trackerBundleService.commit( trackerBundle );

        List<TrackedEntityInstance> trackedEntityInstances = manager.getAll( TrackedEntityInstance.class );
        assertEquals( 0, trackedEntityInstances.size() );
    }

    @Test
    public void testTeaMaxTextValueLength()
        throws IOException
    {
        setUpMetadata( "tracker/validations/te-program_with_tea_fileresource_metadata.json" );

        TrackerValidationReport report = validate( "tracker/validations/te-program_with_tea_too_long_text_value.json" );

        assertEquals( 1, report.getErrorReports().size() );

        printReport( report );

        assertThat( report.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1077 ) ) ) );
    }

    @Test
    public void testEncryptedAttrFail()
        throws IOException
    {
        setUpMetadata( "tracker/validations/te-program_with_tea_encryption_metadata.json" );

        TrackerImportParams trackerImportParams = fromJson(
            "tracker/validations/te-program_with_tea_encryption_data.json", userService.getUser( ADMIN_USER_UID ) );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerImportParams );

        H2DhisConfigurationProvider dhisConfigurationProvider = (H2DhisConfigurationProvider) this.dhisConfigurationProvider;
        dhisConfigurationProvider.setEncryptionStatus( EncryptionStatus.MISSING_ENCRYPTION_PASSWORD );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );

        assertEquals( 1, report.getErrorReports().size() );
        printReport( report );

        assertThat( report.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1112 ) ) ) );
    }

    @Test
    public void testUniqueFailInOrgUnit()
        throws IOException
    {
        setUpMetadata( "tracker/validations/te-program_with_tea_encryption_metadata.json" );

        TrackerImportParams trackerImportParams = fromJson(
            "tracker/validations/te-program_with_tea_unique_data_in_country.json",
            userService.getUser( ADMIN_USER_UID ) );

        trackerBundleService.commit( trackerBundleService.create( trackerImportParams ) );

        TrackerValidationReport report = validate(
            "tracker/validations/te-program_with_tea_unique_data_in_country.json",
            TrackerImportStrategy.CREATE_AND_UPDATE );

        assertEquals( 0, report.getErrorReports().size() );
        printReport( report );

        report = validate( "tracker/validations/te-program_with_tea_unique_data_in_region.json" );
        assertEquals( 0, report.getErrorReports().size() );
        printReport( report );
    }

    @Test
    public void testUniqueFail()
        throws IOException
    {
        setUpMetadata( "tracker/validations/te-program_with_tea_encryption_metadata.json" );

        TrackerImportParams trackerImportParams = fromJson(
            "tracker/validations/te-program_with_tea_unique_data.json",
            userService.getUser( ADMIN_USER_UID ) );
        TrackerBundle trackerBundle = trackerBundleService.create( trackerImportParams );

        trackerBundleService.commit( trackerBundle );

        TrackerValidationReport report = validate( "tracker/validations/te-program_with_tea_unique_data.json" );

        assertEquals( 1, report.getErrorReports().size() );

        printReport( report );

        assertThat( report.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1064 ) ) ) );
    }

    @Test
    public void testTeaInvalidFormat()
        throws IOException
    {
        setUpMetadata( "tracker/validations/te-program_with_tea_fileresource_metadata.json" );

        TrackerValidationReport report = validate(
            "tracker/validations/te-program_with_tea_invalid_format_value.json" );

        assertEquals( 1, report.getErrorReports().size() );

        printReport( report );

        assertThat( report.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1085 ) ) ) );
    }

    @Test
    public void testTeaInvalidImage()
        throws IOException
    {
        setUpMetadata( "tracker/validations/te-program_with_tea_fileresource_metadata.json" );

        TrackerValidationReport report = validate(
            "tracker/validations/te-program_with_tea_invalid_image_value.json" );

        assertEquals( 2, report.getErrorReports().size() );
        printReport( report );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1085 ) ) ) );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1007 ) ) ) );
    }

    @Test
    public void testTeaIsNull()
        throws IOException
    {
        setUpMetadata( "tracker/validations/te-program_with_tea_fileresource_metadata.json" );

        TrackerValidationReport report = validate(
            "tracker/validations/te-program_with_tea_invalid_value_isnull.json" );

        assertEquals( 1, report.getErrorReports().size() );

        printReport( report );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1076 ) ) ) );
    }

    private TrackerValidationReport validate( String path )
        throws IOException
    {
        TrackerImportParams trackerImportParams = fromJson(
            path,
            userService.getUser( ADMIN_USER_UID ) );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerImportParams );

        return trackerValidationService.validate( trackerBundle );
    }

    private TrackerValidationReport validate( String path, TrackerImportStrategy importStrategy )
        throws IOException
    {
        TrackerImportParams trackerImportParams = fromJson(
            path,
            userService.getUser( ADMIN_USER_UID ) );
        trackerImportParams.setImportStrategy( importStrategy );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerImportParams );

        return trackerValidationService.validate( trackerBundle );
    }

    private void validateAndCommit( String path )
        throws IOException
    {
        TrackerImportParams trackerImportParams = fromJson(
            path,
            userService.getUser( ADMIN_USER_UID ) );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerImportParams );
        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );

        assertEquals( 0, report.getErrorReports().size() );

        trackerBundleService.commit( trackerBundleService.create( trackerImportParams ) );
    }
}