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

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.chart.Chart;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.eventchart.EventChart;
import org.hisp.dhis.eventreport.EventReport;
import org.hisp.dhis.mapping.Map;
import org.hisp.dhis.reporttable.ReportTable;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

        assertTrue( aclService.canUpdate( user, dataElement ) );
    }

    @Test
    public void testCanCreatePrivatePublicDashboard()
    {
        User user = createAdminUser( "F_DATAELEMENT_PRIVATE_ADD" );

        assertFalse( aclService.canCreatePublic( user, Dashboard.class ) );
        assertTrue( aclService.canCreatePrivate( user, Dashboard.class ) );
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

        assertFalse( aclService.canCreatePublic( user, Chart.class ) );
        assertTrue( aclService.canCreatePrivate( user, Chart.class ) );
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

        assertFalse( aclService.canCreatePublic( user, Map.class ) );
        assertTrue( aclService.canCreatePrivate( user, Map.class ) );
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

        assertFalse( aclService.canCreatePublic( user, ReportTable.class ) );
        assertTrue( aclService.canCreatePrivate( user, ReportTable.class ) );
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

        assertFalse( aclService.canCreatePublic( user, EventChart.class ) );
        assertTrue( aclService.canCreatePrivate( user, EventChart.class ) );
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

        assertFalse( aclService.canCreatePublic( user, EventReport.class ) );
        assertTrue( aclService.canCreatePrivate( user, EventReport.class ) );
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
}
