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
package org.hisp.dhis.dxf2.metadata;

import static org.hisp.dhis.utils.Assertions.assertNotEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.MapUtils;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.dataexchange.aggregate.AggregateDataExchange;
import org.hisp.dhis.dataexchange.aggregate.SourceRequest;
import org.hisp.dhis.dataexchange.aggregate.TargetType;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.Section;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleMode;
import org.hisp.dhis.eventreport.EventReport;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.feedback.TypeReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.mapping.MapView;
import org.hisp.dhis.mapping.ThematicMapType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageSection;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.visualization.Visualization;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Slf4j
class MetadataImportServiceTest extends TransactionalIntegrationTest
{
    @Autowired
    private MetadataImportService importService;

    @Autowired
    private RenderService _renderService;

    @Autowired
    private UserService _userService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private ProgramStageService programStageService;

    @Autowired
    private AclService aclService;

    @Override
    protected void setUpTest()
        throws Exception
    {
        renderService = _renderService;
        userService = _userService;
    }

    @Test
    void testCorrectStatusOnImportNoErrors()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/dataset_with_sections.json" ).getInputStream(), RenderFormat.JSON );
        MetadataImportParams params = createParams( ImportStrategy.CREATE, metadata );
        ImportReport report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );
    }

    @Test
    void testCorrectStatusOnImportErrors()
        throws IOException
    {
        createUserAndInjectSecurityContext( true );
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/dataset_with_sections.json" ).getInputStream(), RenderFormat.JSON );
        MetadataImportParams params = createParams( ImportStrategy.CREATE, metadata );
        params.setAtomicMode( AtomicMode.NONE );
        ImportReport report = importService.importMetadata( params );
        assertEquals( Status.WARNING, report.getStatus() );
    }

    @Test
    void testCorrectStatusOnImportErrorsATOMIC()
        throws IOException
    {
        createUserAndInjectSecurityContext( true );
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/dataset_with_sections.json" ).getInputStream(), RenderFormat.JSON );
        MetadataImportParams params = createParams( ImportStrategy.CREATE, metadata );
        ImportReport report = importService.importMetadata( params );
        assertEquals( Status.ERROR, report.getStatus() );
    }

    @Test
    void testImportUpdatePublicAccess()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/dataset_with_accesses.json" ).getInputStream(), RenderFormat.JSON );
        MetadataImportParams params = new MetadataImportParams();
        params.setImportMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );
        ImportReport report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );
        DataSet dataSet = manager.get( DataSet.class, "em8Bg4LCr5k" );
        assertEquals( "rw------", dataSet.getPublicAccess() );
        metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/dataset_with_publicAccess_update.json" ).getInputStream(),
            RenderFormat.JSON );
        params = new MetadataImportParams();
        params.setImportMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.UPDATE );
        params.setObjects( metadata );
        report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );
        DataSet updatedDataSet = manager.get( DataSet.class, "em8Bg4LCr5k" );
        assertEquals( "r-------", updatedDataSet.getPublicAccess() );
    }

    @Test
    void testImportWithAccessObjects()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/dataset_with_accesses.json" ).getInputStream(), RenderFormat.JSON );
        MetadataImportParams params = createParams( ImportStrategy.CREATE, metadata );
        ImportReport report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );
        metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/dataset_with_accesses_update.json" ).getInputStream(),
            RenderFormat.JSON );
        params = createParams( ImportStrategy.UPDATE, metadata );
        report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );
    }

    /**
     * User only have READ access to Dashboard object User try to update
     * Dashboard with: skipSharing=true, and payload doesn't include sharing
     * data. Expected: import error
     */
    @Test
    void testImportWithSkipSharingIsTrueAndNoPermission()
    {
        clearSecurityContext();

        User userA = createUserWithAuth( "A" );
        userService.addUser( userA );
        Dashboard dashboard = new Dashboard();
        dashboard.setName( "DashboardA" );
        Sharing sharing = new Sharing();
        sharing.addUserAccess( new UserAccess( userA, AccessStringHelper.READ ) );
        dashboard.setSharing( sharing );
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = new HashMap<>();
        metadata.put( Dashboard.class, Collections.singletonList( dashboard ) );
        MetadataImportParams params = createParams( ImportStrategy.CREATE, metadata );
        params.setSkipSharing( false );
        // Create Dashboard
        ImportReport report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );
        // Check sharing data
        IdentifiableObject savedDashboard = manager.get( Dashboard.class, dashboard.getUid() );
        boolean condition = aclService.canWrite( userA, savedDashboard );
        assertFalse( condition );
        assertTrue( aclService.canRead( userA, savedDashboard ) );
        // Update dashboard with skipSharing=true and no sharing data in payload
        dashboard.setSharing( null );
        metadata.put( Dashboard.class, Collections.singletonList( dashboard ) );
        params = createParams( ImportStrategy.UPDATE, metadata );
        params.setSkipSharing( true );
        params.setUser( userA );
        report = importService.importMetadata( params );
        assertEquals( Status.ERROR, report.getStatus() );
    }

    /**
     * User have READ-WRITE access to Dashboard object User try to update
     * Dashboard with: skipSharing=true, and payload doesn't include sharing
     * data. Expected: import successfully
     */
    @Test
    void testImportWithSkipSharingIsTrueAndWritePermission()
    {
        User userA = makeUser( "A" );
        userService.addUser( userA );

        injectSecurityContext( userA );

        Dashboard dashboard = new Dashboard();
        dashboard.setName( "DashboardA" );
        Sharing sharing = new Sharing();
        sharing.setPublicAccess( AccessStringHelper.DEFAULT );
        sharing.addUserAccess( new UserAccess( userA, AccessStringHelper.READ_WRITE ) );
        dashboard.setSharing( sharing );
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = new HashMap<>();
        metadata.put( Dashboard.class, Collections.singletonList( dashboard ) );
        MetadataImportParams params = createParams( ImportStrategy.CREATE, metadata );
        params.setSkipSharing( false );
        // Create Dashboard
        ImportReport report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );
        // Check all sharing data
        IdentifiableObject savedDashboard = manager.get( Dashboard.class, dashboard.getUid() );
        assertTrue( aclService.canWrite( userA, savedDashboard ) );
        assertTrue( aclService.canRead( userA, savedDashboard ) );
        // Update Dashboard with skipSharing=true and no sharing data in payload
        dashboard.setSharing( null );
        metadata.put( Dashboard.class, Collections.singletonList( dashboard ) );
        params = createParams( ImportStrategy.UPDATE, metadata );
        params.setSkipSharing( true );
        params.setUser( userA );
        report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );
    }

    @Test
    void testImportWithSkipSharingIsTrue()
        throws IOException
    {
        User user = createUserWithAuth( "A", "ALL" );
        manager.save( user );

        injectSecurityContext( user );

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/dataset_with_accesses_skipSharing.json" ).getInputStream(),
            RenderFormat.JSON );
        MetadataImportParams params = createParams( ImportStrategy.CREATE, metadata );
        params.setSkipSharing( false );
        params.setUser( user );
        ImportReport report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );
        DataSet dataSet = manager.get( DataSet.class, "em8Bg4LCr5k" );
        assertNotNull( dataSet.getSharing().getUserGroups() );
        assertEquals( 1, dataSet.getSharing().getUserGroups().size() );
        assertEquals( "fvz8d3u6jFd", dataSet.getSharing().getUserGroups().values().iterator().next().getId() );
        metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/dataset_with_accesses_update_skipSharing.json" ).getInputStream(),
            RenderFormat.JSON );
        params = createParams( ImportStrategy.UPDATE, metadata );
        params.setSkipSharing( true );
        params.setUser( user );
        report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );
        DataSet dataSetUpdated = manager.get( DataSet.class, "em8Bg4LCr5k" );
        assertNotNull( dataSetUpdated.getSharing().getUserGroups() );
        assertEquals( 1, dataSetUpdated.getSharing().getUserGroups().size() );
        assertNotNull( dataSetUpdated.getSharing().getUserGroups().get( "fvz8d3u6jFd" ) );
    }

    @Test
    void testImportWithSkipSharingIsFalse()
        throws IOException
    {
        User user = createUserWithAuth( "A", "ALL" );
        manager.save( user );

        injectSecurityContext( user );

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/dataset_with_accesses_skipSharing.json" ).getInputStream(),
            RenderFormat.JSON );
        MetadataImportParams params = createParams( ImportStrategy.CREATE, metadata );
        params.setSkipSharing( false );
        params.setUser( user );
        ImportReport report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );
        DataSet dataSet = manager.get( DataSet.class, "em8Bg4LCr5k" );
        assertNotNull( dataSet.getSharing().getUserGroups() );
        assertEquals( 1, dataSet.getSharing().getUserGroups().size() );
        assertEquals( "fvz8d3u6jFd", dataSet.getSharing().getUserGroups().values().iterator().next().getId() );
        metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/dataset_with_accesses_update_skipSharing.json" ).getInputStream(),
            RenderFormat.JSON );
        params = createParams( ImportStrategy.UPDATE, metadata );
        params.setSkipSharing( false );
        params.setUser( user );
        report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );
        DataSet dataSetUpdated = manager.get( DataSet.class, "em8Bg4LCr5k" );
        assertTrue( MapUtils.isEmpty( dataSetUpdated.getSharing().getUserGroups() ) );
    }

    @Test
    void testImportNewObjectWithSkipTranslationIsTrue()
        throws IOException
    {
        User user = createUserWithAuth( "A", "ALL" );
        manager.save( user );

        injectSecurityContext( user );

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/dataset_with_accesses_skipSharing.json" ).getInputStream(),
            RenderFormat.JSON );
        MetadataImportParams params = createParams( ImportStrategy.CREATE, metadata );
        params.setSkipTranslation( true );
        params.setUser( user );
        ImportReport report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );
        DataSet dataSet = manager.get( DataSet.class, "em8Bg4LCr5k" );
        assertNotNull( dataSet.getSharing().getUserGroups() );
        // Payload has translations but skipTranslation = true
        assertEquals( 0, dataSet.getTranslations().size() );
    }

    @Test
    void testImportNewObjectWithSkipTranslationIsFalse()
        throws IOException
    {
        User user = createUserWithAuth( "A", "ALL" );
        manager.save( user );

        injectSecurityContext( user );

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/dataset_with_accesses_skipSharing.json" ).getInputStream(),
            RenderFormat.JSON );
        MetadataImportParams params = createParams( ImportStrategy.CREATE, metadata );
        params.setSkipTranslation( false );
        params.setUser( user );
        ImportReport report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );
        DataSet dataSet = manager.get( DataSet.class, "em8Bg4LCr5k" );
        assertNotNull( dataSet.getSharing().getUserGroups() );
        // Payload has translations and skipTranslation = false
        assertEquals( 2, dataSet.getTranslations().size() );
    }

    /**
     * 1. Create object with 2 translations 2. Update object with empty
     * translations and skipTranslation = false Expected: updated object has
     * empty translations
     */
    @Test
    void testUpdateWithSkipTranslationIsFalse()
        throws IOException
    {
        User user = createUserWithAuth( "A", "ALL" );
        manager.save( user );

        injectSecurityContext( user );

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/dataset_with_accesses_skipSharing.json" ).getInputStream(),
            RenderFormat.JSON );
        MetadataImportParams params = createParams( ImportStrategy.CREATE, metadata );
        params.setSkipTranslation( false );
        params.setUser( user );
        ImportReport report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );
        DataSet dataSet = manager.get( DataSet.class, "em8Bg4LCr5k" );
        assertNotNull( dataSet.getSharing().getUserGroups() );
        assertEquals( 2, dataSet.getTranslations().size() );
        metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/dataset_with_accesses_update_skipSharing.json" ).getInputStream(),
            RenderFormat.JSON );
        params = createParams( ImportStrategy.UPDATE, metadata );
        params.setSkipTranslation( false );
        params.setUser( user );
        report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );
        dataSet = manager.get( DataSet.class, "em8Bg4LCr5k" );
        assertNotNull( dataSet.getSharing().getUserGroups() );
        assertEquals( 0, dataSet.getTranslations().size() );
    }

    /**
     * 1. Create object with 2 translations 2. Update object with empty
     * translations and skipTranslation = true Expected: updated object still
     * has 2 translations
     */
    @Test
    void testUpdateWithSkipTranslationIsTrue()
        throws IOException
    {
        User user = createUserWithAuth( "A", "ALL" );
        manager.save( user );

        injectSecurityContext( user );

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/dataset_with_accesses_skipSharing.json" ).getInputStream(),
            RenderFormat.JSON );
        MetadataImportParams params = createParams( ImportStrategy.CREATE, metadata );
        params.setSkipTranslation( false );
        params.setUser( user );
        ImportReport report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );
        DataSet dataSet = manager.get( DataSet.class, "em8Bg4LCr5k" );
        assertNotNull( dataSet.getSharing().getUserGroups() );
        assertEquals( 2, dataSet.getTranslations().size() );
        metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/dataset_with_accesses_update_skipSharing.json" ).getInputStream(),
            RenderFormat.JSON );
        params = createParams( ImportStrategy.UPDATE, metadata );
        params.setSkipTranslation( true );
        params.setUser( user );
        report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );
        dataSet = manager.get( DataSet.class, "em8Bg4LCr5k" );
        assertNotNull( dataSet.getSharing().getUserGroups() );
        assertEquals( 2, dataSet.getTranslations().size() );
    }

    @Test
    void testImportMultiPropertyUniqueness()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/favorites/metadata_multi_property_uniqueness.json" ).getInputStream(),
            RenderFormat.JSON );
        MetadataImportParams params = createParams( ImportStrategy.CREATE, metadata );
        ImportReport importReport = importService.importMetadata( params );
        assertTrue( importReport.hasErrorReport( errorReport -> errorReport.getErrorCode() == ErrorCode.E5005 ) );
    }

    @Test
    void testImportEmbeddedObjectWithSkipSharingIsTrue()
        throws IOException
    {
        User user = makeUser( "A" );
        manager.save( user );
        UserGroup userGroup = createUserGroup( 'A', Sets.newHashSet( user ) );
        manager.save( userGroup );
        userGroup = manager.get( UserGroup.class, "ugabcdefghA" );
        assertNotNull( userGroup );
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/favorites/metadata_visualization_with_accesses.json" ).getInputStream(),
            RenderFormat.JSON );
        MetadataImportParams params = createParams( ImportStrategy.CREATE, metadata );
        ImportReport report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );
        Visualization visualization = manager.get( Visualization.class, "gyYXi0rXAIc" );
        assertNotNull( visualization );
        assertEquals( 1, visualization.getUserGroupAccesses().size() );
        assertEquals( 1, visualization.getUserAccesses().size() );
        assertEquals( user.getUid(), visualization.getUserAccesses().iterator().next().getUserUid() );
        assertEquals( userGroup.getUid(), visualization.getUserGroupAccesses().iterator().next().getUserGroupUid() );
        Visualization dataElementOperandVisualization = manager.get( Visualization.class, "qD72aBqsHvt" );
        assertNotNull( dataElementOperandVisualization );
        assertEquals( 2, dataElementOperandVisualization.getDataDimensionItems().size() );
        dataElementOperandVisualization.getDataDimensionItems().stream()
            .forEach( item -> assertNotNull( item.getDataElementOperand() ) );
        metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/favorites/metadata_visualization_with_accesses_update.json" )
                .getInputStream(),
            RenderFormat.JSON );
        params = createParams( ImportStrategy.UPDATE, metadata );
        params.setSkipSharing( true );
        dbmsManager.clearSession();
        report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );
        visualization = manager.get( Visualization.class, "gyYXi0rXAIc" );
        assertNotNull( visualization );
        assertEquals( 1, visualization.getUserGroupAccesses().size() );
        assertEquals( 1, visualization.getUserAccesses().size() );
        assertEquals( user.getUid(), visualization.getUserAccesses().iterator().next().getUser().getUid() );
        assertEquals( userGroup.getUid(),
            visualization.getUserGroupAccesses().iterator().next().getUserGroup().getUid() );
    }

    @Test
    void testImportEmbeddedObjectWithSkipSharingIsFalse()
        throws IOException
    {
        User user = makeUser( "A" );
        manager.save( user );
        User userA = manager.get( User.class, user.getUid() );
        assertNotNull( userA );
        UserGroup userGroup = createUserGroup( 'A', Sets.newHashSet( userA ) );
        manager.save( userGroup );
        userGroup = manager.get( UserGroup.class, "ugabcdefghA" );
        assertNotNull( userGroup );
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/favorites/metadata_visualization_with_accesses.json" ).getInputStream(),
            RenderFormat.JSON );
        MetadataImportParams params = createParams( ImportStrategy.CREATE, metadata );
        params.setSkipSharing( false );
        ImportReport report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );
        dbmsManager.clearSession();
        Visualization visualization = manager.get( Visualization.class, "gyYXi0rXAIc" );
        assertNotNull( visualization );
        assertEquals( 1, visualization.getUserGroupAccesses().size() );
        assertEquals( 1, visualization.getUserAccesses().size() );
        assertEquals( user.getUid(), visualization.getUserAccesses().iterator().next().getUser().getUid() );
        assertEquals( userGroup.getUid(),
            visualization.getUserGroupAccesses().iterator().next().getUserGroup().getUid() );
        metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/favorites/metadata_visualization_with_accesses_update.json" )
                .getInputStream(),
            RenderFormat.JSON );
        params = createParams( ImportStrategy.UPDATE, metadata );
        params.setSkipSharing( false );
        dbmsManager.clearSession();
        report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );
        visualization = manager.get( Visualization.class, "gyYXi0rXAIc" );
        assertNotNull( visualization );
        assertEquals( 0, visualization.getUserGroupAccesses().size() );
        assertEquals( 0, visualization.getUserAccesses().size() );
    }

    /**
     * 1. Create an object with UserGroupAccessA 2. Update object with only
     * UserGroupAccessB in payload and mergeMode=REPLACE Expected: updated
     * object will have only UserGroupAccessB
     *
     * @throws IOException
     */
    @Test
    void testImportSharingWithMergeModeReplace()
        throws IOException
    {
        User user = createUserWithAuth( "A", "ALL" );
        manager.save( user );

        injectSecurityContext( user );

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/dataset_with_accesses_skipSharing.json" ).getInputStream(),
            RenderFormat.JSON );
        MetadataImportParams params = createParams( ImportStrategy.CREATE, metadata );
        params.setUser( user );
        ImportReport report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );
        DataSet dataSet = manager.get( DataSet.class, "em8Bg4LCr5k" );
        assertNotNull( dataSet.getSharing().getUserGroups() );
        assertEquals( 1, dataSet.getSharing().getUserGroups().size() );
        metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/dataset_with_accesses_merge_mode.json" ).getInputStream(),
            RenderFormat.JSON );
        params = createParams( ImportStrategy.CREATE_AND_UPDATE, metadata );
        params.setMergeMode( MergeMode.REPLACE );
        params.setUser( user );
        report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );
        dataSet = manager.get( DataSet.class, "em8Bg4LCr5k" );
        assertNotNull( dataSet.getSharing().getUserGroups() );
        assertEquals( 1, dataSet.getSharing().getUserGroups().size() );
        assertNotNull( dataSet.getSharing().getUserGroups().get( "FnJeHbPOtVF" ) );
    }

    @Test
    void testImportProgramWithProgramStageSections()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/program_noreg_sections.json" ).getInputStream(), RenderFormat.JSON );
        MetadataImportParams params = createParams( ImportStrategy.CREATE, metadata );
        ImportReport report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );
        Program program = manager.get( Program.class, "s5uvS0Q7jnX" );
        assertNotNull( program );
        assertEquals( 1, program.getProgramStages().size() );
        ProgramStage programStage = program.getProgramStages().iterator().next();
        assertNotNull( programStage.getProgram() );
        assertEquals( 3, programStage.getProgramStageDataElements().size() );
        programStage.getProgramStageDataElements().forEach( psde -> {
            assertNotNull( psde.getSkipAnalytics() );
            assertFalse( psde.getSkipAnalytics() );
        } );
        Set<ProgramStageSection> programStageSections = programStage.getProgramStageSections();
        assertNotNull( programStageSections );
        assertEquals( 2, programStageSections.size() );
        ProgramStageSection programStageSection = programStageSections.iterator().next();
        assertNotNull( programStageSection.getProgramStage() );
    }

    @Test
    void testMetadataSyncWithDeletedDataSetSection()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/dataset_with_sections.json" ).getInputStream(), RenderFormat.JSON );
        MetadataImportParams params = createParams( ImportStrategy.CREATE_AND_UPDATE, metadata );
        ImportReport report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );
        dbmsManager.clearSession();
        DataSet dataset = manager.get( DataSet.class, "em8Bg4LCr5k" );
        assertNotNull( dataset.getSections() );
        assertNotNull( manager.get( Section.class, "JwcV2ZifEQf" ) );
        metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/dataset_with_removed_section.json" ).getInputStream(),
            RenderFormat.JSON );
        params = createParams( ImportStrategy.UPDATE, metadata );
        params.setMetadataSyncImport( true );
        dbmsManager.clearSession();
        report = importService.importMetadata( params );
        report.forEachErrorReport( errorReport -> log.error( "Error report:" + errorReport ) );
        assertEquals( Status.OK, report.getStatus() );
        dataset = manager.get( DataSet.class, "em8Bg4LCr5k" );
        assertEquals( 1, dataset.getSections().size() );
        assertNull( manager.get( Section.class, "JwcV2ZifEQf" ) );
        metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/dataset_with_all_section_removed.json" ).getInputStream(),
            RenderFormat.JSON );
        params = createParams( ImportStrategy.CREATE_AND_UPDATE, metadata );
        params.setMetadataSyncImport( true );
        dbmsManager.clearSession();
        report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );
        dataset = manager.get( DataSet.class, "em8Bg4LCr5k" );
        assertTrue( dataset.getSections().isEmpty() );
    }

    @Test
    void testMetadataImportWithDeletedProgramStageSection()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/programstage_with_sections.json" ).getInputStream(), RenderFormat.JSON );
        MetadataImportParams params = createParams( ImportStrategy.CREATE_AND_UPDATE, metadata );
        ImportReport report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );
        dbmsManager.clearSession();
        ProgramStage programStage = programStageService.getProgramStage( "NpsdDv6kKSO" );
        assertNotNull( programStage.getProgramStageSections() );
        assertNotNull( manager.get( ProgramStageSection.class, "JwcV2ZifEQf" ) );
        metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/programstage_with_removed_section.json" ).getInputStream(),
            RenderFormat.JSON );
        params = createParams( ImportStrategy.UPDATE, metadata );
        params.setMetadataSyncImport( true );
        report = importService.importMetadata( params );
        report.forEachErrorReport( errorReport -> log.error( "Error report:" + errorReport ) );
        assertEquals( Status.OK, report.getStatus() );
        programStage = manager.get( ProgramStage.class, "NpsdDv6kKSO" );
        assertEquals( 1, programStage.getProgramStageSections().size() );
        assertNull( manager.get( ProgramStageSection.class, "JwcV2ZifEQf" ) );
        metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/programstage_with_all_section_removed.json" ).getInputStream(),
            RenderFormat.JSON );
        params.setImportMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.UPDATE );
        params.setObjects( metadata );
        params.setMetadataSyncImport( true );
        report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );
        programStage = manager.get( ProgramStage.class, "NpsdDv6kKSO" );
        assertEquals( true, programStage.getProgramStageSections().isEmpty() );
    }

    @Test
    void testMetadataImportWithDeletedDataElements()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/dataset_with_sections_and_data_elements.json" ).getInputStream(),
            RenderFormat.JSON );
        MetadataImportParams params = createParams( ImportStrategy.CREATE_AND_UPDATE, metadata );
        ImportReport report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );
        DataSet dataset = manager.get( DataSet.class, "em8Bg4LCr5k" );
        assertNotNull( dataset.getSections() );
        assertNotNull( dataset.getDataElements() );
        assertTrue( dataset.getDataElements().stream().map( de -> de.getUid() ).collect( Collectors.toList() )
            .contains( "R45hiT7RLui" ) );
        assertNotNull( manager.get( Section.class, "JwcV2ZifEQf" ) );
        assertTrue( dataset.getSections().stream().filter( s -> s.getUid().equals( "JwcV2ZifEQf" ) ).findFirst().get()
            .getDataElements().stream().map( de -> de.getUid() ).collect( Collectors.toList() )
            .contains( "R45hiT7RLui" ) );
        metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/dataset_with_data_element_removed.json" ).getInputStream(),
            RenderFormat.JSON );
        params = createParams( ImportStrategy.CREATE_AND_UPDATE, metadata );
        params.setMetadataSyncImport( false );
        report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );
        dataset = manager.get( DataSet.class, "em8Bg4LCr5k" );
        assertFalse( dataset.getDataElements().stream().map( de -> de.getUid() ).collect( Collectors.toList() )
            .contains( "R45hiT7RLui" ) );
        assertNotNull( manager.get( Section.class, "JwcV2ZifEQf" ) );
        assertFalse( dataset.getSections().stream().filter( s -> s.getUid().equals( "JwcV2ZifEQf" ) ).findFirst().get()
            .getDataElements().stream().map( de -> de.getUid() ).collect( Collectors.toList() )
            .contains( "R45hiT7RLui" ) );
    }

    @Test
    void testUpdateUserGroupWithoutCreatedUserProperty()
        throws IOException
    {
        User userA = makeUser( "A", Lists.newArrayList( "ALL" ) );
        userService.addUser( userA );

        injectSecurityContext( userA );

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService
            .fromMetadata( new ClassPathResource( "dxf2/usergroups.json" ).getInputStream(), RenderFormat.JSON );
        MetadataImportParams params = createParams( ImportStrategy.CREATE, metadata );
        params.setUser( userA );
        ImportReport report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );
        UserGroup userGroup = manager.get( UserGroup.class, "OPVIvvXzNTw" );
        assertEquals( userA.getUid(), userGroup.getSharing().getOwner() );
        User userB = createUserWithAuth( "B", "ALL" );
        userService.addUser( userB );
        metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/usergroups_update.json" ).getInputStream(),
            RenderFormat.JSON );
        params = createParams( ImportStrategy.UPDATE, metadata );
        params.setUser( userB );
        report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );
        userGroup = manager.get( UserGroup.class, "OPVIvvXzNTw" );
        assertEquals( "TA user group updated", userGroup.getName() );
        assertEquals( userA.getUid(), userGroup.getSharing().getOwner() );
    }

    @Test
    void testUpdateImmutableCreatedByField()
        throws IOException
    {
        User userA = makeUser( "A", Lists.newArrayList( "ALL" ) );
        userService.addUser( userA );

        injectSecurityContext( userA );

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService
            .fromMetadata( new ClassPathResource( "dxf2/usergroups.json" ).getInputStream(), RenderFormat.JSON );
        MetadataImportParams params = createParams( ImportStrategy.CREATE, metadata );
        params.setUser( userA );
        ImportReport report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );
        UserGroup userGroup = manager.get( UserGroup.class, "OPVIvvXzNTw" );
        assertEquals( userA.getUid(), userGroup.getCreatedBy().getUid() );
        User userB = createUserWithAuth( "B", "ALL" );
        userB.setUid( "userabcdefB" );
        userService.addUser( userB );
        metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/usergroups_update.json" ).getInputStream(),
            RenderFormat.JSON );
        params = createParams( ImportStrategy.UPDATE, metadata );
        params.setUser( userA );
        report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );
        userGroup = manager.get( UserGroup.class, "OPVIvvXzNTw" );
        assertEquals( "TA user group updated", userGroup.getName() );
        assertEquals( userA.getUid(), userGroup.getCreatedBy().getUid() );
    }

    @Test
    void testImportUser()
        throws IOException
    {
        User userF = makeUser( "F", Lists.newArrayList( "ALL" ) );
        userService.addUser( userF );

        injectSecurityContext( userF );

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/create_user_without_createdBy.json" ).getInputStream(),
            RenderFormat.JSON );
        MetadataImportParams params = createParams( ImportStrategy.CREATE_AND_UPDATE, metadata );
        params.setUser( userF );
        ImportReport report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );
        User user = manager.get( User.class, "MwhEJUnTHkn" );
        assertNotNull( user.getCreatedBy() );
    }

    @Test
    void testImportUserLegacyFormat()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/create_user_with_legacy_format.json" ).getInputStream(),
            RenderFormat.JSON );
        MetadataImportParams params = createParams( ImportStrategy.CREATE_AND_UPDATE, metadata );
        ImportReport report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );
        assertNotNull( manager.get( User.class, "sPWjoHSY03y" ) );
    }

    @Test
    void testImportUserLegacyFormatWithPersistedReferences()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> userRoles = renderService.fromMetadata(
            new ClassPathResource( "dxf2/userrole.json" ).getInputStream(),
            RenderFormat.JSON );
        MetadataImportParams params = createParams( ImportStrategy.CREATE_AND_UPDATE, userRoles );
        ImportReport report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/user.json" ).getInputStream(),
            RenderFormat.JSON );
        params = createParams( ImportStrategy.CREATE_AND_UPDATE, metadata );
        report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );
        User user = manager.get( User.class, "sPWjoHSY03y" );
        assertNotNull( user );
        assertTrue( user.getUserRoles().stream().anyMatch( userRole -> userRole.getUid().equals( "xJZBzAHI88H" ) ) );
    }

    @Test
    void testImportMapCreateAndUpdate()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService
            .fromMetadata( new ClassPathResource( "dxf2/map_new.json" ).getInputStream(), RenderFormat.JSON );
        MetadataImportParams params = new MetadataImportParams();
        params.setImportMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );
        ImportReport report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );
        List<org.hisp.dhis.mapping.Map> maps = manager.getAll( org.hisp.dhis.mapping.Map.class );
        assertEquals( 1, maps.size() );
        assertEquals( "test1", maps.get( 0 ).getName() );
        assertEquals( 1, maps.get( 0 ).getMapViews().size() );
        org.hisp.dhis.mapping.Map map = manager.get( org.hisp.dhis.mapping.Map.class, "LTNgXfzTFTv" );
        assertNotNull( map );
        assertEquals( 1, map.getMapViews().size() );
        MapView mapView = map.getMapViews().get( 0 );
        assertNotNull( mapView );
        assertEquals( "#ddeeff", mapView.getNoDataColor() );
        assertEquals( "#aabbcc", mapView.getOrganisationUnitColor() );
        assertEquals( ThematicMapType.CHOROPLETH, mapView.getThematicMapType() );
        metadata = renderService.fromMetadata( new ClassPathResource( "dxf2/map_update.json" ).getInputStream(),
            RenderFormat.JSON );
        params = new MetadataImportParams();
        params.setImportMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE_AND_UPDATE );
        params.setObjects( metadata );
        report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );
        map = manager.get( org.hisp.dhis.mapping.Map.class, "LTNgXfzTFTv" );
        assertNotNull( map );
        assertEquals( 1, map.getMapViews().size() );
        mapView = map.getMapViews().get( 0 );
        assertNotNull( mapView );
        assertEquals( "#ddeeff", mapView.getNoDataColor() );
        assertEquals( "#aabbcc", mapView.getOrganisationUnitColor() );
        assertEquals( ThematicMapType.CHOROPLETH, mapView.getThematicMapType() );
    }

    /**
     * Payload includes Program and ProgramStage with sharing settings.
     * <p>
     * Expected: after created, both Program and ProgramStage are saved
     * correctly together with sharing settings.
     */
    @Test
    void testImportProgramWithProgramStageAndSharing()
        throws IOException
    {
        User user = createUserWithAuth( "A", "ALL" );
        manager.save( user );

        injectSecurityContext( user );

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/program_programStage_with_sharing.json" ).getInputStream(),
            RenderFormat.JSON );
        MetadataImportParams params = createParams( ImportStrategy.CREATE, metadata );
        params.setSkipSharing( false );
        params.setUser( user );
        ImportReport report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );
        ProgramStage programStage = programStageService.getProgramStage( "oORy3Rg9hLE" );
        assertEquals( 1, programStage.getSharing().getUserGroups().size() );
        Program program = manager.get( Program.class, "QIHW6CBdLsP" );
        assertEquals( 1, program.getSharing().getUserGroups().size() );
    }

    @Test
    void testImportEventReportWithProgramIndicators()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/eventreport_with_program_indicator.json" ).getInputStream(),
            RenderFormat.JSON );
        MetadataImportParams params = createParams( ImportStrategy.CREATE, metadata );
        ImportReport report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );
        EventReport eventReport = manager.get( EventReport.class, "pCSijMNjMcJ" );
        assertNotNull( eventReport.getProgramIndicatorDimensions() );
        assertEquals( 1, eventReport.getProgramIndicatorDimensions().size() );
        assertEquals( "Cl00ghs775c", eventReport.getProgramIndicatorDimensions().get( 0 ).getUid() );
    }

    @Test
    void testImportVisualizationWithLegendSet()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/favorites/metadata_with_visualization_and_legendSet.json" )
                .getInputStream(),
            RenderFormat.JSON );
        MetadataImportParams params = createParams( ImportStrategy.CREATE, metadata );
        ImportReport report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );

        Visualization visualization = manager.get( Visualization.class, "gyYXi0rXAIc" );
        assertNotNull( visualization.getLegendDefinitions().getLegendSet() );
        assertEquals( "CGWUjDCWaMA", visualization.getLegendDefinitions().getLegendSet().getUid() );
    }

    @Test
    void testImportAggregateDataExchange()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/aggregate_data_exchange.json" ).getInputStream(), RenderFormat.JSON );
        MetadataImportParams params = createParams( ImportStrategy.CREATE_AND_UPDATE, metadata );
        ImportReport report = importService.importMetadata( params );
        TypeReport typeReport = report.getTypeReport( AggregateDataExchange.class );

        assertNotNull( report.getStats() );
        assertNotNull( typeReport );
        assertEquals( Status.OK, report.getStatus(), report.toString() );
        assertEquals( 0, report.getErrorReportsCount() );
        assertEquals( 6, report.getStats().getCreated() );
        assertEquals( 3, typeReport.getStats().getCreated() );

        AggregateDataExchange aeA = manager.get( AggregateDataExchange.class, "iFOyIpQciyk" );
        assertNotNull( aeA );
        assertNotNull( aeA.getSource() );
        assertNotNull( aeA.getSource().getParams() );
        assertNotEmpty( aeA.getSource().getParams().getPeriodTypes() );
        assertNotNull( aeA.getSource().getRequests() );
        SourceRequest srA = aeA.getSource().getRequests().get( 0 );
        assertNotNull( srA );
        assertNotNull( srA.getName() );
        assertNotNull( srA.getVisualization() );
        assertNotNull( aeA.getTarget() );
        assertEquals( "iFOyIpQciyk", aeA.getUid() );
        assertEquals( TargetType.INTERNAL, aeA.getTarget().getType() );

        AggregateDataExchange aeB = manager.get( AggregateDataExchange.class, "PnWccbwCJLQ" );
        assertNotNull( aeB );
        assertNotNull( aeB.getSource() );
        assertNotNull( aeB.getSource().getParams() );
        assertNotEmpty( aeB.getSource().getParams().getPeriodTypes() );
        assertNotNull( aeB.getSource().getRequests() );
        SourceRequest srB = aeA.getSource().getRequests().get( 0 );
        assertNotNull( srB );
        assertNotNull( srB.getName() );
        assertNotNull( srB.getVisualization() );
        assertNotNull( aeB.getTarget() );
        assertEquals( "PnWccbwCJLQ", aeB.getUid() );
        assertEquals( TargetType.EXTERNAL, aeB.getTarget().getType() );
        assertEquals( "https://play.dhis2.org/2.38.1", aeB.getTarget().getApi().getUrl() );
        assertEquals( "admin", aeB.getTarget().getApi().getUsername() );
        assertNotNull( aeB.getTarget().getApi().getPassword() );

        AggregateDataExchange aeC = manager.get( AggregateDataExchange.class, "VpQ4qVEseyM" );
        assertNotNull( aeC );
        assertNotNull( aeC.getSource() );
        assertNotNull( aeC.getSource().getParams() );
        assertNotEmpty( aeC.getSource().getParams().getPeriodTypes() );
        assertNotNull( aeC.getSource().getRequests() );
        SourceRequest srC = aeA.getSource().getRequests().get( 0 );
        assertNotNull( srC );
        assertNotNull( srC.getName() );
        assertNotNull( srC.getVisualization() );
        assertNotNull( aeC.getTarget() );
        assertEquals( "VpQ4qVEseyM", aeC.getUid() );
        assertEquals( TargetType.EXTERNAL, aeC.getTarget().getType() );
        assertEquals( "https://play.dhis2.org/2.38.1", aeC.getTarget().getApi().getUrl() );
        assertNotNull( aeC.getTarget().getApi().getAccessToken() );
    }

    private MetadataImportParams createParams( ImportStrategy importStrategy,
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata )
    {
        return new MetadataImportParams()
            .setImportMode( ObjectBundleMode.COMMIT )
            .setImportStrategy( importStrategy )
            .setObjects( metadata );
    }
}
