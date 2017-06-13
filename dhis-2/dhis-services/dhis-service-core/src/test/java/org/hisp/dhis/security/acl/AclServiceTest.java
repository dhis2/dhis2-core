package org.hisp.dhis.security.acl;

/*
 * Copyright (c) 2004-2016, University of Oslo
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
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOption;
import org.hisp.dhis.eventchart.EventChart;
import org.hisp.dhis.eventreport.EventReport;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.mapping.Map;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.reporttable.ReportTable;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserGroupAccess;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class AclServiceTest
    extends DhisSpringTest
{
    @Autowired
    private AclService aclService;

    @Autowired
    private UserService _userService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Override
    protected void setUpTest() throws Exception
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
        dataElement.setUser( user );
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
    public void testUpdateObjectWithPublicRWSuccessPrivate()
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

        assertFalse( aclService.canMakePublic( user, Dashboard.class ) );
        assertTrue( aclService.canMakePrivate( user, Dashboard.class ) );
    }

    @Test
    public void testCanUpdatePrivateDashboard()
    {
        User user = createAdminUser( "F_DATAELEMENT_PRIVATE_ADD" );

        Dashboard dashboard = new Dashboard( "Dashboard" );
        dashboard.setAutoFields();
        dashboard.setUser( user );
        dashboard.setPublicAccess( AccessStringHelper.DEFAULT );

        assertTrue( aclService.canUpdate( user, dashboard ) );
    }

    @Test
    public void testCanCreatePrivatePublicChart()
    {
        User user = createAdminUser( "F_DATAELEMENT_PRIVATE_ADD" );

        assertFalse( aclService.canMakePublic( user, Chart.class ) );
        assertTrue( aclService.canMakePrivate( user, Chart.class ) );
    }

    @Test
    public void testCanUpdatePrivateChart()
    {
        User user = createAdminUser( "F_DATAELEMENT_PRIVATE_ADD" );

        Chart chart = new Chart( "Chart" );
        chart.setAutoFields();
        chart.setUser( user );
        chart.setPublicAccess( AccessStringHelper.DEFAULT );

        assertTrue( aclService.canUpdate( user, chart ) );
    }

    @Test
    public void testCanCreatePrivatePublicMap()
    {
        User user = createAdminUser( "F_DATAELEMENT_PRIVATE_ADD" );

        assertFalse( aclService.canMakePublic( user, Map.class ) );
        assertTrue( aclService.canMakePrivate( user, Map.class ) );
    }

    @Test
    public void testCanUpdatePrivateMap()
    {
        User user = createAdminUser( "F_DATAELEMENT_PRIVATE_ADD" );

        Map map = new Map();
        map.setAutoFields();
        map.setUser( user );
        map.setPublicAccess( AccessStringHelper.DEFAULT );

        assertTrue( aclService.canUpdate( user, map ) );
    }

    @Test
    public void testCanCreatePrivatePublicReportTable()
    {
        User user = createAdminUser( "F_DATAELEMENT_PRIVATE_ADD" );

        assertFalse( aclService.canMakePublic( user, ReportTable.class ) );
        assertTrue( aclService.canMakePrivate( user, ReportTable.class ) );
    }

    @Test
    public void testCanUpdatePrivateReportTable()
    {
        User user = createAdminUser( "F_DATAELEMENT_PRIVATE_ADD" );

        ReportTable reportTable = new ReportTable();
        reportTable.setAutoFields();
        reportTable.setUser( user );
        reportTable.setPublicAccess( AccessStringHelper.DEFAULT );

        assertTrue( aclService.canUpdate( user, reportTable ) );
    }

    @Test
    public void testCanCreatePrivatePublicEventChart()
    {
        User user = createAdminUser( "F_DATAELEMENT_PRIVATE_ADD" );

        assertFalse( aclService.canMakePublic( user, EventChart.class ) );
        assertTrue( aclService.canMakePrivate( user, EventChart.class ) );
    }

    @Test
    public void testCanUpdatePrivateEventChart()
    {
        User user = createAdminUser( "F_DATAELEMENT_PRIVATE_ADD" );

        EventChart eventChart = new EventChart();
        eventChart.setAutoFields();
        eventChart.setUser( user );
        eventChart.setPublicAccess( AccessStringHelper.DEFAULT );

        assertTrue( aclService.canUpdate( user, eventChart ) );
    }

    @Test
    public void testCanCreatePrivatePublicEventReport()
    {
        User user = createAdminUser( "F_DATAELEMENT_PRIVATE_ADD" );

        assertFalse( aclService.canMakePublic( user, EventReport.class ) );
        assertTrue( aclService.canMakePrivate( user, EventReport.class ) );
    }

    @Test
    public void testCanUpdatePrivateEventReport()
    {
        User user = createAdminUser( "F_DATAELEMENT_PRIVATE_ADD" );

        EventReport eventReport = new EventReport();
        eventReport.setAutoFields();
        eventReport.setUser( user );
        eventReport.setPublicAccess( AccessStringHelper.DEFAULT );

        assertTrue( aclService.canUpdate( user, eventReport ) );
    }

    @Test
    public void testCanCreatePrivatePublicLegendSet()
    {
        User user = createAdminUser( "F_LEGEND_SET_PRIVATE_ADD" );

        assertFalse( aclService.canMakePublic( user, LegendSet.class ) );
        assertTrue( aclService.canMakePrivate( user, LegendSet.class ) );
    }

    @Test
    public void testCanUpdatePrivateLegendSet()
    {
        User user = createAdminUser( "F_LEGEND_SET_PRIVATE_ADD" );

        LegendSet legendSet = new LegendSet();
        legendSet.setAutoFields();
        legendSet.setUser( user );
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
    public void testVerifyReportTableCantExternalize()
    {
        User user = createAdminUser( "F_REPORTTABLE_PUBLIC_ADD" );

        ReportTable reportTable = new ReportTable();
        reportTable.setAutoFields();
        reportTable.setPublicAccess( AccessStringHelper.DEFAULT );
        reportTable.setExternalAccess( true );

        assertFalse( aclService.verifySharing( reportTable, user ).isEmpty() );
    }

    @Test
    public void testResetSharingPropsPrivate()
    {
        User user = createAdminUser();

        ReportTable reportTable = new ReportTable();
        reportTable.setAutoFields();
        reportTable.setPublicAccess( AccessStringHelper.DEFAULT );
        reportTable.setExternalAccess( true );

        assertFalse( aclService.verifySharing( reportTable, user ).isEmpty() );

        aclService.resetSharing( reportTable, user );

        assertTrue( AccessStringHelper.DEFAULT.equals( reportTable.getPublicAccess() ) );
        assertFalse( reportTable.getExternalAccess() );
        assertTrue( reportTable.getUserAccesses().isEmpty() );
        assertTrue( reportTable.getUserGroupAccesses().isEmpty() );
    }

    @Test
    public void testResetSharingPropsPublic()
    {
        User user = createAdminUser( "F_REPORTTABLE_PUBLIC_ADD" );

        ReportTable reportTable = new ReportTable();
        reportTable.setAutoFields();
        reportTable.setPublicAccess( AccessStringHelper.DEFAULT );
        reportTable.setExternalAccess( true );

        assertFalse( aclService.verifySharing( reportTable, user ).isEmpty() );

        aclService.resetSharing( reportTable, user );

        assertTrue( AccessStringHelper.READ_WRITE.equals( reportTable.getPublicAccess() ) );
        assertFalse( reportTable.getExternalAccess() );
        assertTrue( reportTable.getUserAccesses().isEmpty() );
        assertTrue( reportTable.getUserGroupAccesses().isEmpty() );
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
        User user1 = createUser( "user1", "F_DATAELEMENT_PRIVATE_ADD" );
        User user2 = createUser( "user2", "F_DATAELEMENT_PRIVATE_ADD" );

        DataElement dataElement = createDataElement( 'A' );
        dataElement.setUser( user1 );
        manager.save( dataElement );

        assertFalse( aclService.canUpdate( user2, dataElement ) );
        assertEquals( AccessStringHelper.DEFAULT, dataElement.getPublicAccess() );

        UserGroup userGroup = createUserGroup( 'A', new HashSet<>() );
        userGroup.getMembers().add( user1 );
        userGroup.getMembers().add( user2 );

        manager.save( userGroup );

        dataElement.getUserGroupAccesses().add( new UserGroupAccess( userGroup, "rw------" ) );
        manager.update( dataElement );

        assertTrue( aclService.canUpdate( user2, dataElement ) );
    }

    @Test
    public void testCategoryOptionSharingPrivateRW()
    {
        User user1 = createUser( "user1", "F_CATEGORY_OPTION_PRIVATE_ADD" );
        User user2 = createUser( "user2", "F_CATEGORY_OPTION_PRIVATE_ADD" );

        DataElementCategoryOption categoryOption = createCategoryOption( 'A' );
        categoryOption.setUser( user1 );
        manager.save( categoryOption );

        assertFalse( aclService.canUpdate( user2, categoryOption ) );
        assertEquals( AccessStringHelper.DEFAULT, categoryOption.getPublicAccess() );

        UserGroup userGroup = createUserGroup( 'A', new HashSet<>() );
        userGroup.getMembers().add( user1 );
        userGroup.getMembers().add( user2 );

        manager.save( userGroup );

        categoryOption.getUserGroupAccesses().add( new UserGroupAccess( userGroup, "rw------" ) );
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
        dashboard.setUser( user1 );

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

        Dashboard dashboard = new Dashboard( "Dashboard" );
        dashboard.setUser( user1 );
        manager.save( dashboard );

        UserGroupAccess userGroupAccess = new UserGroupAccess( userGroup, AccessStringHelper.READ );
        dashboard.getUserGroupAccesses().add( userGroupAccess );
        manager.update( dashboard );

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
        User user1 = createUser( "user1", "F_DATAELEMENT_PRIVATE_ADD" );
        User user2 = createUser( "user2", "F_DATAELEMENT_PRIVATE_ADD" );

        manager.save( user1 );
        manager.save( user2 );

        UserGroup userGroup = createUserGroup( 'A', Sets.newHashSet( user1, user2 ) );
        manager.save( userGroup );

        DataElement dataElement = createDataElement( 'A' );
        dataElement.setPublicAccess( AccessStringHelper.DEFAULT );
        dataElement.setUser( user1 );

        assertTrue( aclService.canWrite( user1, dataElement ) );
        manager.save( dataElement );

        UserGroupAccess userGroupAccess = new UserGroupAccess( userGroup, AccessStringHelper.READ );
        dataElement.getUserGroupAccesses().add( userGroupAccess );

        assertTrue( aclService.canUpdate( user1, dataElement ) );
        manager.update( dataElement );

        assertTrue( aclService.canRead( user1, dataElement ) );
        assertTrue( aclService.canWrite( user1, dataElement ) );
        assertTrue( aclService.canUpdate( user1, dataElement ) );
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
        User user1 = createUser( "user1", "F_DATAELEMENT_PRIVATE_ADD" );
        User user2 = createUser( "user2", "F_DATAELEMENT_PRIVATE_ADD" );

        manager.save( user1 );
        manager.save( user2 );

        UserGroup userGroup = createUserGroup( 'A', Sets.newHashSet( user1, user2 ) );
        manager.save( userGroup );

        DataElement dataElement = createDataElement( 'A' );
        dataElement.setPublicAccess( AccessStringHelper.DEFAULT );
        dataElement.setUser( user1 );

        manager.save( dataElement );

        UserGroupAccess userGroupAccess = new UserGroupAccess( userGroup, AccessStringHelper.READ_WRITE );
        dataElement.getUserGroupAccesses().add( userGroupAccess );
        manager.update( dataElement );

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
        User user1 = createUser( "user1", "F_DATAELEMENT_PRIVATE_ADD" );
        manager.save( user1 );

        DataElement dataElement = createDataElement( 'A' );
        dataElement.setPublicAccess( AccessStringHelper.DEFAULT );
        dataElement.setUser( user1 );

        assertTrue( aclService.canWrite( user1, dataElement ) );
        manager.save( dataElement );

        dataElement.setPublicAccess( AccessStringHelper.READ_WRITE );
        assertFalse( aclService.canUpdate( user1, dataElement ) );
    }

    @Test
    public void testAllowSuperuserMakePublic1()
    {
        User user1 = createUser( "user1", "F_DATAELEMENT_PRIVATE_ADD" );
        User user2 = createUser( "user2", "ALL" );
        manager.save( user1 );
        manager.save( user2 );

        DataElement dataElement = createDataElement( 'A' );
        dataElement.setPublicAccess( AccessStringHelper.DEFAULT );
        dataElement.setUser( user1 );

        assertTrue( aclService.canWrite( user1, dataElement ) );
        manager.save( dataElement );

        dataElement.setPublicAccess( AccessStringHelper.READ_WRITE );
        assertTrue( aclService.canUpdate( user2, dataElement ) );
    }

    @Test
    public void testAllowSuperuserMakePublic2()
    {
        User user1 = createUser( "user1", "F_DATAELEMENT_PRIVATE_ADD" );
        User user2 = createUser( "user2", "ALL" );
        manager.save( user1 );
        manager.save( user2 );

        DataElement dataElement = createDataElement( 'A' );
        dataElement.setPublicAccess( AccessStringHelper.DEFAULT );
        dataElement.setUser( user1 );

        assertTrue( aclService.canWrite( user1, dataElement ) );
        manager.save( dataElement, user1 );

        dataElement.setPublicAccess( AccessStringHelper.READ_WRITE );
        assertTrue( aclService.canUpdate( user2, dataElement ) );
        manager.save( dataElement, user2 );

        assertFalse( aclService.canWrite( user1, dataElement ) );
        manager.save( dataElement, user1 );
    }

    @Test
    public void testAllowMakePublic()
    {
        User user1 = createUser( "user1", "F_DATAELEMENT_PUBLIC_ADD" );
        manager.save( user1 );

        DataElement dataElement = createDataElement( 'A' );
        dataElement.setPublicAccess( AccessStringHelper.DEFAULT );
        dataElement.setUser( user1 );

        Access access = aclService.getAccess( dataElement, user1 );
        assertTrue( access.isRead() );
        assertTrue( access.isWrite() );
        assertTrue( access.isUpdate() );
        assertFalse( access.isDelete() );

        manager.save( dataElement );

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
        User user1 = createUser( "user1" );
        manager.save( user1 );

        Dashboard dashboard = new Dashboard( "Dashboard" );
        dashboard.setPublicAccess( AccessStringHelper.DEFAULT );
        dashboard.setUser( user1 );

        aclService.canWrite( user1, dashboard );
        manager.save( dashboard );

        dashboard.setPublicAccess( AccessStringHelper.READ_WRITE );
        assertFalse( aclService.canUpdate( user1, dashboard ) );
        manager.update( dashboard );
    }

    @Test
    public void testAllowDashboardPublic()
    {
        User user1 = createUser( "user1", "F_DASHBOARD_PUBLIC_ADD" );
        manager.save( user1 );

        Dashboard dashboard = new Dashboard( "Dashboard" );
        dashboard.setPublicAccess( AccessStringHelper.DEFAULT );
        dashboard.setUser( user1 );

        aclService.canWrite( user1, dashboard );
        manager.save( dashboard );

        dashboard.setPublicAccess( AccessStringHelper.READ_WRITE );
        assertTrue( aclService.canUpdate( user1, dashboard ) );
        manager.update( dashboard );
    }

    @Test
    public void testSuperuserOverride()
    {
        User user1 = createUser( "user1", "F_DATAELEMENT_PRIVATE_ADD" );
        User user2 = createUser( "user2", "F_DATAELEMENT_PRIVATE_ADD" );
        User user3 = createUser( "user3", "ALL" );

        manager.save( user1 );
        manager.save( user2 );

        UserGroup userGroup = createUserGroup( 'A', Sets.newHashSet( user1, user2 ) );
        manager.save( userGroup );

        DataElement dataElement = createDataElement( 'A' );
        dataElement.setPublicAccess( AccessStringHelper.DEFAULT );
        dataElement.setUser( user1 );

        manager.save( dataElement );

        UserGroupAccess userGroupAccess = new UserGroupAccess( userGroup, AccessStringHelper.READ_WRITE );
        dataElement.getUserGroupAccesses().add( userGroupAccess );
        manager.update( dataElement );

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
        User user = createUser( "user1", "F_PROGRAM_PRIVATE_ADD", "F_PROGRAMSTAGE_ADD" );

        Program program = createProgram( 'A' );
        program.setUser( user );
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
}
