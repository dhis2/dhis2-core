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

import org.hisp.dhis.H2DhisConfigurationProvider;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleParams;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleService;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleValidationService;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleValidationReport;
import org.hisp.dhis.encryption.EncryptionStatus;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.bundle.TrackerBundleParams;
import org.hisp.dhis.tracker.bundle.TrackerBundleService;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerValidationReport;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.core.Every.everyItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class TeTaValidationTest
    extends AbstractImportValidationTest
{
    @Autowired
    private DhisConfigurationProvider dhisConfigurationProvider;

    @Autowired
    private ObjectBundleService objectBundleService;

    @Autowired
    private ObjectBundleValidationService objectBundleValidationService;

    @Autowired
    private RenderService _renderService;

    @Autowired
    private UserService _userService;

    @Autowired
    private TrackerBundleService trackerBundleService;

    @Autowired
    private TrackedEntityAttributeValueService trackedEntityAttributeValueService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private FileResourceService fileResourceService;

    private void setupMetadata( String metaDataFile )
        throws IOException
    {
        renderService = _renderService;
        userService = _userService;

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( metaDataFile ).getInputStream(),
            RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validationReport = objectBundleValidationService.validate( bundle );
        List<ErrorReport> errorReports = validationReport.getErrorReports();
        assertTrue( errorReports.isEmpty() );

        objectBundleService.commit( bundle );
    }

    @Test
    public void testTrackedEntityProgramAttributeFileResourceValue()
        throws IOException
    {
        String metaDataFile = "tracker/validations/te-program_with_tea_fileresource_metadata.json";
        setupMetadata( metaDataFile );

        FileResource fileResource = new FileResource( "test.pdf", "application/pdf",
            0, "d41d8cd98f00b204e9800998ecf8427e", FileResourceDomain.DOCUMENT );
        fileResource.setUid( "Jzf6hHNP7jx" );
        File file = File.createTempFile( "file-resource", "test" );

        fileResourceService.saveFileResource( fileResource, file );
        assertFalse( fileResource.isAssigned() );

        TrackerBundle trackerBundle = renderService
            .fromJson( new ClassPathResource( "tracker/validations/te-program_with_tea_fileresource_data.json" )
                    .getInputStream(),
                TrackerBundleParams.class ).toTrackerBundle();

        User user = userService.getUser( ADMIN_USER_UID );

        TrackerBundleParams bundle = TrackerBundleParams.builder()
            .trackedEntities( trackerBundle.getTrackedEntities() )
            .enrollments( trackerBundle.getEnrollments() )
            .events( trackerBundle.getEvents() )
            .build();

        bundle.setUser( user );

        List<TrackerBundle> trackerBundles = trackerBundleService.create( bundle );

        assertEquals( 1, trackerBundles.size() );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundles.get( 0 ) );
        assertEquals( 0, report.getErrorReports().size() );

        trackerBundleService.commit( trackerBundles.get( 0 ) );

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
        String metaDataFile = "tracker/validations/te-program_with_tea_fileresource_metadata.json";
        setupMetadata( metaDataFile );

        FileResource fileResource = new FileResource( "test.pdf", "application/pdf",
            0, "d41d8cd98f00b204e9800998ecf8427e", FileResourceDomain.DOCUMENT );
        fileResource.setUid( "Jzf6hHNP7jx" );
        File file = File.createTempFile( "file-resource", "test" );

        fileResourceService.saveFileResource( fileResource, file );
        assertFalse( fileResource.isAssigned() );

        TrackerBundle trackerBundle = renderService
            .fromJson( new ClassPathResource( "tracker/validations/te-program_with_tea_fileresource_data.json" )
                    .getInputStream(),
                TrackerBundleParams.class ).toTrackerBundle();

        User user = userService.getUser( ADMIN_USER_UID );

        TrackerBundleParams bundle = TrackerBundleParams.builder()
            .trackedEntities( trackerBundle.getTrackedEntities() )
            .enrollments( trackerBundle.getEnrollments() )
            .events( trackerBundle.getEvents() )
            .build();

        bundle.setUser( user );

        List<TrackerBundle> trackerBundles = trackerBundleService.create( bundle );

        assertEquals( 1, trackerBundles.size() );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundles.get( 0 ) );
        assertEquals( 0, report.getErrorReports().size() );

        trackerBundleService.commit( trackerBundles.get( 0 ) );

        List<TrackedEntityInstance> trackedEntityInstances = manager.getAll( TrackedEntityInstance.class );
        assertEquals( 1, trackedEntityInstances.size() );
        TrackedEntityInstance trackedEntityInstance = trackedEntityInstances.get( 0 );

        List<TrackedEntityAttributeValue> attributeValues = trackedEntityAttributeValueService
            .getTrackedEntityAttributeValues(
                trackedEntityInstance );

        assertEquals( 1, attributeValues.size() );

        fileResource = fileResourceService.getFileResource( fileResource.getUid() );
        assertTrue( fileResource.isAssigned() );

        trackerBundle = renderService
            .fromJson( new ClassPathResource( "tracker/validations/te-program_with_tea_fileresource_data.json" )
                    .getInputStream(),
                TrackerBundleParams.class ).toTrackerBundle();

        bundle = TrackerBundleParams.builder()
            .trackedEntities( trackerBundle.getTrackedEntities() )
            .enrollments( trackerBundle.getEnrollments() )
            .events( trackerBundle.getEvents() )
            .build();

        bundle.setUser( user );

        trackerBundles = trackerBundleService.create( bundle );
        report = trackerValidationService.validate( trackerBundles.get( 0 ) );
        assertEquals( 1, report.getErrorReports().size() );

        assertThat( report.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1009 ) ) ) );
    }

    @Test
    public void testNoFileRef()
        throws IOException
    {
        String metaDataFile = "tracker/validations/te-program_with_tea_fileresource_metadata.json";
        setupMetadata( metaDataFile );

        TrackerBundle trackerBundle = renderService
            .fromJson( new ClassPathResource( "tracker/validations/te-program_with_tea_fileresource_data.json" )
                    .getInputStream(),
                TrackerBundleParams.class ).toTrackerBundle();

        User user = userService.getUser( ADMIN_USER_UID );

        TrackerBundleParams bundle = TrackerBundleParams.builder()
            .trackedEntities( trackerBundle.getTrackedEntities() )
            .enrollments( trackerBundle.getEnrollments() )
            .events( trackerBundle.getEvents() )
            .build();

        bundle.setUser( user );

        List<TrackerBundle> trackerBundles = trackerBundleService.create( bundle );

        assertEquals( 1, trackerBundles.size() );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundles.get( 0 ) );
        assertEquals( 1, report.getErrorReports().size() );
        assertThat( report.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1084 ) ) ) );

        trackerBundleService.commit( trackerBundles.get( 0 ) );

        List<TrackedEntityInstance> trackedEntityInstances = manager.getAll( TrackedEntityInstance.class );
        assertEquals( 1, trackedEntityInstances.size() );

        TrackedEntityInstance trackedEntityInstance = trackedEntityInstances.get( 0 );

        List<TrackedEntityAttributeValue> attributeValues = trackedEntityAttributeValueService
            .getTrackedEntityAttributeValues(
                trackedEntityInstance );

        assertEquals( 1, attributeValues.size() );
    }

    @Test
    public void testGeneratedValuePatternDoNotMatch()
        throws IOException
    {
        String metaDataFile = "tracker/validations/te-program_with_tea_fileresource_metadata.json";
        setupMetadata( metaDataFile );

        TrackerBundle trackerBundle = renderService
            .fromJson( new ClassPathResource( "tracker/validations/te-program_with_tea_generated_data.json" )
                    .getInputStream(),
                TrackerBundleParams.class ).toTrackerBundle();

        User user = userService.getUser( ADMIN_USER_UID );

        TrackerBundleParams bundle = TrackerBundleParams.builder()
            .trackedEntities( trackerBundle.getTrackedEntities() )
            .enrollments( trackerBundle.getEnrollments() )
            .events( trackerBundle.getEvents() )
            .build();

        bundle.setUser( user );

        List<TrackerBundle> trackerBundles = trackerBundleService.create( bundle );

        assertEquals( 1, trackerBundles.size() );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundles.get( 0 ) );
        assertEquals( 1, report.getErrorReports().size() );
        printReport( report );
        assertThat( report.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1008 ) ) ) );

        trackerBundleService.commit( trackerBundles.get( 0 ) );

        List<TrackedEntityInstance> trackedEntityInstances = manager.getAll( TrackedEntityInstance.class );
        assertEquals( 1, trackedEntityInstances.size() );

        TrackedEntityInstance trackedEntityInstance = trackedEntityInstances.get( 0 );

        List<TrackedEntityAttributeValue> attributeValues = trackedEntityAttributeValueService
            .getTrackedEntityAttributeValues(
                trackedEntityInstance );

        assertEquals( 1, attributeValues.size() );
    }

    @Test
    public void testTeaMaxTextValueLength()
        throws IOException
    {
        String metaDataFile = "tracker/validations/te-program_with_tea_fileresource_metadata.json";
        setupMetadata( metaDataFile );

        TrackerBundle trackerBundle = renderService
            .fromJson( new ClassPathResource( "tracker/validations/te-program_with_tea_too_long_text_value.json" )
                    .getInputStream(),
                TrackerBundleParams.class ).toTrackerBundle();

        User user = userService.getUser( ADMIN_USER_UID );

        TrackerBundleParams bundle = TrackerBundleParams.builder()
            .trackedEntities( trackerBundle.getTrackedEntities() )
            .enrollments( trackerBundle.getEnrollments() )
            .events( trackerBundle.getEvents() )
            .build();

        bundle.setUser( user );

        List<TrackerBundle> trackerBundles = trackerBundleService.create( bundle );

        assertEquals( 1, trackerBundles.size() );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundles.get( 0 ) );
        assertEquals( 1, report.getErrorReports().size() );
        printReport( report );
        assertThat( report.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1077 ) ) ) );
    }

    @Test
    public void testEncryptedAttrFail()
        throws IOException
    {
        String metaDataFile = "tracker/validations/te-program_with_tea_encryption_metadata.json";
        setupMetadata( metaDataFile );

        TrackerBundle trackerBundle = renderService
            .fromJson( new ClassPathResource( "tracker/validations/te-program_with_tea_encryption_data.json" )
                    .getInputStream(),
                TrackerBundleParams.class ).toTrackerBundle();

        User user = userService.getUser( ADMIN_USER_UID );

        TrackerBundleParams build = TrackerBundleParams.builder()
            .trackedEntities( trackerBundle.getTrackedEntities() )
            .enrollments( trackerBundle.getEnrollments() )
            .events( trackerBundle.getEvents() )
            .build();

        build.setUser( user );

        List<TrackerBundle> trackerBundles = trackerBundleService.create( build );

        assertEquals( 1, trackerBundles.size() );

        H2DhisConfigurationProvider dhisConfigurationProvider = (H2DhisConfigurationProvider) this.dhisConfigurationProvider;
        dhisConfigurationProvider.setEncryptionStatus( EncryptionStatus.MISSING_ENCRYPTION_PASSWORD );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundles.get( 0 ) );
        assertEquals( 1, report.getErrorReports().size() );
        printReport( report );
        assertThat( report.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1112 ) ) ) );
    }

    @Test
    public void testUniqueFail()
        throws IOException
    {
        String metaDataFile = "tracker/validations/te-program_with_tea_encryption_metadata.json";
        setupMetadata( metaDataFile );

        TrackerBundle trackerBundle = renderService
            .fromJson( new ClassPathResource( "tracker/validations/te-program_with_tea_unique_data.json" )
                    .getInputStream(),
                TrackerBundleParams.class ).toTrackerBundle();

        User user = userService.getUser( ADMIN_USER_UID );

        TrackerBundleParams bundle = TrackerBundleParams.builder()
            .trackedEntities( trackerBundle.getTrackedEntities() )
            .enrollments( trackerBundle.getEnrollments() )
            .events( trackerBundle.getEvents() )
            .build();

        bundle.setUser( user );

        List<TrackerBundle> trackerBundles = trackerBundleService.create( bundle );

        assertEquals( 1, trackerBundles.size() );

        trackerBundleService.commit( trackerBundles.get( 0 ) );

        trackerBundle = renderService
            .fromJson( new ClassPathResource( "tracker/validations/te-program_with_tea_unique_data.json" )
                    .getInputStream(),
                TrackerBundleParams.class ).toTrackerBundle();

        bundle = TrackerBundleParams.builder()
            .trackedEntities( trackerBundle.getTrackedEntities() )
            .enrollments( trackerBundle.getEnrollments() )
            .events( trackerBundle.getEvents() )
            .build();

        user = userService.getUser( ADMIN_USER_UID );

        bundle.setUser( user );

        trackerBundles = trackerBundleService.create( bundle );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundles.get( 0 ) );
        assertEquals( 1, report.getErrorReports().size() );
        printReport( report );
        assertThat( report.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1064 ) ) ) );
    }

    @Test
    public void testTeaInvalidFormat()
        throws IOException
    {
        String metaDataFile = "tracker/validations/te-program_with_tea_fileresource_metadata.json";
        setupMetadata( metaDataFile );

        TrackerBundle trackerBundle = renderService
            .fromJson( new ClassPathResource( "tracker/validations/te-program_with_tea_invalid_format_value.json" )
                    .getInputStream(),
                TrackerBundleParams.class ).toTrackerBundle();

        User user = userService.getUser( ADMIN_USER_UID );

        TrackerBundleParams bundle = TrackerBundleParams.builder()
            .trackedEntities( trackerBundle.getTrackedEntities() )
            .enrollments( trackerBundle.getEnrollments() )
            .events( trackerBundle.getEvents() )
            .build();

        bundle.setUser( user );

        List<TrackerBundle> trackerBundles = trackerBundleService.create( bundle );

        assertEquals( 1, trackerBundles.size() );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundles.get( 0 ) );
        assertEquals( 1, report.getErrorReports().size() );
        printReport( report );
        assertThat( report.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1085 ) ) ) );
    }

    @Test
    public void testTeaInvalidImage()
        throws IOException
    {
        String metaDataFile = "tracker/validations/te-program_with_tea_fileresource_metadata.json";
        setupMetadata( metaDataFile );

        TrackerBundle trackerBundle = renderService
            .fromJson( new ClassPathResource( "tracker/validations/te-program_with_tea_invalid_image_value.json" )
                    .getInputStream(),
                TrackerBundleParams.class ).toTrackerBundle();

        User user = userService.getUser( ADMIN_USER_UID );

        TrackerBundleParams bundle = TrackerBundleParams.builder()
            .trackedEntities( trackerBundle.getTrackedEntities() )
            .enrollments( trackerBundle.getEnrollments() )
            .events( trackerBundle.getEvents() )
            .build();

        bundle.setUser( user );

        List<TrackerBundle> trackerBundles = trackerBundleService.create( bundle );
        assertEquals( 1, trackerBundles.size() );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundles.get( 0 ) );
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
        String metaDataFile = "tracker/validations/te-program_with_tea_fileresource_metadata.json";
        setupMetadata( metaDataFile );

        TrackerBundle trackerBundle = renderService
            .fromJson( new ClassPathResource( "tracker/validations/te-program_with_tea_invalid_value_isnull.json" )
                    .getInputStream(),
                TrackerBundleParams.class ).toTrackerBundle();

        User user = userService.getUser( ADMIN_USER_UID );

        TrackerBundleParams bundle = TrackerBundleParams.builder()
            .trackedEntities( trackerBundle.getTrackedEntities() )
            .enrollments( trackerBundle.getEnrollments() )
            .events( trackerBundle.getEvents() )
            .build();

        bundle.setUser( user );

        List<TrackerBundle> trackerBundles = trackerBundleService.create( bundle );
        assertEquals( 1, trackerBundles.size() );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundles.get( 0 ) );
        assertEquals( 1, report.getErrorReports().size() );
        printReport( report );

        assertThat( report.getErrorReports(),
            hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1076 ) ) ) );
    }
}