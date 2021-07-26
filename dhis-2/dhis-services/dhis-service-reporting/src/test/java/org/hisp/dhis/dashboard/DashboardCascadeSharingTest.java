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
package org.hisp.dhis.dashboard;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.commons.collections.SetUtils;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dashboard.impl.DashboardCascadeSharingService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.mapping.Map;
import org.hisp.dhis.mapping.MapView;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.sharing.CascadeSharingParameters;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.user.sharing.UserGroupAccess;
import org.hisp.dhis.visualization.Visualization;
import org.hisp.dhis.visualization.VisualizationService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

public class DashboardCascadeSharingTest
    extends DhisSpringTest
{
    @Autowired
    private VisualizationService visualizationService;

    @Autowired
    private UserService _userService;

    @Autowired
    private AclService aclService;

    @Autowired
    private DashboardCascadeSharingService dashboardCascadeSharingService;

    @Autowired
    private IdentifiableObjectManager objectManager;

    private UserGroup userGroupA;

    private User userA;

    private User userB;

    private Sharing sharingUserA;

    private Sharing sharingUserGroupA;

    private DataElement dataElementA;

    private Dashboard dashboard;

    @Override
    public void setUpTest()
    {
        userService = _userService;
        userA = createUser( 'A' );
        userB = createUser( 'B' );

        userGroupA = createUserGroup( 'A', SetUtils.EMPTY_SET );

        sharingUserA = new Sharing();
        sharingUserA.addUserAccess( new UserAccess( userA, AccessStringHelper.READ ) );

        sharingUserGroupA = new Sharing();
        sharingUserGroupA.addUserGroupAccess( new UserGroupAccess( userGroupA, AccessStringHelper.READ ) );

        dataElementA = createDataElement( 'A' );
        dataElementA.setSharing( Sharing.builder().publicAccess( AccessStringHelper.DEFAULT ).build() );

        dashboard = new Dashboard();
        dashboard.setName( "dashboardA" );
        dashboard.setAutoFields();

        createAndInjectAdminUser();
    }

    /**
     * Dashboard has sharingUserA Dashboard has visuallizationA visuallizationA
     * has dataElementA Expected: vzA and dataElementA should be shared to userA
     */
    public void testCascadeShareVisualization()
    {
        Visualization vzA = createVisualization( 'A' );
        addDimensionItemToVisualization( vzA, dataElementA.getUid() );
        visualizationService.save( vzA );

        DashboardItem dashboardItemA = createDashboardItem( "A" );
        dashboardItemA.setVisualization( vzA );

        objectManager.save( dashboardItemA, false );

        dashboard.getItems().add( dashboardItemA );
        dashboard.setSharing( sharingUserA );

        objectManager.save( dashboard, false );

        dashboardCascadeSharingService.cascadeSharing( dashboard, new CascadeSharingParameters() );

        assertTrue( aclService.canRead( userA, dashboardItemA ) );
    }

    /**
     * Dashboard is shared to userA
     * <p>
     * Dashboard has a MapA
     * <p>
     * Expected: Map will be shared to userA
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
        dashboard.setSharing( sharingUserA );
        objectManager.save( dashboard, false );

        dashboardCascadeSharingService.cascadeSharing( dashboard, new CascadeSharingParameters() );

        assertTrue( aclService.canRead( userA, dashboardItemA ) );
        assertTrue( aclService.canRead( userA, dashboardItemA.getMap() ) );
        assertFalse( aclService.canRead( userB, dashboardItemA.getMap() ) );
    }

    private void addDimensionItemToVisualization( Visualization visualization, final String dimensionItem )
    {
        final List<String> rowsDimensions = asList( "dx" );
        final List<DimensionalItemObject> dimensionalItemObjects = asList(
            baseDimensionalItemObjectStub( dimensionItem ) );

        visualization.setRowDimensions( rowsDimensions );
        visualization.setGridRows( asList( dimensionalItemObjects ) );
    }

    private BaseDimensionalItemObject baseDimensionalItemObjectStub( final String dimensionItem )
    {
        final BaseDimensionalItemObject baseDimensionalItemObject = new BaseDimensionalItemObject( dimensionItem );
        baseDimensionalItemObject.setDescription( "display " + dimensionItem );
        return baseDimensionalItemObject;
    }

    private DashboardItem createDashboardItem( String name )
    {
        DashboardItem dashboardItem = new DashboardItem();
        dashboardItem.setName( "dashboardItemA" );
        dashboardItem.setAutoFields();
        return dashboardItem;
    }
}
