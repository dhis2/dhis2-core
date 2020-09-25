package org.hisp.dhis.dxf2.metadata;

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
 */

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.xpath.XPathExpressionException;

import org.hibernate.MappingException;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.dataset.Section;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleMode;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.node.NodeService;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageSection;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.render.DeviceRenderTypeMap;
import org.hisp.dhis.render.RenderDevice;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.render.type.SectionRenderingObject;
import org.hisp.dhis.render.type.SectionRenderingType;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.visualization.Visualization;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import com.google.common.collect.Sets;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class MetadataImportServiceTest extends DhisSpringTest
{
    @Autowired
    private MetadataImportService importService;

    @Autowired
    private MetadataExportService exportService;

    @Autowired
    private RenderService _renderService;

    @Autowired
    private UserService _userService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private SchemaService schemaService;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private NodeService nodeService;

    @Override
    protected void setUpTest()
        throws Exception
    {
        renderService = _renderService;
        userService = _userService;
    }

    @Test
    public void testCorrectStatusOnImportNoErrors()
        throws IOException
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
    public void testCorrectStatusOnImportErrors()
        throws IOException
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
    public void testCorrectStatusOnImportErrorsATOMIC()
        throws IOException
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
    public void testImportWithAccessObjects()
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
    public void testImportWithSkipSharingIsTrue()
        throws IOException
    {
        User user = createUser( "A", "ALL" );
        manager.save( user );

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/dataset_with_accesses_skipSharing.json" ).getInputStream(),
            RenderFormat.JSON );

        MetadataImportParams params = new MetadataImportParams();
        params.setImportMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setSkipSharing( true );
        params.setObjects( metadata );
        params.setUser( user );

        ImportReport report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );

        metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/dataset_with_accesses_update_skipSharing.json" ).getInputStream(),
            RenderFormat.JSON );

        params = new MetadataImportParams();
        params.setImportMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.UPDATE );
        params.setSkipSharing( true );
        params.setObjects( metadata );
        params.setUser( user );

        report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );
    }

    @Test( expected = MappingException.class )
    public void testImportNonExistingEntityObject()
        throws IOException
    {
        User user = createUser( 'A' );
        manager.save( user );

        UserGroup userGroup = createUserGroup( 'A', Sets.newHashSet( user ) );
        manager.save( userGroup );

        userGroup = manager.get( UserGroup.class, "ugabcdefghA" );
        assertNotNull( userGroup );

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/favorites/metadata_chart_with_accesses.json" ).getInputStream(),
            RenderFormat.JSON );

        MetadataImportParams params = new MetadataImportParams();
        params.setImportMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );

        importService.importMetadata( params );

        // Should not get to this point.
        fail( "The exception org.hibernate.MappingException was expected." );
    }

    @Test
    public void testImportEmbeddedObjectWithSkipSharingIsTrue()
        throws IOException
    {
        User user = createUser( 'A' );
        manager.save( user );

        UserGroup userGroup = createUserGroup( 'A', Sets.newHashSet( user ) );
        manager.save( userGroup );

        userGroup = manager.get( UserGroup.class, "ugabcdefghA" );
        assertNotNull( userGroup );

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/favorites/metadata_visualization_with_accesses.json" ).getInputStream(),
            RenderFormat.JSON );

        MetadataImportParams params = new MetadataImportParams();
        params.setImportMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );

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
        dataElementOperandVisualization.getDataDimensionItems()
            .stream()
            .forEach( item -> assertNotNull( item.getDataElementOperand() ) );

        metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/favorites/metadata_visualization_with_accesses_update.json" ).getInputStream(),
            RenderFormat.JSON );

        params = new MetadataImportParams();
        params.setImportMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.UPDATE );
        params.setObjects( metadata );
        params.setSkipSharing( true );

        report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );

        visualization = manager.get( Visualization.class, "gyYXi0rXAIc" );
        assertNotNull( visualization );
        assertEquals( 1, visualization.getUserGroupAccesses().size() );
        assertEquals( 1, visualization.getUserAccesses().size() );
        assertEquals( user.getUid(), visualization.getUserAccesses().iterator().next().getUserUid() );
        assertEquals( userGroup.getUid(), visualization.getUserGroupAccesses().iterator().next().getUserGroupUid() );
    }

    @Test
    public void testImportEmbeddedObjectWithSkipSharingIsFalse()
        throws IOException
    {

        User user = createUser( 'A' );
        manager.save( user );

        UserGroup userGroup = createUserGroup( 'A', Sets.newHashSet( user ) );
        manager.save( userGroup );

        userGroup = manager.get( UserGroup.class, "ugabcdefghA" );
        assertNotNull( userGroup );

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/favorites/metadata_visualization_with_accesses.json" ).getInputStream(),
            RenderFormat.JSON );

        MetadataImportParams params = new MetadataImportParams();
        params.setImportMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );

        ImportReport report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );

        Visualization visualization = manager.get( Visualization.class, "gyYXi0rXAIc" );
        assertNotNull( visualization );
        assertEquals( 1, visualization.getUserGroupAccesses().size() );
        assertEquals( 1, visualization.getUserAccesses().size() );
        assertEquals( user.getUid(), visualization.getUserAccesses().iterator().next().getUserUid() );
        assertEquals( userGroup.getUid(), visualization.getUserGroupAccesses().iterator().next().getUserGroupUid() );

        metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/favorites/metadata_visualization_with_accesses_update.json" ).getInputStream(),
            RenderFormat.JSON );

        params = new MetadataImportParams();
        params.setImportMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.UPDATE );
        params.setObjects( metadata );
        params.setSkipSharing( false );

        report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );

        visualization = manager.get( Visualization.class, "gyYXi0rXAIc" );
        assertNotNull( visualization );
        assertEquals( 0, visualization.getUserGroupAccesses().size() );
        assertEquals( 0, visualization.getUserAccesses().size() );
    }

    @Test
    public void testImportProgramWithProgramStageSections()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/program_noreg_sections.json" ).getInputStream(), RenderFormat.JSON );

        MetadataImportParams params = new MetadataImportParams();
        params.setImportMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );

        ImportReport report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );

        Program program = manager.get( Program.class, "s5uvS0Q7jnX" );

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
    public void testMetadataSyncWithDeletedDataSetSection()
        throws IOException
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

        assertEquals( 1, dataset.getSections().size() );

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

        assertEquals( true, dataset.getSections().isEmpty() );

    }

    @Test
    public void testMetadataImportWithDeletedDataElements()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/dataset_with_sections_and_data_elements.json" ).getInputStream(),
            RenderFormat.JSON );

        MetadataImportParams params = new MetadataImportParams();
        params.setImportMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE_AND_UPDATE );
        params.setObjects( metadata );

        ImportReport report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );

        DataSet dataset = dataSetService.getDataSet( "em8Bg4LCr5k" );

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
        params.setImportMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE_AND_UPDATE );
        params.setObjects( metadata );
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
    public void testUpdateUserGroupWithoutCreatedUserProperty()
        throws IOException
    {
        User userA = createUser( "A", "ALL" );
        userService.addUser( userA );

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService
            .fromMetadata( new ClassPathResource( "dxf2/usergroups.json" ).getInputStream(), RenderFormat.JSON );

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

        metadata = renderService.fromMetadata( new ClassPathResource( "dxf2/usergroups_update.json" ).getInputStream(),
            RenderFormat.JSON );

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
    public void testSerializeDeviceRenderTypeMap()
        throws IOException,
        XPathExpressionException
    {
        Metadata metadata = renderService.fromXml(
            new ClassPathResource( "dxf2/programstagesection_with_deps.xml" ).getInputStream(), Metadata.class );

        MetadataImportParams params = new MetadataImportParams();
        params.setImportMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE_AND_UPDATE );
        params.addMetadata( schemaService.getMetadataSchemas(), metadata );

        ImportReport report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );

        ProgramStageSection programStageSection = manager.get( ProgramStageSection.class, "e99B1JXVMMQ" );
        assertNotNull( programStageSection );
        assertEquals( 2, programStageSection.getRenderType().size() );
        DeviceRenderTypeMap<SectionRenderingObject> renderingType = programStageSection.getRenderType();

        SectionRenderingObject renderDevice1 = renderingType.get( RenderDevice.MOBILE );
        SectionRenderingObject renderDevice2 = renderingType.get( RenderDevice.DESKTOP );

        assertEquals( SectionRenderingType.SEQUENTIAL, renderDevice1.getType() );
        assertEquals( SectionRenderingType.LISTING, renderDevice2.getType() );

        MetadataExportParams exportParams = new MetadataExportParams();
        exportParams.addQuery( Query.from( schemaService.getSchema( ProgramStageSection.class ) ) );

        RootNode rootNode = exportService.getMetadataAsNode( exportParams );

        OutputStream outputStream = new ByteArrayOutputStream();

        nodeService.serialize( rootNode, "application/xml", outputStream );
        assertEquals( "1", xpathTest( "count(//d:programStageSection)", outputStream.toString() ) );
        assertEquals( "SEQUENTIAL", xpathTest( "//d:MOBILE/@type", outputStream.toString() ) );
        assertEquals( "LISTING", xpathTest( "//d:DESKTOP/@type", outputStream.toString() ) );
    }
}