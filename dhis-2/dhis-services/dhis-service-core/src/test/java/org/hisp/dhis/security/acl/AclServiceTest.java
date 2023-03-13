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
package org.hisp.dhis.security.acl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;

import org.hisp.dhis.TransactionalIntegrationTest;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.eventchart.EventChart;
import org.hisp.dhis.eventreport.EventReport;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.mapping.Map;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.query.JpaQueryUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAuthorityGroup;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.user.sharing.UserGroupAccess;
import org.hisp.dhis.visualization.Visualization;
import org.hisp.dhis.visualization.VisualizationType;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import com.google.common.collect.Sets;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class AclServiceTest extends TransactionalIntegrationTest
{
    @Autowired
    private AclService aclService;

    @Autowired
    private UserService _userService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public boolean emptyDatabaseAfterTest()
    {
        return true;
    }

    @Override
    protected void setUpTest()
        throws Exception
    {
        userService = _userService;
    }

    @Test
    public void testUpdateObjectWithPublicRWFail()
    {
        User user = createAdminUser( "F_OPTIONSET_PUBLIC_ADD" );
        DataElement dataElement = createDataElement( 'A' );
        dataElement.setPublicAccess( AccessStringHelper.READ_WRITE );

        assertFalse( aclService.canUpdate( user, dataElement ) );
    }

    @Test
    public void testUpdateObjectWithPublicWFail()
    {
        User user = createAdminUser( "F_OPTIONSET_PUBLIC_ADD" );
        DataElement dataElement = createDataElement( 'A' );
        dataElement.setPublicAccess( AccessStringHelper.WRITE );

        assertFalse( aclService.canUpdate( user, dataElement ) );
    }

    @Test
    public void testUpdateObjectWithPublicRFail()
    {
        User user = createAdminUser( "F_OPTIONSET_PUBLIC_ADD" );
        DataElement dataElement = createDataElement( 'A' );
        dataElement.setPublicAccess( AccessStringHelper.READ );

        assertFalse( aclService.canUpdate( user, dataElement ) );
    }

    @Test
    public void testUpdateObjectWithPublicRUserOwner()
    {
        User user = createAdminUser( "F_DATAELEMENT_PUBLIC_ADD" );
        DataElement dataElement = createDataElement( 'A' );
        dataElement.setCreatedBy( user );
        dataElement.getSharing().setOwner( user );
        dataElement.setPublicAccess( AccessStringHelper.READ );

        assertTrue( aclService.canUpdate( user, dataElement ) );
    }

    @Test
    public void testUpdateObjectWithPublicRWSuccessPublic()
    {
        User user = createAdminUser( "F_DATAELEMENT_PUBLIC_ADD" );
        DataElement dataElement = createDataElement( 'A' );
        dataElement.setPublicAccess( AccessStringHelper.READ_WRITE );

        assertTrue( aclService.canUpdate( user, dataElement ) );
    }

    @Test
    public void testUpdateObjectWithPublicRWSuccessPrivate1()
    {
        User user = createAdminUser( "F_DATAELEMENT_PRIVATE_ADD" );
        DataElement dataElement = createDataElement( 'A' );
        dataElement.setCreatedBy( user );
        dataElement.getSharing().setOwner( user );
        dataElement.setPublicAccess( AccessStringHelper.READ_WRITE );

        assertFalse( aclService.canUpdate( user, dataElement ) );
    }

    @Test
    public void testUpdateObjectWithPublicRWSuccessPrivate2()
    {
        User user = createAdminUser( "F_DATAELEMENT_PRIVATE_ADD" );
        DataElement dataElement = createDataElement( 'A' );
        dataElement.setPublicAccess( AccessStringHelper.READ_WRITE );

        assertFalse( aclService.canUpdate( user, dataElement ) );
    }

    @Test
    public void testCanCreatePrivatePublicDashboard()
    {
        User user = createAdminUser( "F_DATAELEMENT_PRIVATE_ADD" );

        assertFalse( aclService.canMakeClassPublic( user, Dashboard.class ) );
        assertTrue( aclService.canMakeClassPrivate( user, Dashboard.class ) );
    }

    @Test
    public void testCanUpdatePrivateDashboard()
    {
        User user = createAdminUser( "F_DATAELEMENT_PRIVATE_ADD" );

        Dashboard dashboard = new Dashboard( "Dashboard" );
        dashboard.setAutoFields();
        dashboard.setCreatedBy( user );
        dashboard.getSharing().setOwner( user );
        dashboard.setPublicAccess( AccessStringHelper.DEFAULT );

        assertTrue( aclService.canUpdate( user, dashboard ) );
    }

    @Test
    public void testCanCreatePrivatePublicVisualization()
    {
        User user = createAdminUser( "F_DATAELEMENT_PRIVATE_ADD" );

        assertFalse( aclService.canMakeClassPublic( user, Visualization.class ) );
        assertTrue( aclService.canMakeClassPrivate( user, Visualization.class ) );
    }

    @Test
    public void testCanUpdatePrivateVisualization()
    {
        User user = createAdminUser( "F_DATAELEMENT_PRIVATE_ADD" );

        Visualization visualization = new Visualization( "Visualization" );
        visualization.setAutoFields();
        visualization.setCreatedBy( user );
        visualization.getSharing().setOwner( user );
        visualization.setType( VisualizationType.COLUMN );
        visualization.setPublicAccess( AccessStringHelper.DEFAULT );

        assertTrue( aclService.canUpdate( user, visualization ) );
    }

    @Test
    public void testCanCreatePrivatePublicMap()
    {
        User user = createAdminUser( "F_DATAELEMENT_PRIVATE_ADD" );

        assertFalse( aclService.canMakeClassPublic( user, Map.class ) );
        assertTrue( aclService.canMakeClassPrivate( user, Map.class ) );
    }

    @Test
    public void testCanUpdatePrivateMap()
    {
        User user = createAdminUser( "F_DATAELEMENT_PRIVATE_ADD" );

        Map map = new Map();
        map.setAutoFields();
        map.setCreatedBy( user );
        map.getSharing().setOwner( user );
        map.setPublicAccess( AccessStringHelper.DEFAULT );

        assertTrue( aclService.canUpdate( user, map ) );
    }

    @Test
    public void testCanCreatePrivatePublicEventChart()
    {
        User user = createAdminUser( "F_DATAELEMENT_PRIVATE_ADD" );

        assertFalse( aclService.canMakeClassPublic( user, EventChart.class ) );
        assertTrue( aclService.canMakeClassPrivate( user, EventChart.class ) );
    }

    @Test
    public void testCanUpdatePrivateEventChart()
    {
        User user = createAdminUser( "F_DATAELEMENT_PRIVATE_ADD" );

        EventChart eventChart = new EventChart();
        eventChart.setAutoFields();
        eventChart.setCreatedBy( user );
        eventChart.getSharing().setOwner( user );
        eventChart.setPublicAccess( AccessStringHelper.DEFAULT );

        assertTrue( aclService.canUpdate( user, eventChart ) );
    }

    @Test
    public void testCanCreatePrivatePublicEventReport()
    {
        User user = createAdminUser( "F_DATAELEMENT_PRIVATE_ADD" );

        assertFalse( aclService.canMakeClassPublic( user, EventReport.class ) );
        assertTrue( aclService.canMakeClassPrivate( user, EventReport.class ) );
    }

    @Test
    public void testCanUpdatePrivateEventReport()
    {
        User user = createAdminUser( "F_DATAELEMENT_PRIVATE_ADD" );

        EventReport eventReport = new EventReport();
        eventReport.setAutoFields();
        eventReport.setCreatedBy( user );
        eventReport.getSharing().setOwner( user );
        eventReport.setPublicAccess( AccessStringHelper.DEFAULT );

        assertTrue( aclService.canUpdate( user, eventReport ) );
    }

    @Test
    public void testCanCreatePrivatePublicLegendSet()
    {
        User user = createAdminUser( "F_LEGEND_SET_PRIVATE_ADD" );

        assertFalse( aclService.canMakeClassPublic( user, LegendSet.class ) );
        assertTrue( aclService.canMakeClassPrivate( user, LegendSet.class ) );
    }

    @Test
    public void testCanUpdatePrivateLegendSet()
    {
        User user = createAdminUser( "F_LEGEND_SET_PRIVATE_ADD" );

        LegendSet legendSet = new LegendSet();
        legendSet.setAutoFields();
        legendSet.setCreatedBy( user );
        legendSet.getSharing().setOwner( user );
        legendSet.setPublicAccess( AccessStringHelper.DEFAULT );

        assertTrue( aclService.canUpdate( user, legendSet ) );
    }

    @Test
    public void testVerifyDataElementPrivateRW()
    {
        User user = createAdminUser( "F_DATAELEMENT_PRIVATE_ADD" );

        DataElement dataElement = createDataElement( 'A' );
        dataElement.setPublicAccess( AccessStringHelper.READ_WRITE );

        assertFalse( aclService.verifySharing( dataElement, user ).isEmpty() );
    }

    @Test
    public void testVerifyDataElementPrivate()
    {
        User user = createAdminUser( "F_DATAELEMENT_PRIVATE_ADD" );

        DataElement dataElement = createDataElement( 'A' );
        dataElement.setPublicAccess( AccessStringHelper.DEFAULT );

        assertTrue( aclService.verifySharing( dataElement, user ).isEmpty() );
    }

    @Test
    public void testVerifyDataElementPublicRW()
    {
        User user = createAdminUser( "F_DATAELEMENT_PUBLIC_ADD" );

        DataElement dataElement = createDataElement( 'A' );
        dataElement.setPublicAccess( AccessStringHelper.READ_WRITE );

        assertTrue( aclService.verifySharing( dataElement, user ).isEmpty() );
    }

    @Test
    public void testVerifyDataElementPublic()
    {
        User user = createAdminUser( "F_DATAELEMENT_PUBLIC_ADD" );

        DataElement dataElement = createDataElement( 'A' );
        dataElement.setPublicAccess( AccessStringHelper.DEFAULT );

        assertTrue( aclService.verifySharing( dataElement, user ).isEmpty() );
    }

    @Test
    public void testVerifyVisualizationCantExternalize()
    {
        User user = createAdminUser( "F_VISUALIZATION_PUBLIC_ADD" );

        Visualization visualization = new Visualization();
        visualization.setAutoFields();
        visualization.setPublicAccess( AccessStringHelper.DEFAULT );
        visualization.setExternalAccess( true );
        visualization.setType( VisualizationType.COLUMN );

        assertFalse( aclService.verifySharing( visualization, user ).isEmpty() );
    }

    @Test
    public void testResetSharingPropsPrivate()
    {
        User user = createAdminUser();

        Visualization visualization = new Visualization();
        visualization.setAutoFields();
        visualization.setPublicAccess( AccessStringHelper.DEFAULT );
        visualization.setExternalAccess( true );
        visualization.setType( VisualizationType.COLUMN );

        assertFalse( aclService.verifySharing( visualization, user ).isEmpty() );

        aclService.resetSharing( visualization, user );

        assertTrue( AccessStringHelper.DEFAULT.equals( visualization.getPublicAccess() ) );
        assertFalse( visualization.getExternalAccess() );
        assertTrue( visualization.getUserAccesses().isEmpty() );
        assertTrue( visualization.getUserGroupAccesses().isEmpty() );
    }

    @Test
    public void testResetSharingPropsPublic()
    {
        User user = createAdminUser( "F_VISUALIZATION_PUBLIC_ADD" );

        Visualization visualization = new Visualization();
        visualization.setAutoFields();
        visualization.setPublicAccess( AccessStringHelper.DEFAULT );
        visualization.setExternalAccess( true );
        visualization.setType( VisualizationType.COLUMN );

        assertFalse( aclService.verifySharing( visualization, user ).isEmpty() );

        aclService.resetSharing( visualization, user );

        assertTrue( AccessStringHelper.READ_WRITE.equals( visualization.getPublicAccess() ) );
        assertFalse( visualization.getExternalAccess() );
        assertTrue( visualization.getUserAccesses().isEmpty() );
        assertTrue( visualization.getUserGroupAccesses().isEmpty() );
    }

    @Test
    public void testCreateNoSharingObject()
    {
        User user = createAdminUser();
        assertFalse( aclService.canCreate( user, OrganisationUnit.class ) );
    }

    @Test
    public void testUpdateNoSharingObject()
    {
        User user = createAdminUser();
        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );
        assertFalse( aclService.canUpdate( user, organisationUnit ) );
    }

    @Test
    public void testDataElementSharingPrivateRW()
    {
        User user1 = createUser( "user1A9", "F_DATAELEMENT_PRIVATE_ADD" );
        User user2 = createUser( "user2A9", "F_DATAELEMENT_PRIVATE_ADD" );

        DataElement dataElement = createDataElement( 'A' );
        dataElement.setCreatedBy( user1 );
        dataElement.getSharing().setOwner( user1 );
        manager.save( dataElement );

        assertFalse( aclService.canUpdate( user2, dataElement ) );
        assertEquals( AccessStringHelper.DEFAULT, dataElement.getPublicAccess() );

        UserGroup userGroup = createUserGroup( 'A', new HashSet<>() );
        userGroup.getMembers().add( user1 );
        userGroup.getMembers().add( user2 );

        manager.save( userGroup );
        user1.getGroups().add( userGroup );
        user2.getGroups().add( userGroup );

        dataElement.getSharing().addUserGroupAccess( new UserGroupAccess( userGroup, "rw------" ) );
        manager.update( dataElement );

        assertTrue( aclService.canUpdate( user2, dataElement ) );
    }

    @Test
    public void testCategoryOptionSharingPrivateRW()
    {
        User user1 = createUser( "user11", "F_CATEGORY_OPTION_PRIVATE_ADD" );
        User user2 = createUser( "user22", "F_CATEGORY_OPTION_PRIVATE_ADD" );

        CategoryOption categoryOption = createCategoryOption( 'A' );
        categoryOption.setCreatedBy( user1 );
        categoryOption.getSharing().setOwner( user1 );
        manager.save( categoryOption );

        assertFalse( aclService.canUpdate( user2, categoryOption ) );
        assertEquals( AccessStringHelper.DEFAULT, categoryOption.getPublicAccess() );

        UserGroup userGroup = createUserGroup( 'A', new HashSet<>() );
        userGroup.getMembers().add( user1 );
        userGroup.getMembers().add( user2 );

        manager.save( userGroup );

        user2.getGroups().add( userGroup );
        user1.getGroups().add( userGroup );

        categoryOption.getSharing().addUserGroupAccess( new UserGroupAccess( userGroup, "rw------" ) );
        manager.update( categoryOption );

        assertTrue( aclService.canUpdate( user2, categoryOption ) );
    }

    @Test
    public void testUserCanUpdateDashboard()
    {
        User user1 = createUser( 'A' );
        User user2 = createUser( 'B' );

        manager.save( user1 );
        manager.save( user2 );

        Dashboard dashboard = new Dashboard( "Dashboard" );
        dashboard.setCreatedBy( user1 );
        dashboard.getSharing().setOwner( user1 );
        dashboard.setAutoFields();

        manager.save( dashboard );

        assertTrue( aclService.canRead( user1, dashboard ) );
        assertTrue( aclService.canUpdate( user1, dashboard ) );
        assertTrue( aclService.canDelete( user1, dashboard ) );
        assertTrue( aclService.canManage( user1, dashboard ) );

        assertFalse( aclService.canRead( user2, dashboard ) );
        assertFalse( aclService.canUpdate( user2, dashboard ) );
        assertFalse( aclService.canDelete( user2, dashboard ) );
        assertFalse( aclService.canManage( user2, dashboard ) );
    }

    @Test
    public void testUserCanUpdateDeleteSharedDashboard()
    {
        User user1 = createUser( 'A' );
        User user2 = createUser( 'B' );

        manager.save( user1 );
        manager.save( user2 );

        Dashboard dashboard = new Dashboard( "Dashboard" );
        dashboard.setCreatedBy( user1 );
        dashboard.getSharing().setOwner( user1 );
        dashboard.setAutoFields();

        manager.save( dashboard );

        assertTrue( aclService.canRead( user1, dashboard ) );
        assertTrue( aclService.canUpdate( user1, dashboard ) );
        assertTrue( aclService.canDelete( user1, dashboard ) );
        assertTrue( aclService.canManage( user1, dashboard ) );

        UserAccess userAccess = new UserAccess();
        userAccess.setUser( user2 );
        userAccess.setAccess( AccessStringHelper.READ_WRITE );
        dashboard.getSharing().addUserAccess( userAccess );

        assertTrue( aclService.canRead( user2, dashboard ) );
        assertTrue( aclService.canUpdate( user2, dashboard ) );
        assertTrue( aclService.canDelete( user2, dashboard ) );
        assertTrue( aclService.canManage( user2, dashboard ) );
    }

    @Test
    public void testUserCantUpdateDeletePrivateDashboard()
    {
        User user1 = createUser( 'A' );
        User user2 = createUser( 'B' );

        manager.save( user1 );
        manager.save( user2 );

        Dashboard dashboard = new Dashboard( "Dashboard" );
        dashboard.setCreatedBy( user1 );
        dashboard.getSharing().setOwner( user1 );
        dashboard.setAutoFields();

        manager.save( dashboard );

        assertTrue( aclService.canRead( user1, dashboard ) );
        assertTrue( aclService.canUpdate( user1, dashboard ) );
        assertTrue( aclService.canDelete( user1, dashboard ) );
        assertTrue( aclService.canManage( user1, dashboard ) );

        UserAccess userAccess = new UserAccess();
        userAccess.setUser( user2 );
        userAccess.setAccess( AccessStringHelper.READ );
        dashboard.getSharing().addUserAccess( userAccess );

        assertTrue( aclService.canRead( user2, dashboard ) );
        assertFalse( aclService.canUpdate( user2, dashboard ) );
        assertFalse( aclService.canDelete( user2, dashboard ) );
        assertFalse( aclService.canManage( user2, dashboard ) );
    }

    @Test
    public void testUserCantReadPrivateDashboard()
    {
        User user1 = createUser( 'A' );
        User user2 = createUser( 'B' );

        manager.save( user1 );
        manager.save( user2 );

        Dashboard dashboard = new Dashboard( "Dashboard" );
        dashboard.setCreatedBy( user1 );
        dashboard.getSharing().setOwner( user1 );
        dashboard.setAutoFields();

        manager.save( dashboard );

        assertTrue( aclService.canRead( user1, dashboard ) );
        assertTrue( aclService.canUpdate( user1, dashboard ) );
        assertTrue( aclService.canDelete( user1, dashboard ) );
        assertTrue( aclService.canManage( user1, dashboard ) );

        assertFalse( aclService.canRead( user2, dashboard ) );
        assertFalse( aclService.canUpdate( user2, dashboard ) );
        assertFalse( aclService.canDelete( user2, dashboard ) );
        assertFalse( aclService.canManage( user2, dashboard ) );
    }

    @Test
    public void testUserCanUpdateDashboardSharedWithUserGroup()
    {
        User user1 = createUser( 'A' );
        User user2 = createUser( 'B' );

        manager.save( user1 );
        manager.save( user2 );

        UserGroup userGroup = createUserGroup( 'A', Sets.newHashSet( user1, user2 ) );
        manager.save( userGroup );

        user1.getGroups().add( userGroup );
        user2.getGroups().add( userGroup );

        Dashboard dashboard = new Dashboard( "Dashboard" );
        dashboard.setCreatedBy( user1 );
        dashboard.getSharing().setOwner( user1 );
        manager.save( dashboard );

        UserGroupAccess userGroupAccess = new UserGroupAccess( userGroup, AccessStringHelper.READ );
        dashboard.getSharing().addUserGroupAccess( userGroupAccess );
        manager.save( dashboard, false );

        assertTrue( aclService.canRead( user1, dashboard ) );
        assertTrue( aclService.canUpdate( user1, dashboard ) );
        assertTrue( aclService.canDelete( user1, dashboard ) );
        assertTrue( aclService.canManage( user1, dashboard ) );

        Access access = aclService.getAccess( dashboard, user2 );
        assertTrue( access.isRead() );
        assertFalse( access.isUpdate() );
        assertFalse( access.isDelete() );
        assertFalse( access.isManage() );

        assertTrue( aclService.canRead( user2, dashboard ) );
        assertFalse( aclService.canUpdate( user2, dashboard ) );
        assertFalse( aclService.canDelete( user2, dashboard ) );
        assertFalse( aclService.canManage( user2, dashboard ) );
    }

    @Test
    public void testReadPrivateDataElementSharedThroughGroup()
    {
        User user1 = createUser( "user111", "F_DATAELEMENT_PRIVATE_ADD" );
        User user2 = createUser( "user222", "F_DATAELEMENT_PRIVATE_ADD" );

        manager.save( user1 );
        manager.save( user2 );

        UserGroup userGroup = createUserGroup( 'A', Sets.newHashSet( user1, user2 ) );
        manager.save( userGroup );
        user1.getGroups().add( userGroup );
        user2.getGroups().add( userGroup );

        DataElement dataElement = createDataElement( 'A' );
        dataElement.setPublicAccess( AccessStringHelper.DEFAULT );
        dataElement.setCreatedBy( user1 );
        dataElement.getSharing().setOwner( user1 );
        UserGroupAccess userGroupAccess = new UserGroupAccess( userGroup, AccessStringHelper.READ );
        dataElement.getSharing().addUserGroupAccess( userGroupAccess );
        manager.save( dataElement, false );

        assertTrue( aclService.canWrite( user1, dataElement ) );
        assertTrue( aclService.canUpdate( user1, dataElement ) );
        assertTrue( aclService.canRead( user1, dataElement ) );
        assertFalse( aclService.canDelete( user1, dataElement ) );
        assertTrue( aclService.canManage( user1, dataElement ) );

        Access access = aclService.getAccess( dataElement, user2 );
        assertTrue( access.isRead() );
        assertFalse( access.isWrite() );
        assertFalse( access.isUpdate() );
        assertFalse( access.isDelete() );
        assertFalse( access.isManage() );

        assertTrue( aclService.canRead( user2, dataElement ) );
        assertFalse( aclService.canWrite( user2, dataElement ) );
        assertFalse( aclService.canUpdate( user2, dataElement ) );
        assertFalse( aclService.canDelete( user2, dataElement ) );
        assertFalse( aclService.canManage( user2, dataElement ) );
    }

    @Test
    public void testUpdatePrivateDataElementSharedThroughGroup()
    {
        User user1 = createUser( "user1A", "F_DATAELEMENT_PRIVATE_ADD" );
        User user2 = createUser( "user2A", "F_DATAELEMENT_PRIVATE_ADD" );

        manager.save( user1 );
        manager.save( user2 );

        UserGroup userGroup = createUserGroup( 'A', Sets.newHashSet( user1, user2 ) );
        manager.save( userGroup );

        user1.getGroups().add( userGroup );
        user2.getGroups().add( userGroup );

        DataElement dataElement = createDataElement( 'A' );
        dataElement.getSharing().setPublicAccess( AccessStringHelper.DEFAULT );
        dataElement.setCreatedBy( user1 );
        dataElement.getSharing().setOwner( user1 );
        UserGroupAccess userGroupAccess = new UserGroupAccess( userGroup, AccessStringHelper.READ_WRITE );
        dataElement.getSharing().addUserGroupAccess( userGroupAccess );
        manager.save( dataElement, false );

        assertTrue( aclService.canRead( user1, dataElement ) );
        assertTrue( aclService.canUpdate( user1, dataElement ) );
        assertFalse( aclService.canDelete( user1, dataElement ) );
        assertTrue( aclService.canManage( user1, dataElement ) );

        Access access = aclService.getAccess( dataElement, user2 );
        assertTrue( access.isRead() );
        assertTrue( access.isWrite() );
        assertTrue( access.isUpdate() );
        assertFalse( access.isDelete() );
        assertTrue( access.isManage() );

        assertTrue( aclService.canRead( user2, dataElement ) );
        assertTrue( aclService.canWrite( user2, dataElement ) );
        assertTrue( aclService.canUpdate( user2, dataElement ) );
        assertFalse( aclService.canDelete( user2, dataElement ) );
        assertTrue( aclService.canManage( user2, dataElement ) );
    }

    @Test
    public void testBlockMakePublic()
    {
        User user1 = createUser( "user1B", "F_DATAELEMENT_PRIVATE_ADD" );
        manager.save( user1 );

        DataElement dataElement = createDataElement( 'A' );
        dataElement.setPublicAccess( AccessStringHelper.DEFAULT );
        dataElement.setCreatedBy( user1 );
        dataElement.getSharing().setOwner( user1 );

        assertTrue( aclService.canWrite( user1, dataElement ) );
        manager.save( dataElement, false );

        dataElement.setPublicAccess( AccessStringHelper.READ_WRITE );
        assertFalse( aclService.canUpdate( user1, dataElement ) );
    }

    @Test
    public void testAllowSuperuserMakePublic1()
    {
        User user1 = createUser( "user1C", "F_DATAELEMENT_PRIVATE_ADD" );
        User user2 = createUser( "user2C", "ALL" );
        manager.save( user1 );
        manager.save( user2 );

        DataElement dataElement = createDataElement( 'A' );
        dataElement.setPublicAccess( AccessStringHelper.DEFAULT );
        dataElement.setCreatedBy( user1 );
        dataElement.getSharing().setOwner( user1 );

        assertTrue( aclService.canWrite( user1, dataElement ) );
        manager.save( dataElement, false );

        dataElement.setPublicAccess( AccessStringHelper.READ_WRITE );
        assertTrue( aclService.canUpdate( user2, dataElement ) );
    }

    @Test
    public void testAllowMakePublic()
    {
        User user1 = createUser( "user1D", "F_DATAELEMENT_PUBLIC_ADD" );
        manager.save( user1 );

        DataElement dataElement = createDataElement( 'A' );
        dataElement.setPublicAccess( AccessStringHelper.DEFAULT );
        dataElement.setCreatedBy( user1 );
        dataElement.getSharing().setOwner( user1 );

        Access access = aclService.getAccess( dataElement, user1 );
        assertTrue( access.isRead() );
        assertTrue( access.isWrite() );
        assertTrue( access.isUpdate() );
        assertFalse( access.isDelete() );

        manager.save( dataElement, false );

        dataElement.setPublicAccess( AccessStringHelper.READ_WRITE );

        access = aclService.getAccess( dataElement, user1 );
        assertTrue( access.isRead() );
        assertTrue( access.isWrite() );
        assertTrue( access.isUpdate() );
        assertFalse( access.isDelete() );

        assertTrue( aclService.canUpdate( user1, dataElement ) );
    }

    @Test
    public void testBlockDashboardPublic()
    {
        User user1 = createUser( "user1E" );
        manager.save( user1 );

        Dashboard dashboard = new Dashboard( "Dashboard" );
        dashboard.setPublicAccess( AccessStringHelper.DEFAULT );
        dashboard.setCreatedBy( user1 );
        dashboard.getSharing().setOwner( user1 );

        aclService.canWrite( user1, dashboard );
        manager.save( dashboard, false );

        dashboard.setPublicAccess( AccessStringHelper.READ_WRITE );
        assertFalse( aclService.canUpdate( user1, dashboard ) );
        manager.update( dashboard );
    }

    @Test
    public void testAllowDashboardPublic()
    {
        User user1 = createUser( "user1F", "F_DASHBOARD_PUBLIC_ADD" );
        manager.save( user1 );

        Dashboard dashboard = new Dashboard( "Dashboard" );
        dashboard.setPublicAccess( AccessStringHelper.DEFAULT );
        dashboard.setCreatedBy( user1 );
        dashboard.getSharing().setOwner( user1 );

        aclService.canWrite( user1, dashboard );
        manager.save( dashboard, false );

        dashboard.setPublicAccess( AccessStringHelper.READ_WRITE );
        assertTrue( aclService.canUpdate( user1, dashboard ) );
        manager.update( dashboard );
    }

    @Test
    public void testSuperuserOverride()
    {
        User user1 = createUser( "user11A", "F_DATAELEMENT_PRIVATE_ADD" );
        User user2 = createUser( "user22A", "F_DATAELEMENT_PRIVATE_ADD" );
        User user3 = createUser( "user33A", "ALL" );

        UserGroup userGroup = createUserGroup( 'A', Sets.newHashSet( user1, user2 ) );
        manager.save( userGroup );
        user1.getGroups().add( userGroup );
        user2.getGroups().add( userGroup );

        DataElement dataElement = createDataElement( 'A' );
        dataElement.getSharing().setPublicAccess( AccessStringHelper.DEFAULT );
        dataElement.setCreatedBy( user1 );
        dataElement.getSharing().setOwner( user1 );

        UserGroupAccess userGroupAccess = new UserGroupAccess( userGroup, AccessStringHelper.READ_WRITE );
        dataElement.getSharing().addUserGroupAccess( userGroupAccess );

        manager.save( dataElement, false );

        assertTrue( aclService.canRead( user1, dataElement ) );
        assertTrue( aclService.canUpdate( user1, dataElement ) );
        assertFalse( aclService.canDelete( user1, dataElement ) );
        assertTrue( aclService.canManage( user1, dataElement ) );
        assertTrue( aclService.canDataOrMetadataRead( user1, dataElement ) );

        Access access = aclService.getAccess( dataElement, user2 );
        assertTrue( access.isRead() );
        assertTrue( access.isWrite() );
        assertTrue( access.isUpdate() );
        assertFalse( access.isDelete() );
        assertTrue( access.isManage() );

        assertTrue( aclService.canRead( user2, dataElement ) );
        assertTrue( aclService.canWrite( user2, dataElement ) );
        assertTrue( aclService.canUpdate( user2, dataElement ) );
        assertFalse( aclService.canDelete( user2, dataElement ) );
        assertTrue( aclService.canManage( user2, dataElement ) );

        access = aclService.getAccess( dataElement, user3 );
        assertTrue( access.isRead() );
        assertTrue( access.isWrite() );
        assertTrue( access.isUpdate() );
        assertTrue( access.isDelete() );
        assertTrue( access.isManage() );

        assertTrue( aclService.canRead( user3, dataElement ) );
        assertTrue( aclService.canWrite( user3, dataElement ) );
        assertTrue( aclService.canUpdate( user3, dataElement ) );
        assertTrue( aclService.canDelete( user3, dataElement ) );
        assertTrue( aclService.canManage( user3, dataElement ) );
    }

    @Test
    public void testUpdatePrivateProgram()
    {
        User user = createUser( "user1A1", "F_PROGRAM_PRIVATE_ADD", "F_PROGRAMSTAGE_ADD" );

        Program program = createProgram( 'A' );
        program.setCreatedBy( user );
        program.getSharing().setOwner( user );
        program.setPublicAccess( AccessStringHelper.DEFAULT );

        manager.save( program );

        Access access = aclService.getAccess( program, user );
        assertTrue( access.isRead() );
        assertTrue( access.isWrite() );
        assertTrue( access.isUpdate() );
        assertFalse( access.isDelete() );
        assertTrue( access.isManage() );

        List<ErrorReport> errorReports = aclService.verifySharing( program, user );
        assertTrue( errorReports.isEmpty() );

        manager.update( program );
    }

    @Test
    public void testShouldBlockUpdatesForNoAuthorityUser()
    {
        User adminUser = createAndInjectAdminUser();
        assertEquals( adminUser, currentUserService.getCurrentUser() );

        User userNoAuthorities = createUser( "user1A2" );
        manager.save( userNoAuthorities );

        Visualization visualization = new Visualization();
        visualization.setName( "RT" );
        visualization.setCreatedBy( adminUser );
        visualization.getSharing().setOwner( adminUser );
        visualization.setAutoFields();
        visualization.setPublicAccess( AccessStringHelper.READ );
        visualization.setExternalAccess( true );
        visualization.setType( VisualizationType.COLUMN );

        manager.save( visualization );

        injectSecurityContext( userNoAuthorities );
        assertEquals( userNoAuthorities, currentUserService.getCurrentUser() );

        List<ErrorReport> errorReports = aclService.verifySharing( visualization, userNoAuthorities );
        assertFalse( errorReports.isEmpty() );
    }

    @Test
    public void testShouldBlockUpdatesForNoAuthorityUserEvenWithNonPublicObject()
    {
        User adminUser = createAndInjectAdminUser();
        assertEquals( adminUser, currentUserService.getCurrentUser() );

        User user1 = createUser( "user1A3" );
        User user2 = createUser( "user2A3" );

        injectSecurityContext( user1 );
        assertEquals( user1, currentUserService.getCurrentUser() );

        Visualization visualization = new Visualization();
        visualization.setName( "RT" );
        visualization.setCreatedBy( user1 );
        visualization.getSharing().setOwner( user1 );
        visualization.setAutoFields();
        visualization.setExternalAccess( false );
        visualization.setType( VisualizationType.COLUMN );

        manager.save( visualization );
        visualization.setPublicAccess( AccessStringHelper.DEFAULT );
        manager.update( visualization );

        injectSecurityContext( user2 );
        assertEquals( user2, currentUserService.getCurrentUser() );

        List<ErrorReport> errorReports = aclService.verifySharing( visualization, user2 );
        assertFalse( errorReports.isEmpty() );
    }

    @Test
    public void testNotShouldBlockAdminUpdatesForNoAuthorityUserEvenWithNonPublicObject()
    {
        User adminUser = createAndInjectAdminUser();
        assertEquals( adminUser, currentUserService.getCurrentUser() );

        User user1 = createUser( "user1A4" );
        injectSecurityContext( user1 );
        assertEquals( user1, currentUserService.getCurrentUser() );

        Visualization visualization = new Visualization();
        visualization.setName( "RT" );
        visualization.setCreatedBy( user1 );
        visualization.getSharing().setOwner( user1 );
        visualization.setAutoFields();
        visualization.setExternalAccess( false );
        visualization.setType( VisualizationType.COLUMN );

        manager.save( visualization );
        visualization.setPublicAccess( AccessStringHelper.DEFAULT );
        manager.update( visualization );

        injectSecurityContext( adminUser );
        assertEquals( adminUser, currentUserService.getCurrentUser() );

        List<ErrorReport> errorReports = aclService.verifySharing( visualization, adminUser );
        assertTrue( errorReports.isEmpty() );
    }

    @Test
    public void shouldUseAuthoritiesIfSharingPropsAreNullOrEmptyWithPublicAuth()
    {
        User user1 = createUser( "user1A5", "F_DATAELEMENT_PUBLIC_ADD" );
        User user2 = createUser( "user2A5", "F_DATAELEMENT_PUBLIC_ADD" );

        injectSecurityContext( user1 );

        DataElement dataElement = createDataElement( 'A' );
        dataElement.setCreatedBy( user1 );
        dataElement.getSharing().setOwner( user1 );

        Access access = aclService.getAccess( dataElement, user1 );
        assertTrue( access.isRead() );
        assertTrue( access.isWrite() );
        assertTrue( access.isUpdate() );
        assertFalse( access.isDelete() );

        assertTrue( aclService.canUpdate( user1, dataElement ) );

        manager.save( dataElement );

        dataElement.setPublicAccess( null );
        manager.update( dataElement );

        injectSecurityContext( user2 );

        access = aclService.getAccess( dataElement, user2 );
        assertTrue( access.isRead() );
        assertTrue( access.isWrite() );
        assertTrue( access.isUpdate() );
        assertFalse( access.isDelete() );

        assertTrue( aclService.canUpdate( user2, dataElement ) );

        List<ErrorReport> errorReports = aclService.verifySharing( dataElement, user2 );
        assertTrue( errorReports.isEmpty() );
    }

    @Test
    public void shouldUseAuthoritiesIfSharingPropsAreNullOrEmptyWithPrivateAuth()
    {
        User user1 = createUser( "user1A6", "F_DATAELEMENT_PRIVATE_ADD" );
        User user2 = createUser( "user2A6", "F_DATAELEMENT_PRIVATE_ADD" );

        injectSecurityContext( user1 );

        DataElement dataElement = createDataElement( 'A' );
        dataElement.setCreatedBy( user1 );
        dataElement.getSharing().setOwner( user1 );
        dataElement.setPublicAccess( AccessStringHelper.DEFAULT );

        Access access = aclService.getAccess( dataElement, user1 );
        assertTrue( access.isRead() );
        assertTrue( access.isWrite() );
        assertTrue( access.isUpdate() );
        assertFalse( access.isDelete() );

        manager.save( dataElement );

        dataElement.setPublicAccess( AccessStringHelper.DEFAULT );
        manager.update( dataElement );

        injectSecurityContext( user2 );

        access = aclService.getAccess( dataElement, user2 );
        assertFalse( access.isRead() );
        assertFalse( access.isWrite() );
        assertFalse( access.isUpdate() );
        assertFalse( access.isDelete() );

        assertFalse( aclService.canUpdate( user2, dataElement ) );

        List<ErrorReport> errorReports = aclService.verifySharing( dataElement, user2 );
        assertTrue( errorReports.isEmpty() );
    }

    @Test
    public void testDefaultShouldBlockReadsFromOtherUsers()
    {
        User user1 = createUser( "user1A7", "F_DATAELEMENT_PUBLIC_ADD" );
        User user2 = createUser( "user2A7", "F_DATAELEMENT_PUBLIC_ADD" );

        injectSecurityContext( user1 );

        DataElement dataElement = createDataElement( 'A' );
        dataElement.setCreatedBy( user1 );
        dataElement.getSharing().setOwner( user1 );

        Access access = aclService.getAccess( dataElement, user1 );
        assertTrue( access.isRead() );
        assertTrue( access.isWrite() );
        assertTrue( access.isUpdate() );
        assertFalse( access.isDelete() );

        manager.save( dataElement );

        dataElement.setPublicAccess( AccessStringHelper.DEFAULT );
        manager.update( dataElement );

        injectSecurityContext( user2 );

        access = aclService.getAccess( dataElement, user2 );
        assertFalse( access.isRead() );
        assertFalse( access.isWrite() );
        assertFalse( access.isUpdate() );
        assertFalse( access.isDelete() );

        assertFalse( aclService.canUpdate( user2, dataElement ) );

        List<ErrorReport> errorReports = aclService.verifySharing( dataElement, user2 );
        assertTrue( errorReports.isEmpty() );
    }

    @Test
    public void testUserBCanUpdateReportTableWithAuthority()
    {
        User userA = createUser( 'A' );
        manager.save( userA );

        Visualization visualization = new Visualization();
        visualization.setAutoFields();
        visualization.setName( "FavA" );
        visualization.setCreatedBy( userA );
        visualization.getSharing().setOwner( userA );
        visualization.setPublicAccess( AccessStringHelper.DEFAULT );
        visualization.setType( VisualizationType.COLUMN );

        assertTrue( aclService.canUpdate( userA, visualization ) );

        manager.save( visualization );

        UserAuthorityGroup userAuthorityGroup = new UserAuthorityGroup();
        userAuthorityGroup.setAutoFields();
        userAuthorityGroup.setName( "UR" );

        userAuthorityGroup.getAuthorities().add( "F_VISUALIZATION_PUBLIC_ADD" );
        manager.save( userAuthorityGroup );

        User userB = createUser( 'B' );
        userB.getUserCredentials().getUserAuthorityGroups().add( userAuthorityGroup );
        manager.save( userB );

        visualization.getSharing().addUserAccess( new UserAccess( userB, AccessStringHelper.FULL ) );
        manager.update( visualization );

        assertTrue( aclService.canUpdate( userB, visualization ) );
    }

    @Test
    public void testUserBCanUpdateReportTableWithAuthorityNoUserAccess()
    {
        User userA = createUser( 'A' );
        manager.save( userA );

        Visualization visualization = new Visualization();
        visualization.setAutoFields();
        visualization.setName( "FavA" );
        visualization.setCreatedBy( userA );
        visualization.getSharing().setOwner( userA );
        visualization.setPublicAccess( AccessStringHelper.DEFAULT );
        visualization.setType( VisualizationType.COLUMN );

        assertTrue( aclService.canUpdate( userA, visualization ) );

        manager.save( visualization );

        UserAuthorityGroup userAuthorityGroup = new UserAuthorityGroup();
        userAuthorityGroup.setAutoFields();
        userAuthorityGroup.setName( "UR" );

        userAuthorityGroup.getAuthorities().add( "F_VISUALIZATION_PUBLIC_ADD" );
        manager.save( userAuthorityGroup );

        User userB = createUser( 'B' );
        userB.getUserCredentials().getUserAuthorityGroups().add( userAuthorityGroup );
        manager.save( userB );

        manager.update( visualization );

        assertFalse( aclService.canUpdate( userB, visualization ) );
    }

    @Test
    public void testUserBCanUpdateVisualizationWithoutAuthority()
    {
        User userA = createUser( 'A' );
        manager.save( userA );

        Visualization visualization = new Visualization();
        visualization.setAutoFields();
        visualization.setName( "FavA" );
        visualization.setCreatedBy( userA );
        visualization.getSharing().setOwner( userA );
        visualization.setPublicAccess( AccessStringHelper.DEFAULT );
        visualization.setType( VisualizationType.COLUMN );

        assertTrue( aclService.canUpdate( userA, visualization ) );

        manager.save( visualization );

        User userB = createUser( 'B' );
        manager.save( userB );

        visualization.getSharing().addUserAccess( new UserAccess( userB, AccessStringHelper.FULL ) );
        manager.update( visualization );

        assertTrue( aclService.canUpdate( userB, visualization ) );
    }

    @Test
    public void testCanDataOrMetadataRead()
    {
        User user1 = createUser( "user1A8", "F_CATEGORY_OPTION_GROUP_SET_PUBLIC_ADD" );
        manager.save( user1 );

        // non data shareable object //

        CategoryOptionGroupSet categoryOptionGroupSet = new CategoryOptionGroupSet();
        categoryOptionGroupSet.setAutoFields();
        categoryOptionGroupSet.setName( "cogA" );

        manager.save( categoryOptionGroupSet );

        assertTrue( aclService.canDataOrMetadataRead( user1, categoryOptionGroupSet ) );

        // data shareable object //

        CategoryOption categoryOption = new CategoryOption();
        categoryOption.setAutoFields();
        categoryOption.setName( "coA" );
        categoryOption.setPublicAccess( AccessStringHelper.DATA_READ );
        categoryOption.setCreatedBy( user1 );
        categoryOption.getSharing().setOwner( user1 );
        categoryOption.setPublicAccess( "rwrw----" );

        manager.save( categoryOption, false );

        assertTrue( aclService.canDataOrMetadataRead( user1, categoryOption ) );
    }

    @Test
    public void testAccessObjectWithoutOwner()
    {
        DataElement de = createDataElement( 'A' );
        Sharing sharing = Sharing.builder()
            .publicAccess( AccessStringHelper.DEFAULT )
            .owner( null ).build();
        de.setSharing( sharing );

        manager.save( de, false );
        User userA = createUser( 'A' );
        manager.save( userA );
        dbmsManager.flushSession();

        de = manager.get( de.getUid() );

        assertEquals( AccessStringHelper.DEFAULT, de.getPublicAccess() );
        assertEquals( null, de.getSharing().getOwner() );
        assertTrue( de.getSharing().getUsers().isEmpty() );

        assertTrue( aclService.canRead( userA, de ) );

        String sql = "select uid as uid from dataelement where "
            + JpaQueryUtils.generateSQlQueryForSharingCheck( "sharing", userA, AccessStringHelper.READ );
        SqlRowSet row = jdbcTemplate.queryForRowSet( sql );
        assertEquals( true, row.next() );
        assertEquals( de.getUid(), row.getString( "uid" ) );
    }

    @Test
    void testOwnerDataRead()
    {
        User userA = createUser( "A" );
        manager.save( userA );
        CategoryOption categoryOption = createCategoryOption( 'A' );
        categoryOption.getSharing().setPublicAccess( AccessStringHelper.DEFAULT );
        categoryOption.getSharing().setOwner( userA );
        manager.save( categoryOption );

        assertTrue( aclService.canDataRead( userA, categoryOption ) );
    }

    @Test
    void testOwnerDataReadFail()
    {
        User admin = createAdminUser( "ALL" );
        CategoryOption categoryOption = createCategoryOption( 'A' );
        categoryOption.getSharing().setPublicAccess( AccessStringHelper.DEFAULT );
        categoryOption.getSharing().setOwner( admin );
        manager.save( categoryOption );
        User userA = createUser( "A" );
        manager.save( userA );

        assertFalse( aclService.canDataRead( userA, categoryOption ) );
    }

    @Test
    void testOwnerMetadataRead()
    {
        User userA = createUser( "A" );
        manager.save( userA );
        CategoryOption categoryOption = createCategoryOption( 'A' );
        categoryOption.getSharing().setPublicAccess( AccessStringHelper.DEFAULT );
        categoryOption.getSharing().setOwner( userA );
        manager.save( categoryOption );

        assertTrue( aclService.canRead( userA, categoryOption ) );
    }

    @Test
    void testOwnerMetadataReadFail()
    {
        User admin = createAdminUser( "ALL" );
        CategoryOption categoryOption = createCategoryOption( 'A' );
        categoryOption.getSharing().setPublicAccess( AccessStringHelper.DEFAULT );
        categoryOption.getSharing().setOwner( admin );
        manager.save( categoryOption );
        User userA = createUser( "A" );
        manager.save( userA );

        assertFalse( aclService.canRead( userA, categoryOption ) );
    }
}
