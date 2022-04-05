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
package org.hisp.dhis.sharing;

import static org.hisp.dhis.security.acl.AccessStringHelper.DEFAULT;
import static org.hisp.dhis.security.acl.AccessStringHelper.READ;
import static org.hisp.dhis.security.acl.AccessStringHelper.READ_WRITE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.dashboard.DashboardItem;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.eventchart.EventChart;
import org.hisp.dhis.eventreport.EventReport;
import org.hisp.dhis.eventvisualization.EventVisualization;
import org.hisp.dhis.eventvisualization.EventVisualizationType;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.mapping.Map;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.schema.descriptors.DataElementSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.EventChartSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.EventReportSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.IndicatorSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.LegendSetSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.ProgramStageSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.TrackedEntityAttributeSchemaDescriptor;
import org.hisp.dhis.schema.descriptors.VisualizationSchemaDescriptor;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeDimension;
import org.hisp.dhis.trackedentity.TrackedEntityDataElementDimension;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.user.sharing.UserGroupAccess;
import org.hisp.dhis.visualization.Visualization;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DashboardCascadeSharingTest extends CascadeSharingTest
{

    @Autowired
    private UserService _userService;

    @Autowired
    private AclService aclService;

    @Autowired
    private CascadeSharingService cascadeSharingService;

    @Autowired
    private IdentifiableObjectManager objectManager;

    private UserGroup userGroupA;

    private User userA;

    private User userB;

    private Sharing sharingReadForUserA;

    private Sharing sharingReadForUserAB;

    private Sharing sharingReadWriteForUserB;

    private Sharing sharingUserGroupA;

    private Program programA;

    @Override
    public void setUpTest()
    {
        userService = _userService;
        userGroupA = createUserGroup( 'A', Collections.EMPTY_SET );
        objectManager.save( userGroupA );
        userA = makeUser( "A" );
        userA.getGroups().add( userGroupA );
        userService.addUser( userA );
        userB = makeUser( "B" );
        userService.addUser( userB );
        sharingReadForUserA = new Sharing( DEFAULT, new UserAccess( userA, READ ) );
        sharingReadWriteForUserB = new Sharing( DEFAULT, new UserAccess( userB, READ_WRITE ) );
        sharingReadForUserAB = new Sharing( DEFAULT, new UserAccess( userA, READ ), new UserAccess( userB, READ ) );
        sharingUserGroupA = new Sharing( DEFAULT, new UserGroupAccess( userGroupA, READ ) );
        programA = createProgram( 'A' );
        programA.setSharing( defaultSharing() );
        objectManager.save( programA, false );
        createAndInjectAdminUser();
    }

    /**
     * Dashboard has sharingUserA and visualizationA
     * <p>
     * visualizationA has dataElementA
     * <p>
     * Expected: visualizationA and dataElementA should be shared to userA
     */
    @Test
    void testCascadeShareVisualization()
    {
        DataElement dataElementA = createDEWithDefaultSharing( 'A' );
        objectManager.save( dataElementA );
        DataElement dataElementB = createDEWithDefaultSharing( 'B' );
        objectManager.save( dataElementB );
        Visualization visualizationA = createVisualization( 'A' );
        visualizationA.addDataDimensionItem( dataElementA );
        visualizationA.addDataDimensionItem( dataElementB );
        visualizationA.setSharing( Sharing.builder().publicAccess( DEFAULT ).build() );
        objectManager.save( visualizationA, false );
        Dashboard dashboard = createDashboardWithItem( "A", sharingReadForUserAB );
        dashboard.getItems().get( 0 ).setVisualization( visualizationA );
        objectManager.save( dashboard, false );
        CascadeSharingReport report = cascadeSharingService.cascadeSharing( dashboard, new CascadeSharingParameters() );
        assertEquals( 0, report.getErrorReports().size() );
        DataElement updatedDataElementA = objectManager.get( DataElement.class, dataElementA.getUid() );
        DataElement updatedDataElementB = objectManager.get( DataElement.class, dataElementB.getUid() );
        assertTrue( aclService.canRead( userA, visualizationA ) );
        assertTrue( aclService.canRead( userB, visualizationA ) );
        assertTrue( aclService.canRead( userA, updatedDataElementA ) );
        assertTrue( aclService.canRead( userB, updatedDataElementB ) );
    }

    /**
     * Dashboard has sharingUserA and eventVisualizationA
     * <p>
     * eventVisualizationA has dataElementA
     * <p>
     * Expected: eventVisualizationA and dataElementA should be shared to userA
     */
    @Test
    void testCascadeShareEventVisualization()
    {
        DataElement dataElementA = createDEWithDefaultSharing( 'A' );
        objectManager.save( dataElementA );
        DataElement dataElementB = createDEWithDefaultSharing( 'B' );
        objectManager.save( dataElementB );
        Program program = createProgram( 'Y', null, null );
        objectManager.save( program );
        EventVisualization eventVisualizationA = createEventVisualization( 'A', program );
        eventVisualizationA.addDataDimensionItem( dataElementA );
        eventVisualizationA.addDataDimensionItem( dataElementB );
        eventVisualizationA.setSharing( Sharing.builder().publicAccess( DEFAULT ).build() );
        objectManager.save( eventVisualizationA, false );
        Dashboard dashboard = createDashboardWithItem( "A", sharingReadForUserAB );
        dashboard.getItems().get( 0 ).setEventVisualization( eventVisualizationA );
        objectManager.save( dashboard, false );
        CascadeSharingReport report = cascadeSharingService.cascadeSharing( dashboard, new CascadeSharingParameters() );
        assertEquals( 0, report.getErrorReports().size() );
        DataElement updatedDataElementA = objectManager.get( DataElement.class, dataElementA.getUid() );
        DataElement updatedDataElementB = objectManager.get( DataElement.class, dataElementB.getUid() );
        assertTrue( aclService.canRead( userA, eventVisualizationA ) );
        assertTrue( aclService.canRead( userB, eventVisualizationA ) );
        assertTrue( aclService.canRead( userA, updatedDataElementA ) );
        assertTrue( aclService.canRead( userB, updatedDataElementB ) );
    }

    @Test
    void testCascadeShareVisualizationError()
    {
        DataElement dataElementA = createDataElement( 'A' );
        dataElementA.setSharing( Sharing.builder().publicAccess( DEFAULT ).build() );
        objectManager.save( dataElementA, false );
        Visualization vzA = createVisualization( 'A' );
        vzA.setSharing( Sharing.builder().publicAccess( DEFAULT ).build() );
        vzA.addDataDimensionItem( dataElementA );
        objectManager.save( vzA, false );
        Sharing sharing = new Sharing();
        sharing.setPublicAccess( DEFAULT );
        sharing.addUserAccess( new UserAccess( userB, DEFAULT ) );
        Dashboard dashboard = createDashboardWithItem( "A", sharing );
        dashboard.getItems().get( 0 ).setVisualization( vzA );
        objectManager.save( dashboard, false );
        CascadeSharingReport report = cascadeSharingService.cascadeSharing( dashboard, new CascadeSharingParameters() );
        assertEquals( 0, report.getUpdateObjects().size() );
        assertFalse( aclService.canRead( userB, vzA ) );
        assertFalse( aclService.canRead( userB, dataElementA ) );
    }

    @Test
    void testCascadeShareEventVisualizationError()
    {
        DataElement dataElementA = createDataElement( 'A' );
        dataElementA.setSharing( Sharing.builder().publicAccess( DEFAULT ).build() );
        objectManager.save( dataElementA, false );
        Program program = createProgram( 'Y', null, null );
        objectManager.save( program );
        EventVisualization eventVisualizationA = createEventVisualization( 'A', program );
        eventVisualizationA.setSharing( Sharing.builder().publicAccess( DEFAULT ).build() );
        eventVisualizationA.addDataDimensionItem( dataElementA );
        objectManager.save( eventVisualizationA, false );
        Sharing sharing = new Sharing();
        sharing.setPublicAccess( DEFAULT );
        sharing.addUserAccess( new UserAccess( userB, DEFAULT ) );
        Dashboard dashboard = createDashboardWithItem( "A", sharing );
        dashboard.getItems().get( 0 ).setEventVisualization( eventVisualizationA );
        objectManager.save( dashboard, false );
        CascadeSharingReport report = cascadeSharingService.cascadeSharing( dashboard, new CascadeSharingParameters() );
        assertEquals( 0, report.getUpdateObjects().size() );
        assertFalse( aclService.canRead( userB, eventVisualizationA ) );
        assertFalse( aclService.canRead( userB, dataElementA ) );
    }

    /**
     * Dashboard is shared to userA
     * <p>
     * Dashboard has a MapA
     * <p>
     * Expected: MapA will be shared to userA
     */
    @Test
    void testCascadeShareMap()
    {
        Map map = createMap( "A" );
        map.setSharing( Sharing.builder().publicAccess( DEFAULT ).build() );
        objectManager.save( map, false );
        Dashboard dashboard = createDashboardWithItem( "A", sharingReadForUserA );
        dashboard.getItems().get( 0 ).setMap( map );
        objectManager.save( dashboard, false );
        CascadeSharingReport report = cascadeSharingService.cascadeSharing( dashboard, new CascadeSharingParameters() );
        assertEquals( 0, report.getErrorReports().size() );
        assertTrue( aclService.canRead( userA, dashboard.getItems().get( 0 ).getMap() ) );
        assertEquals( READ,
            dashboard.getItems().get( 0 ).getMap().getSharing().getUsers().get( userA.getUid() ).getAccess() );
        assertFalse( aclService.canRead( userB, dashboard.getItems().get( 0 ).getMap() ) );
    }

    /**
     * Dashboard has publicAccess READ and not shared to any User or UserGroup.
     * <p>
     * Expected cascade sharing for PublicAccess is not supported, so user can't
     * access dashboardItem's objects.
     */
    @Test
    void testCascadeSharePublicAccess()
    {
        Map map = createMap( "A" );
        map.setSharing( Sharing.builder().publicAccess( DEFAULT ).build() );
        objectManager.save( map, false );
        Dashboard dashboard = createDashboardWithItem( "dashboardA", Sharing.builder().publicAccess( READ ).build() );
        dashboard.getItems().get( 0 ).setMap( map );
        objectManager.save( dashboard, false );
        CascadeSharingReport report = cascadeSharingService.cascadeSharing( dashboard, new CascadeSharingParameters() );
        assertEquals( 0, report.getErrorReports().size() );
        assertFalse( aclService.canRead( userA, dashboard.getItems().get( 0 ).getMap() ) );
        assertFalse( aclService.canRead( userB, dashboard.getItems().get( 0 ).getMap() ) );
    }

    /**
     * Dashboard is shared to userB.
     * <p>
     * But userB's access is DEFAULT('--------')
     * <p>
     * Expected: no objects being updated.
     */
    @Test
    void testCascadeShareMapError()
    {
        DataElement dataElementB = createDataElement( 'B' );
        dataElementB.setSharing( Sharing.builder().publicAccess( DEFAULT ).build() );
        objectManager.save( dataElementB, false );
        Map map = createMap( "A" );
        map.setSharing( Sharing.builder().publicAccess( DEFAULT ).build() );
        objectManager.save( map, false );
        objectManager.flush();
        Sharing sharing = new Sharing();
        sharing.setPublicAccess( DEFAULT );
        sharing.addUserAccess( new UserAccess( userB, DEFAULT ) );
        Dashboard dashboard = createDashboardWithItem( "dashboardA", sharing );
        dashboard.getItems().get( 0 ).setMap( map );
        objectManager.save( dashboard, false );
        CascadeSharingReport report = cascadeSharingService.cascadeSharing( dashboard, new CascadeSharingParameters() );
        assertEquals( 0, report.getUpdateObjects().size() );
        assertFalse( aclService.canRead( userB, dashboard.getItems().get( 0 ).getMap() ) );
    }

    @Test
    void testCascadeShareEventReport()
    {
        DataElement deA = createDataElement( 'A' );
        deA.setSharing( defaultSharing() );
        objectManager.save( deA, false );
        LegendSet lsA = createLegendSet( 'A' );
        lsA.setSharing( defaultSharing() );
        objectManager.save( lsA, false );
        ProgramStage psA = createProgramStage( 'A', 1 );
        psA.setSharing( defaultSharing() );
        objectManager.save( psA, false );
        TrackedEntityDataElementDimension teDeDim = new TrackedEntityDataElementDimension( deA, lsA, psA, "EQ:1" );
        EventReport eventReport = createEventReport( "A", programA );
        eventReport.addTrackedEntityDataElementDimension( teDeDim );
        eventReport.setSharing( defaultSharing() );
        eventReport.setType( EventVisualizationType.LINE_LIST );
        objectManager.save( eventReport, false );
        // Add eventReport to dashboard
        Dashboard dashboard = createDashboardWithItem( "dashboardA", sharingReadForUserA );
        dashboard.getItems().get( 0 ).setEventReport( eventReport );
        objectManager.save( dashboard, false );
        CascadeSharingReport report = cascadeSharingService.cascadeSharing( dashboard,
            CascadeSharingParameters.builder().build() );
        assertEquals( 0, report.getErrorReports().size() );
        assertEquals( 4, report.getUpdateObjects().size() );
        assertEquals( 1, report.getUpdateObjects().get( DataElementSchemaDescriptor.PLURAL ).size() );
        assertEquals( 1, report.getUpdateObjects().get( LegendSetSchemaDescriptor.PLURAL ).size() );
        assertEquals( 1, report.getUpdateObjects().get( ProgramStageSchemaDescriptor.PLURAL ).size() );
        assertEquals( 1, report.getUpdateObjects().get( EventReportSchemaDescriptor.PLURAL ).size() );
        assertTrue( aclService.canRead( userA, eventReport ) );
        assertTrue( aclService.canRead( userA, deA ) );
        assertTrue( aclService.canRead( userA, lsA ) );
        assertTrue( aclService.canRead( userA, psA ) );
        assertFalse( aclService.canRead( userB, eventReport ) );
        assertFalse( aclService.canRead( userB, deA ) );
        assertFalse( aclService.canRead( userB, lsA ) );
        assertFalse( aclService.canRead( userB, psA ) );
    }

    @Test
    void cascadeShareEventChart()
    {
        LegendSet legendSet = createLegendSet( 'A' );
        legendSet.setSharing( defaultSharing() );
        objectManager.save( legendSet, false );
        TrackedEntityAttribute trackedEntityAttribute = createTrackedEntityAttribute( 'A' );
        trackedEntityAttribute.setSharing( defaultSharing() );
        objectManager.save( trackedEntityAttribute, false );
        assertFalse( aclService.canRead( userA, legendSet ) );
        assertFalse( aclService.canRead( userA, trackedEntityAttribute ) );
        TrackedEntityAttributeDimension attributeDimension = new TrackedEntityAttributeDimension();
        attributeDimension.setLegendSet( legendSet );
        attributeDimension.setAttribute( trackedEntityAttribute );
        EventChart eventChart = createEventChart( "A", programA );
        eventChart.setAttributeValueDimension( trackedEntityAttribute );
        eventChart.getAttributeDimensions().add( attributeDimension );
        eventChart.setSharing( defaultSharing() );
        objectManager.save( eventChart, false );
        Dashboard dashboard = createDashboardWithItem( "dashboardA", sharingReadForUserA );
        dashboard.getItems().get( 0 ).setEventChart( eventChart );
        objectManager.save( dashboard, false );
        CascadeSharingReport report = cascadeSharingService.cascadeSharing( dashboard,
            CascadeSharingParameters.builder().build() );
        assertEquals( 0, report.getErrorReports().size() );
        assertEquals( 3, report.getUpdateObjects().size() );
        assertEquals( 1, report.getUpdateObjects().get( LegendSetSchemaDescriptor.PLURAL ).size() );
        assertEquals( 1, report.getUpdateObjects().get( TrackedEntityAttributeSchemaDescriptor.PLURAL ).size() );
        assertEquals( 1, report.getUpdateObjects().get( EventChartSchemaDescriptor.PLURAL ).size() );
        assertTrue( aclService.canRead( userA, eventChart ) );
        assertTrue( aclService.canRead( userA, legendSet ) );
        assertTrue( aclService.canRead( userA, trackedEntityAttribute ) );
        assertFalse( aclService.canRead( userB, eventChart ) );
        assertFalse( aclService.canRead( userB, legendSet ) );
        assertFalse( aclService.canRead( userB, trackedEntityAttribute ) );
    }

    @Test
    void testCascadeIndicatorAndDataElement()
    {
        IndicatorType indicatorType = createIndicatorType( 'A' );
        objectManager.save( indicatorType );
        Indicator indicatorA = createIndicator( 'A', indicatorType );
        indicatorA.setSharing( Sharing.builder().publicAccess( DEFAULT ).build() );
        objectManager.save( indicatorA, false );
        DataElement dataElementA = createDEWithDefaultSharing( 'A' );
        objectManager.save( dataElementA, false );
        Visualization visualizationA = createVisualization( 'A' );
        visualizationA.addDataDimensionItem( dataElementA );
        visualizationA.addDataDimensionItem( indicatorA );
        visualizationA.setSharing( defaultSharing() );
        objectManager.save( visualizationA, false );
        Dashboard dashboard = createDashboardWithItem( "a", sharingReadForUserA );
        dashboard.getItems().get( 0 ).setVisualization( visualizationA );
        objectManager.save( dashboard, false );
        CascadeSharingReport report = cascadeSharingService.cascadeSharing( dashboard, new CascadeSharingParameters() );
        assertEquals( 0, report.getErrorReports().size() );
        assertEquals( 3, report.getUpdateObjects().size() );
        assertEquals( 1, report.getUpdateObjects().get( DataElementSchemaDescriptor.PLURAL ).size() );
        assertEquals( 1, report.getUpdateObjects().get( IndicatorSchemaDescriptor.PLURAL ).size() );
        assertEquals( 1, report.getUpdateObjects().get( VisualizationSchemaDescriptor.PLURAL ).size() );
        DataElement updatedDataElementA = objectManager.get( DataElement.class, dataElementA.getUid() );
        Indicator updatedIndicatorA = objectManager.get( Indicator.class, indicatorA.getUid() );
        assertTrue( aclService.canRead( userA, visualizationA ) );
        assertTrue( aclService.canRead( userA, updatedDataElementA ) );
        assertTrue( aclService.canRead( userA, updatedIndicatorA ) );
        assertFalse( aclService.canRead( userB, visualizationA ) );
        assertFalse( aclService.canRead( userB, updatedDataElementA ) );
        assertFalse( aclService.canRead( userB, updatedIndicatorA ) );
    }

    @Test
    void testDryRunTrue()
    {
        Map map = createMap( "A" );
        map.setSharing( Sharing.builder().publicAccess( DEFAULT ).build() );
        objectManager.save( map, false );
        Dashboard dashboard = createDashboardWithItem( "A", sharingReadForUserA );
        dashboard.getItems().get( 0 ).setMap( map );
        objectManager.save( dashboard, false );
        CascadeSharingReport report = cascadeSharingService.cascadeSharing( dashboard,
            CascadeSharingParameters.builder().dryRun( true ).build() );
        assertEquals( 0, report.getErrorReports().size() );
        assertEquals( 1, report.getUpdateObjects().size() );
        assertFalse( aclService.canRead( userA, dashboard.getItems().get( 0 ).getMap() ) );
        assertFalse( aclService.canRead( userB, dashboard.getItems().get( 0 ).getMap() ) );
    }

    @Test
    void testDryRunFalse()
    {
        Map map = createMap( "A" );
        map.setSharing( Sharing.builder().publicAccess( DEFAULT ).build() );
        objectManager.save( map, false );
        Dashboard dashboard = createDashboardWithItem( "A", sharingReadForUserA );
        dashboard.getItems().get( 0 ).setMap( map );
        objectManager.save( dashboard, false );
        CascadeSharingReport report = cascadeSharingService.cascadeSharing( dashboard,
            CascadeSharingParameters.builder().dryRun( false ).build() );
        assertEquals( 0, report.getErrorReports().size() );
        assertEquals( 1, report.getUpdateObjects().size() );
        assertTrue( aclService.canRead( userA, dashboard.getItems().get( 0 ).getMap() ) );
        assertFalse( aclService.canRead( userB, dashboard.getItems().get( 0 ).getMap() ) );
    }

    @Test
    void testAtomicTrue()
    {
        Map mapA = createMap( "A" );
        mapA.setSharing( sharingReadWriteForUserB );
        objectManager.save( mapA, false );
        assertFalse( aclService.canRead( userA, mapA ) );
        Map mapB = createMap( "A" );
        mapB.setSharing( defaultSharing() );
        objectManager.save( mapB, false );
        DashboardItem itemB = createDashboardItem( "B" );
        itemB.setMap( mapB );
        DashboardItem itemA = createDashboardItem( "A" );
        itemA.setMap( mapA );
        Dashboard dashboard = createDashboard( "A", sharingReadForUserA );
        dashboard.getItems().add( itemA );
        dashboard.getItems().add( itemB );
        objectManager.save( dashboard, false );
        assertFalse( aclService.canRead( userA, mapA ) );
        CascadeSharingReport report = cascadeSharingService.cascadeSharing( dashboard,
            CascadeSharingParameters.builder().atomic( true ).user( userB ).build() );
        assertEquals( 1, report.getErrorReports().size() );
        assertEquals( 0, report.getUpdateObjects().size() );
        assertFalse( aclService.canRead( userA, mapA ) );
        assertFalse( aclService.canRead( userA, mapB ) );
    }

    @Test
    void testAtomicFalse()
    {
        Map mapA = createMap( "A" );
        mapA.setSharing( sharingReadWriteForUserB );
        objectManager.save( mapA, false );
        Map mapB = createMap( "A" );
        mapB.setSharing( defaultSharing() );
        objectManager.save( mapB, false );
        DashboardItem itemB = createDashboardItem( "B" );
        itemB.setMap( mapB );
        Dashboard dashboard = createDashboardWithItem( "A", sharingReadForUserA );
        dashboard.getItems().get( 0 ).setMap( mapA );
        dashboard.getItems().add( itemB );
        objectManager.save( dashboard, false );
        CascadeSharingReport report = cascadeSharingService.cascadeSharing( dashboard,
            CascadeSharingParameters.builder().atomic( false ).user( userB ).build() );
        assertEquals( 1, report.getErrorReports().size() );
        assertEquals( 1, report.getUpdateObjects().size() );
        assertTrue( aclService.canRead( userA, mapA ) );
        assertFalse( aclService.canRead( userA, mapB ) );
    }

    @Test
    void testUserGroup()
    {
        Map mapA = createMap( "A" );
        mapA.setSharing( defaultSharing() );
        objectManager.save( mapA, false );
        Map mapB = createMap( "A" );
        mapB.setSharing( defaultSharing() );
        objectManager.save( mapB, false );
        DashboardItem itemA = createDashboardItem( "A" );
        itemA.setMap( mapA );
        DashboardItem itemB = createDashboardItem( "B" );
        itemB.setMap( mapB );
        Dashboard dashboard = createDashboard( "A", sharingUserGroupA );
        dashboard.getItems().add( itemB );
        dashboard.getItems().add( itemA );
        objectManager.save( dashboard, false );
        CascadeSharingReport report = cascadeSharingService.cascadeSharing( dashboard,
            CascadeSharingParameters.builder().build() );
        assertEquals( 0, report.getErrorReports().size() );
        assertEquals( 1, report.getUpdateObjects().size() );
        assertTrue( aclService.canRead( userA, mapA ) );
        assertTrue( aclService.canRead( userA, mapB ) );
    }
}
