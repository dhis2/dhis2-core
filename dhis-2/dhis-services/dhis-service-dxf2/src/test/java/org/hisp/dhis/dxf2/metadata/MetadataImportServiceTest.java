package org.hisp.dhis.dxf2.metadata;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import com.google.common.collect.Sets;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.chart.Chart;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.dataset.Section;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleMode;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageSection;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class MetadataImportServiceTest
    extends DhisSpringTest
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
    private DataSetService dataSetService;

    @Override
    protected void setUpTest() throws Exception
    {
        renderService = _renderService;
        userService = _userService;
    }

    @Test
    public void testCorrectStatusOnImportNoErrors() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/dataset_with_sections.json" ).getInputStream(), RenderFormat.JSON );

        MetadataImportParams params = new MetadataImportParams();
        params.setImportMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );

        ImportReport report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );
    }

    @Test
    public void testCorrectStatusOnImportErrors() throws IOException
    {
        createUserAndInjectSecurityContext( true );

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/dataset_with_sections.json" ).getInputStream(), RenderFormat.JSON );

        MetadataImportParams params = new MetadataImportParams();
        params.setImportMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setAtomicMode( AtomicMode.NONE );
        params.setObjects( metadata );

        ImportReport report = importService.importMetadata( params );
        assertEquals( Status.WARNING, report.getStatus() );
    }

    @Test
    public void testCorrectStatusOnImportErrorsATOMIC() throws IOException
    {
        createUserAndInjectSecurityContext( true );

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/dataset_with_sections.json" ).getInputStream(), RenderFormat.JSON );

        MetadataImportParams params = new MetadataImportParams();
        params.setImportMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );

        ImportReport report = importService.importMetadata( params );
        assertEquals( Status.ERROR, report.getStatus() );
    }

    @Test
    public void testImportWithAccessObjects() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/dataset_with_accesses.json" ).getInputStream(), RenderFormat.JSON );

        MetadataImportParams params = new MetadataImportParams();
        params.setImportMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );

        ImportReport report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );

        metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/dataset_with_accesses_update.json" ).getInputStream(), RenderFormat.JSON );

        params = new MetadataImportParams();
        params.setImportMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.UPDATE );
        params.setObjects( metadata );

        report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );
    }

    @Test
    public void testImportEmbeddedObjectWithSkipSharingIsTrue() throws IOException
    {
        User user = createUser( 'A' );
        manager.save( user );

        UserGroup userGroup = createUserGroup( 'A', Sets.newHashSet( user ) );
        manager.save( userGroup );

        userGroup = manager.get( UserGroup.class, "ugabcdefghA" );
        assertNotNull( userGroup );

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/favorites/metadata_chart_with_accesses.json" ).getInputStream(), RenderFormat.JSON );

        MetadataImportParams params = new MetadataImportParams();
        params.setImportMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );

        ImportReport report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );

        Chart chart = manager.get( Chart.class, "gyYXi0rXAIc" );
        assertNotNull( chart );
        assertEquals( 1, chart.getUserGroupAccesses().size() );
        assertEquals( 1, chart.getUserAccesses().size() );
        assertEquals( user.getUid(), chart.getUserAccesses().iterator().next().getUserUid() );
        assertEquals( userGroup.getUid(), chart.getUserGroupAccesses().iterator().next().getUserGroupUid() );

        metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/favorites/metadata_chart_with_accesses_update.json" ).getInputStream(), RenderFormat.JSON );

        params = new MetadataImportParams();
        params.setImportMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.UPDATE );
        params.setObjects( metadata );
        params.setSkipSharing( true );

        report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );

        chart = manager.get( Chart.class, "gyYXi0rXAIc" );
        assertNotNull( chart );
        assertEquals( 1, chart.getUserGroupAccesses().size() );
        assertEquals( 1, chart.getUserAccesses().size() );
        assertEquals( user.getUid(), chart.getUserAccesses().iterator().next().getUserUid() );
        assertEquals( userGroup.getUid(), chart.getUserGroupAccesses().iterator().next().getUserGroupUid() );
    }

    @Test
    public void testImportEmbeddedObjectWithSkipSharingIsFalse() throws IOException
    {

        User user = createUser( 'A' );
        manager.save( user );

        UserGroup userGroup = createUserGroup( 'A', Sets.newHashSet( user ) );
        manager.save( userGroup );

        userGroup = manager.get( UserGroup.class, "ugabcdefghA" );
        assertNotNull( userGroup );

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/favorites/metadata_chart_with_accesses.json" ).getInputStream(), RenderFormat.JSON );

        MetadataImportParams params = new MetadataImportParams();
        params.setImportMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );

        ImportReport report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );

        Chart chart = manager.get( Chart.class, "gyYXi0rXAIc" );
        assertNotNull( chart );
        assertEquals( 1, chart.getUserGroupAccesses().size() );
        assertEquals( 1, chart.getUserAccesses().size() );
        assertEquals( user.getUid(), chart.getUserAccesses().iterator().next().getUserUid() );
        assertEquals( userGroup.getUid(), chart.getUserGroupAccesses().iterator().next().getUserGroupUid() );

        metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/favorites/metadata_chart_with_accesses_update.json" ).getInputStream(), RenderFormat.JSON );

        params = new MetadataImportParams();
        params.setImportMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.UPDATE );
        params.setObjects( metadata );
        params.setSkipSharing( false );

        report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );

        chart = manager.get( Chart.class, "gyYXi0rXAIc" );
        assertNotNull( chart );
        assertEquals( 0, chart.getUserGroupAccesses().size() );
        assertEquals( 0, chart.getUserAccesses().size() );
    }

    @Test
    public void testImportProgramWithProgramStageSections() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
                new ClassPathResource( "dxf2/program_noreg_sections.json" ).getInputStream(), RenderFormat.JSON );

        MetadataImportParams params = new MetadataImportParams();
        params.setImportMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );

        ImportReport report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );

        Program program = manager.get( Program.class,  "s5uvS0Q7jnX");

        assertNotNull( program );
        assertEquals( 1, program.getProgramStages().size() );

        ProgramStage programStage = program.getProgramStages().iterator().next();
        assertNotNull( programStage.getProgram() );

        Set<ProgramStageSection> programStageSections = programStage.getProgramStageSections();
        assertNotNull( programStageSections );
        assertEquals( 2, programStageSections.size() );

        ProgramStageSection programStageSection = programStageSections.iterator().next();
        assertNotNull( programStageSection.getProgramStage() );
    }

    @Test
    public void testMetadataSyncWithDeletedDataSetSection() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
                new ClassPathResource( "dxf2/dataset_with_sections.json" ).getInputStream(), RenderFormat.JSON );

        MetadataImportParams params = new MetadataImportParams();
        params.setImportMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE_AND_UPDATE );
        params.setObjects( metadata );

        ImportReport report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );

        DataSet dataset = dataSetService.getDataSet( "em8Bg4LCr5k" );

        assertNotNull( dataset.getSections() );

        assertNotNull( manager.get( Section.class, "JwcV2ZifEQf" ) );

        metadata = renderService.fromMetadata(
                new ClassPathResource( "dxf2/dataset_with_removed_section.json" ).getInputStream(), RenderFormat.JSON );
        params.setImportMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE_AND_UPDATE );
        params.setObjects( metadata );
        params.setMetadataSyncImport( true );

        report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );

        dataset = manager.get( DataSet.class, "em8Bg4LCr5k" );

        assertEquals(1, dataset.getSections().size() );

        assertNull( manager.get( Section.class, "JwcV2ZifEQf" ) );

        metadata = renderService.fromMetadata(
                new ClassPathResource( "dxf2/dataset_with_all_section_removed.json" ).getInputStream(), RenderFormat.JSON );
        params.setImportMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE_AND_UPDATE );
        params.setObjects( metadata );
        params.setMetadataSyncImport( true );

        report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );

        dataset = manager.get( DataSet.class, "em8Bg4LCr5k" );

        assertEquals(true, dataset.getSections().isEmpty() );
    }

    @Test
    public void testUpdateUserGroupWithoutCreatedUserProperty() throws IOException
    {
        User userA = createUser( "A", "ALL" );
        userService.addUser( userA );

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/usergroups.json" ).getInputStream(), RenderFormat.JSON );

        MetadataImportParams params = new MetadataImportParams();
        params.setImportMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );
        params.setUser( userA );

        ImportReport report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );

        UserGroup userGroup = manager.get( UserGroup.class, "OPVIvvXzNTw" );
        assertEquals( userA, userGroup.getUser() );

        User userB = createUser( "B", "ALL" );
        userService.addUser( userB );

        metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/usergroups_update.json" ).getInputStream(), RenderFormat.JSON );

        params = new MetadataImportParams();
        params.setImportMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.UPDATE );
        params.setObjects( metadata );
        params.setUser( userB );

        report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );

        userGroup = manager.get( UserGroup.class, "OPVIvvXzNTw" );
        assertEquals( "TA user group updated", userGroup.getName() );
        assertEquals( userA, userGroup.getUser() );
    }

    @Test
    public void testImportWithSkipSharingIsTrue() throws IOException
    {
        User user = createUser( "A", "ALL" );
        manager.save( user );

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/dataset_with_accesses_skipSharing.json" ).getInputStream(), RenderFormat.JSON );

        MetadataImportParams params = new MetadataImportParams();
        params.setImportMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setSkipSharing( true );
        params.setObjects( metadata );
        params.setUser( user );

        ImportReport report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );

        metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/dataset_with_accesses_update_skipSharing.json" ).getInputStream(), RenderFormat.JSON );

        params = new MetadataImportParams();
        params.setImportMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.UPDATE );
        params.setSkipSharing( true );
        params.setObjects( metadata );
        params.setUser( user );

        report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );
    }
}