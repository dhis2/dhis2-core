/*
 * Copyright (c) 2004-2004-2021, University of Oslo
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.commons.collections.SetUtils;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.dashboard.DashboardItem;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.mapping.Map;
import org.hisp.dhis.mapping.MapView;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.user.sharing.UserGroupAccess;
import org.hisp.dhis.visualization.Visualization;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

public class DashboardCascadeSharingTest
    extends CascadeSharingTest
{
    @Autowired
    private UserService _userService;

    @Autowired
    private AclService aclService;

    @Autowired
    private CascadeSharingService<Dashboard> dashboardCascadeSharingService;

    @Autowired
    private IdentifiableObjectManager objectManager;

    private UserGroup userGroupA;

    private User userA;

    private User userB;

    private Sharing sharingReadForUserA;

    private Sharing sharingReadForUserAB;

    private Sharing sharingReadWriteForUserB;

    private Sharing sharingUserGroupA;

    private Dashboard dashboard;

    @Override
    public void setUpTest()
    {
        userService = _userService;
        userA = createUser( 'A' );
        userB = createUser( 'B' );

        userGroupA = createUserGroup( 'A', SetUtils.EMPTY_SET );

        sharingReadForUserA = new Sharing();
        sharingReadForUserA.addUserAccess( new UserAccess( userA, AccessStringHelper.READ ) );

        sharingReadWriteForUserB = new Sharing();
        sharingReadWriteForUserB.addUserAccess( new UserAccess( userB, AccessStringHelper.READ_WRITE ) );

        sharingReadForUserAB = new Sharing();
        sharingReadForUserAB.addUserAccess( new UserAccess( userA, AccessStringHelper.READ ) );
        sharingReadForUserAB.addUserAccess( new UserAccess( userB, AccessStringHelper.READ ) );

        sharingUserGroupA = new Sharing();
        sharingUserGroupA.addUserGroupAccess( new UserGroupAccess( userGroupA, AccessStringHelper.READ ) );

        dashboard = new Dashboard();
        dashboard.setName( "dashboardA" );
        dashboard.setAutoFields();

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
    public void testCascadeShareVisualization()
    {
        DataElement dataElementA = createDEWithDefaultSharing( 'A' );
        objectManager.save( dataElementA );
        DataElement dataElementB = createDEWithDefaultSharing( 'B' );
        objectManager.save( dataElementB );

        Visualization visualizationA = createVisualization( 'A' );
        addDimensionItemToVisualizationRow( visualizationA, dataElementA.getUid(), DimensionItemType.DATA_ELEMENT );
        addDimensionItemToVisualizationColumn( visualizationA, dataElementB.getUid(), DimensionItemType.DATA_ELEMENT );
        visualizationA.setSharing( Sharing.builder().publicAccess( AccessStringHelper.DEFAULT ).build() );
        objectManager.save( visualizationA, false );

        DashboardItem dashboardItemA = createDashboardItem( "A" );
        dashboardItemA.setVisualization( visualizationA );
        objectManager.save( dashboardItemA, false );

        dashboard.getItems().clear();
        dashboard.getItems().add( dashboardItemA );
        dashboard.setSharing( sharingReadForUserAB );

        objectManager.save( dashboard, false );

        List<ErrorReport> errors = dashboardCascadeSharingService.cascadeSharing( dashboard,
            new CascadeSharingParameters() );
        assertEquals( 0, errors.size() );

        DataElement updatedDataElementA = objectManager.get( DataElement.class, dataElementA.getUid() );
        DataElement updatedDataElementB = objectManager.get( DataElement.class, dataElementB.getUid() );

        assertTrue( aclService.canRead( userA, visualizationA ) );
        assertTrue( aclService.canRead( userB, visualizationA ) );
        assertTrue( aclService.canRead( userA, updatedDataElementA ) );
        assertTrue( aclService.canRead( userB, updatedDataElementB ) );

    }

    @Test
    public void testCascadeShareVisualizationError()
    {
        DataElement dataElementA = createDataElement( 'A' );
        dataElementA.setSharing( Sharing.builder().publicAccess( AccessStringHelper.DEFAULT ).build() );
        objectManager.save( dataElementA, false );

        Visualization vzA = createVisualization( 'A' );
        vzA.setSharing( Sharing.builder().publicAccess( AccessStringHelper.DEFAULT ).build() );
        addDimensionItemToVisualizationRow( vzA, dataElementA.getUid(), DimensionItemType.DATA_ELEMENT );
        objectManager.save( vzA, false );

        DashboardItem dashboardItemA = createDashboardItem( "A" );
        dashboardItemA.setVisualization( vzA );

        objectManager.save( dashboardItemA, false );
        Sharing sharing = new Sharing();
        sharing.addUserAccess( new UserAccess( userB, AccessStringHelper.DEFAULT ) );

        dashboard.getItems().clear();
        dashboard.getItems().add( dashboardItemA );
        dashboard.setSharing( sharing );

        objectManager.save( dashboard, false );

        List<ErrorReport> errors = dashboardCascadeSharingService.cascadeSharing( dashboard,
            new CascadeSharingParameters() );
        assertEquals( 1, errors.size() );
        assertEquals( ErrorCode.E3019, errors.get( 0 ).getErrorCode() );

        assertFalse( aclService.canRead( userB, vzA ) );
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
    public void testCascadeShareMap()
    {
        MapView mapView = createMapView( "Test" );
        Map map = new Map();
        map.setName( "mapA" );
        map.setSharing( Sharing.builder().publicAccess( AccessStringHelper.DEFAULT ).build() );
        map.setMapViews( Lists.newArrayList( mapView ) );
        objectManager.save( map, false );

        DashboardItem dashboardItemA = createDashboardItem( "A" );
        dashboardItemA.setMap( map );

        dashboard.getItems().clear();
        dashboard.getItems().add( dashboardItemA );
        dashboard.setSharing( sharingReadForUserA );
        objectManager.save( dashboard, false );

        List<ErrorReport> errors = dashboardCascadeSharingService.cascadeSharing( dashboard,
            new CascadeSharingParameters() );
        assertEquals( 0, errors.size() );
        assertTrue( aclService.canRead( userA, dashboardItemA.getMap() ) );
        assertEquals( AccessStringHelper.READ,
            dashboardItemA.getMap().getSharing().getUsers().get( userA.getUid() ).getAccess() );
        assertFalse( aclService.canRead( userB, dashboardItemA.getMap() ) );
    }

    /**
     * Dashboard is shared to userB.
     * <p>
     * But userB's access is DEFAULT('--------')
     * <p>
     * Expected: return error with code E3019
     */
    @Test
    public void testCascadeShareMapError()
    {
        DataElement dataElementB = createDataElement( 'B' );
        dataElementB.setSharing( Sharing.builder().publicAccess( AccessStringHelper.DEFAULT ).build() );
        objectManager.save( dataElementB, false );

        MapView mapView = createMapView( "Test" );
        Map map = new Map();
        map.setName( "mapA" );
        map.setSharing( Sharing.builder().publicAccess( AccessStringHelper.DEFAULT ).build() );
        map.setMapViews( Lists.newArrayList( mapView ) );
        objectManager.save( map, false );
        objectManager.flush();

        DashboardItem dashboardItemA = createDashboardItem( "A" );
        dashboardItemA.setMap( map );

        Sharing sharing = new Sharing();
        sharing.addUserAccess( new UserAccess( userB, AccessStringHelper.DEFAULT ) );

        dashboard.getItems().clear();
        dashboard.getItems().add( dashboardItemA );
        dashboard.setSharing( sharing );
        objectManager.save( dashboard, false );

        List<ErrorReport> errors = dashboardCascadeSharingService
            .cascadeSharing( dashboard, new CascadeSharingParameters() );
        assertEquals( 1, errors.size() );
        assertEquals( ErrorCode.E3019, errors.get( 0 ).getErrorCode() );

        assertFalse( aclService.canRead( userB, dashboardItemA.getMap() ) );
    }
}
