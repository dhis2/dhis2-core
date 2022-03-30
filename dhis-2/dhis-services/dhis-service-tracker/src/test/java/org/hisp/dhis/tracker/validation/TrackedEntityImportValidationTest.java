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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.hamcrest.core.IsCollectionContaining;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleParams;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleService;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleValidationService;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleCommitReport;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleValidationReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.tracker.AtomicMode;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerImportService;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerImportReport;
import org.hisp.dhis.tracker.report.TrackerStatus;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class TrackedEntityImportValidationTest extends AbstractImportValidationTest
{

    @Autowired
    protected TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    private ObjectBundleService objectBundleService;

    @Autowired
    private ObjectBundleValidationService objectBundleValidationService;

    @Autowired
    private TrackerImportService trackerImportService;

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
        User systemUser = createUserWithAuth( "systemUser", "ALL" );
        userService.addUser( systemUser );
        injectSecurityContext( systemUser );
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "tracker/tracker_basic_metadata.json" ).getInputStream(), RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validationReport = objectBundleValidationService.validate( bundle );
        assertFalse( validationReport.hasErrorReports() );
        ObjectBundleCommitReport commit = objectBundleService.commit( bundle );
        assertFalse( commit.hasErrorReports() );
        manager.flush();
    }

    @Test
    void failValidationWhenTrackedEntityAttributeHasWrongOptionValue()
        throws IOException
    {
        TrackerImportParams params = createBundleFromJson( "tracker/validations/te-with_invalid_option_value.json" );
        params.setImportStrategy( TrackerImportStrategy.CREATE );
        TrackerImportReport trackerImportReport = trackerImportService.importTracker( params );
        assertEquals( 1, trackerImportReport.getValidationReport().getErrors().size() );
        assertThat( trackerImportReport.getValidationReport().getErrors(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1125 ) ) ) );
    }

    @Test
    void successValidationWhenTrackedEntityAttributeHasValidOptionValue()
        throws IOException
    {
        TrackerImportParams params = createBundleFromJson( "tracker/validations/te-with_valid_option_value.json" );
        params.setImportStrategy( TrackerImportStrategy.CREATE );
        TrackerImportReport trackerImportReport = trackerImportService.importTracker( params );
        assertEquals( 0, trackerImportReport.getValidationReport().getErrors().size() );
    }

    @Test
    void failValidationWhenTrackedEntityAttributesHaveSameUniqueValues()
        throws IOException
    {
        TrackerImportParams params = createBundleFromJson( "tracker/validations/te-with_unique_attributes.json" );
        params.setImportStrategy( TrackerImportStrategy.CREATE );
        TrackerImportReport trackerImportReport = trackerImportService.importTracker( params );
        assertEquals( 2, trackerImportReport.getValidationReport().getErrors().size() );
        assertThat( trackerImportReport.getValidationReport().getErrors(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1064 ) ) ) );
    }

    @Test
    void testTeValidationOkAll()
        throws IOException
    {
        TrackerImportParams params = createBundleFromJson( "tracker/validations/te-data_with_different_ou.json" );
        params.setImportStrategy( TrackerImportStrategy.CREATE );
        TrackerImportReport trackerImportReport = trackerImportService.importTracker( params );
        assertEquals( 0, trackerImportReport.getValidationReport().getErrors().size() );
        assertEquals( TrackerStatus.OK, trackerImportReport.getStatus() );
    }

    @Test
    void testNoCreateTeiAccessOutsideCaptureScopeOu()
        throws IOException
    {
        TrackerImportParams params = createBundleFromJson( "tracker/validations/te-data_with_different_ou.json" );
        User user = userService.getUser( USER_7 );
        params.setUser( user );
        params.setImportStrategy( TrackerImportStrategy.CREATE );
        params.setAtomicMode( AtomicMode.OBJECT );
        TrackerImportReport trackerImportReport = trackerImportService.importTracker( params );
        assertEquals( 1, trackerImportReport.getValidationReport().getErrors().size() );
        assertEquals( 2, trackerImportReport.getStats().getCreated() );
        assertEquals( 1, trackerImportReport.getStats().getIgnored() );
        assertThat( trackerImportReport.getValidationReport().getErrors(),
            IsCollectionContaining.hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1000 ) ) ) );
    }

    @Test
    void testUpdateAccessInSearchScopeOu()
        throws IOException
    {
        TrackerImportParams params = createBundleFromJson( "tracker/validations/te-data_with_different_ou.json" );
        params.setImportStrategy( TrackerImportStrategy.CREATE );
        TrackerImportReport trackerImportReport = trackerImportService.importTracker( params );
        assertEquals( TrackerStatus.OK, trackerImportReport.getStatus() );
        assertEquals( 0, trackerImportReport.getValidationReport().getErrors().size() );
        assertEquals( 3, trackerImportReport.getStats().getCreated() );
        // For some reason teiSearchOrgunits is not created properly from
        // metadata
        // Redoing the update here for the time being.
        User user = userService.getUser( USER_8 );
        user.setTeiSearchOrganisationUnits( new HashSet<>( user.getDataViewOrganisationUnits() ) );
        userService.updateUser( user );
        dbmsManager.clearSession();
        params = createBundleFromJson( "tracker/validations/te-data_with_different_ou.json" );
        user = userService.getUser( USER_8 );
        params.setUser( user );
        params.setImportStrategy( TrackerImportStrategy.CREATE_AND_UPDATE );
        params.setAtomicMode( AtomicMode.OBJECT );
        trackerImportReport = trackerImportService.importTracker( params );
        assertEquals( TrackerStatus.OK, trackerImportReport.getStatus() );
        assertEquals( 0, trackerImportReport.getValidationReport().getErrors().size() );
        assertEquals( 3, trackerImportReport.getStats().getUpdated() );
    }

    @Test
    void testNoUpdateAccessOutsideSearchScopeOu()
        throws IOException
    {
        TrackerImportParams params = createBundleFromJson( "tracker/validations/te-data_with_different_ou.json" );
        params.setImportStrategy( TrackerImportStrategy.CREATE );
        TrackerImportReport trackerImportReport = trackerImportService.importTracker( params );
        assertEquals( TrackerStatus.OK, trackerImportReport.getStatus() );
        assertEquals( 0, trackerImportReport.getValidationReport().getErrors().size() );
        assertEquals( 3, trackerImportReport.getStats().getCreated() );
        dbmsManager.clearSession();
        params = createBundleFromJson( "tracker/validations/te-data_with_different_ou.json" );
        User user = userService.getUser( USER_7 );
        params.setUser( user );
        params.setImportStrategy( TrackerImportStrategy.CREATE_AND_UPDATE );
        params.setAtomicMode( AtomicMode.OBJECT );
        trackerImportReport = trackerImportService.importTracker( params );
        assertEquals( 1, trackerImportReport.getValidationReport().getErrors().size() );
        assertEquals( 2, trackerImportReport.getStats().getUpdated() );
        assertEquals( 1, trackerImportReport.getStats().getIgnored() );
        assertThat( trackerImportReport.getValidationReport().getErrors(),
            IsCollectionContaining.hasItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1003 ) ) ) );
    }

    @Test
    void testNoWriteAccessInAcl()
        throws IOException
    {
        TrackerImportParams params = createBundleFromJson( "tracker/validations/te-data_ok.json" );
        User user = userService.getUser( USER_1 );
        params.setUser( user );
        params.setImportStrategy( TrackerImportStrategy.CREATE );
        TrackerImportReport trackerImportReport = trackerImportService.importTracker( params );
        assertEquals( 13, trackerImportReport.getValidationReport().getErrors().size() );
        assertThat( trackerImportReport.getValidationReport().getErrors(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1001 ) ) ) );
    }

    @Test
    void testWriteAccessInAclViaUserGroup()
        throws IOException
    {
        TrackerImportParams params = createBundleFromJson( "tracker/validations/te-data_ok.json" );
        User user = userService.getUser( USER_3 );
        params.setUserId( user.getUid() );
        params.setUser( user );
        user.setPassword( "user4password" );
        injectSecurityContext( user );
        params.setImportStrategy( TrackerImportStrategy.CREATE );
        TrackerImportReport trackerImportReport = trackerImportService.importTracker( params );
        assertEquals( 0, trackerImportReport.getValidationReport().getErrors().size() );
        assertEquals( TrackerStatus.OK, trackerImportReport.getStatus() );
    }

    @Test
    void testGeoOk()
        throws IOException
    {
        TrackerImportParams params = createBundleFromJson( "tracker/validations/te-data_error_geo-ok.json" );
        params.setImportStrategy( TrackerImportStrategy.CREATE );
        TrackerImportReport trackerImportReport = trackerImportService.importTracker( params );
        assertEquals( 0, trackerImportReport.getValidationReport().getErrors().size() );
    }

    @Test
    void testTeAttrNonExistentAttr()
        throws IOException
    {
        TrackerImportParams params = createBundleFromJson( "tracker/validations/te-data_error_attr-non-existing.json" );
        params.setImportStrategy( TrackerImportStrategy.CREATE );
        TrackerImportReport trackerImportReport = trackerImportService.importTracker( params );
        assertEquals( 2, trackerImportReport.getValidationReport().getErrors().size() );
        assertThat( trackerImportReport.getValidationReport().getErrors(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1006 ) ) ) );
    }

    @Test
    void testDeleteCascadeProgramInstances()
        throws IOException
    {
        TrackerImportParams params = createBundleFromJson( "tracker/validations/enrollments_te_te-data.json" );
        params.setImportStrategy( TrackerImportStrategy.CREATE );
        TrackerImportReport trackerImportReport = trackerImportService.importTracker( params );
        assertEquals( 0, trackerImportReport.getValidationReport().getErrors().size() );
        assertEquals( TrackerStatus.OK, trackerImportReport.getStatus() );
        importProgramInstances();
        manager.flush();
        manager.clear();
        params = renderService.fromJson(
            new ClassPathResource( "tracker/validations/enrollments_te_te-data.json" ).getInputStream(),
            TrackerImportParams.class );
        User user2 = userService.getUser( USER_4 );
        params.setUser( user2 );
        params.setImportStrategy( TrackerImportStrategy.DELETE );
        trackerImportReport = trackerImportService.importTracker( params );
        assertEquals( 2, trackerImportReport.getValidationReport().getErrors().size() );
        assertThat( trackerImportReport.getValidationReport().getErrors(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1100 ) ) ) );
    }

    @Test
    void testTeDeleteOk()
        throws IOException
    {
        TrackerImportParams params = createBundleFromJson( "tracker/validations/te-data.json" );
        params.setImportStrategy( TrackerImportStrategy.CREATE );
        TrackerImportReport trackerImportReport = trackerImportService.importTracker( params );
        assertEquals( 0, trackerImportReport.getValidationReport().getErrors().size() );
        assertEquals( TrackerStatus.OK, trackerImportReport.getStatus() );

        manager.flush();
        manager.clear();

        TrackerImportParams paramsDelete = createBundleFromJson(
            "tracker/validations/te-data-delete.json" );
        paramsDelete.setImportStrategy( TrackerImportStrategy.DELETE );

        TrackerImportReport trackerImportReportDelete = trackerImportService.importTracker( paramsDelete );
        assertEquals( 0, trackerImportReportDelete.getValidationReport().getErrors().size() );
        assertEquals( TrackerStatus.OK, trackerImportReportDelete.getStatus() );
        assertEquals( 1, trackerImportReportDelete.getStats().getDeleted() );
    }

    protected void importProgramInstances()
        throws IOException
    {
        TrackerImportParams params = createBundleFromJson( "tracker/validations/enrollments_te_enrollments-data.json" );
        params.setImportStrategy( TrackerImportStrategy.CREATE );
        TrackerImportReport trackerImportReport = trackerImportService.importTracker( params );
        assertEquals( 0, trackerImportReport.getValidationReport().getErrors().size() );
        assertEquals( TrackerStatus.OK, trackerImportReport.getStatus() );
    }
}
